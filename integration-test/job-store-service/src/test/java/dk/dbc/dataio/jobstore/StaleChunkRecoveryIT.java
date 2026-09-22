package dk.dbc.dataio.jobstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * The two sweeps that pick up a chunk nothing else is watching, driven through the deployed
 * service.
 * <p>
 * {@code AdminBean.updateStaleChunks} is the only thing watching a chunk whose dispatch attempt was
 * fired and never arrived. A chunk holds {@code READY_FOR_PROCESSING} only between its row
 * committing and the asynchronous dispatch call running, and that call is in-memory, so a crash or a
 * redeploy in that window strands it. The two bulk submitters read only the {@code SCHEDULED_*}
 * statuses, so nothing else would ever look at it again.
 * <p>
 * {@code JobSchedulerBulkSubmitterBean.sweepSinksWithParkedChunks} is what watches a chunk that did
 * reach a {@code SCHEDULED_*} status but whose sink the chunk counts have lost. Those counts are
 * what the once-a-second dispatch sweeps read, so a sink missing from them is a sink nothing
 * dispatches for.
 * <p>
 * <b>Driven by {@code dependency/stale_sweep} rather than by waiting out the timers.</b> Both
 * sweeps are guarded by {@code Hazelcast.isSlave} inside themselves, so the endpoint exercises the
 * guard exactly as the timer does, and what is left uncovered is only that the {@code @Schedule}
 * fires. Waiting for two minute boundaries to cover that cost this class a hundred seconds, which
 * is not what it is worth.
 * <p>
 * The dispatch each sweep triggers is asynchronous and runs in its own transaction, so a chunk is
 * sent shortly after the call returns rather than during it. Hence the wait that follows, which is
 * seconds rather than minutes.
 * <p>
 * Which statuses move where, and that the writes are validated against a chunk that moved on in the
 * meantime, are covered by {@code AdminBeanIT} at bean level, where the race can be arranged by
 * hand. Nothing about that is re-asserted here.
 */
public class StaleChunkRecoveryIT extends AbstractJobStoreServiceContainerTest {
    private static final long SUBMITTER = 820030;

    /**
     * Slack for the asynchronous dispatch the sweep triggers, and for the once-a-second bulk
     * submitter behind it, on a loaded worker.
     */
    private static final Duration DEADLINE = Duration.ofSeconds(30);

    /** Comfortably past the sweep's ten minute window for {@code READY_FOR_PROCESSING}. */
    private static final Duration STALE_BY = Duration.ofMinutes(20);

    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private static final int READY_FOR_PROCESSING = 1;
    private static final int QUEUED_FOR_PROCESSING = 2;
    private static final int SCHEDULED_FOR_PROCESSING = 6;

    /**
     * A chunk stranded in {@code READY_FOR_PROCESSING} is rescued and dispatched.
     * <p>
     * The assertion is that it reaches {@code QUEUED_FOR_PROCESSING}, which takes the whole recovery
     * chain: the sweep has to fire and move it to {@code SCHEDULED_FOR_PROCESSING}, and the
     * once-a-second bulk submitter has to then pick it up and send it. Asserting the intermediate
     * status instead would be a race against that submitter, which claims the chunk within a second
     * of the sweep releasing it.
     * <p>
     * Without the rescue the chunk stays at {@code READY_FOR_PROCESSING} for good, so the deadline
     * is what reports the defect.
     * <p>
     * The state is set up by backdating a real chunk's row, because no dispatch path leaves a chunk
     * in that status for long enough to catch it there.
     */
    @Test
    public void chunkStrandedInReadyForProcessingIsRescuedAndDispatched() throws Exception {
        int jobId = addJob();
        awaitPartitioned(jobId, "the job whose chunk gets stranded");
        strand(jobId, 0);

        triggerStaleSweep();

        awaitStatus(jobId, 0, QUEUED_FOR_PROCESSING);
    }

    /**
     * A chunk parked in {@code SCHEDULED_FOR_PROCESSING} that the sink chunk counts do not know
     * about is dispatched all the same.
     * <p>
     * Those counts are what the once-a-second dispatch sweeps read, and they can lose a chunk: a
     * recount replaces them with a census of the table, and a move whose transaction is still open
     * when that census is taken is not in it. Nothing else reads a parked chunk, so the sink stops
     * dispatching entirely until the next hourly recount, and because the direct paths stand down
     * for the head of a sink's parked queue, every chunk partitioned afterwards parks behind this
     * one.
     * <p>
     * Writing the row directly is what produces that state, since the counts are moved by the
     * scheduler rather than derived from the table. What has to pick the chunk up is
     * {@code JobSchedulerBulkSubmitterBean.sweepSinksWithParkedChunks}, which asks the table once a
     * minute, so the deadline is again what reports the defect.
     */
    @Test
    public void chunkParkedWithoutBeingCountedIsStillDispatched() throws Exception {
        int jobId = addJob();
        awaitPartitioned(jobId, "the job whose chunk is parked uncounted");
        park(jobId, 0);

        triggerStaleSweep();

        awaitStatus(jobId, 0, QUEUED_FOR_PROCESSING);
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * Runs both recovery sweeps and waits for them, so what follows is the dispatch rather than
     * the sweep.
     */
    private void triggerStaleSweep() {
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(jobStoreBaseUrl())
                    .path("dependency/stale_sweep")
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_JSON));
            assertThat("stale sweep accepted", response.getStatus(),
                    is(Response.Status.OK.getStatusCode()));
        }
    }

    /**
     * Parks a chunk the way a lost counter delta leaves one: the row says the chunk is waiting for
     * the bulk sweep, and the sink chunk counts, which this does not touch, do not say so.
     */
    private void park(int jobId, int chunkId) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE dependencytracking SET status = ?, lastmodified = now(), retries = 0 " +
                             " WHERE jobid = ? AND chunkid = ?")) {
            statement.setInt(1, SCHEDULED_FOR_PROCESSING);
            statement.setInt(2, jobId);
            statement.setInt(3, chunkId);
            assertThat("the chunk's row was there to park", statement.executeUpdate(), is(1));
        }
    }

    /**
     * Puts a chunk into the state a crash between its row's commit and the asynchronous dispatch
     * call would have left, backdated past the sweep's window.
     */
    private void strand(int jobId, int chunkId) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE dependencytracking SET status = ?, lastmodified = ?, retries = 0 " +
                             " WHERE jobid = ? AND chunkid = ?")) {
            statement.setInt(1, READY_FOR_PROCESSING);
            statement.setTimestamp(2, Timestamp.from(Instant.now().minus(STALE_BY)));
            statement.setInt(3, jobId);
            statement.setInt(4, chunkId);
            assertThat("the chunk's row was there to strand", statement.executeUpdate(), is(1));
        }
    }

    private Integer statusOf(int jobId, int chunkId) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT status FROM dependencytracking WHERE jobid = ? AND chunkid = ?")) {
            statement.setInt(1, jobId);
            statement.setInt(2, chunkId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : null;
            }
        }
    }

    /**
     * Waits for the chunk to reach the status, and fails on the deadline rather than returning,
     * since the chunk never moving is the defect under test.
     */
    private void awaitStatus(int jobId, int chunkId, int status) throws Exception {
        Instant deadline = Instant.now().plus(DEADLINE);
        Integer seen = null;
        while (Instant.now().isBefore(deadline)) {
            seen = statusOf(jobId, chunkId);
            if (seen != null && seen == status) {
                return;
            }
            Thread.sleep(1000);
        }
        fail("timed out after " + DEADLINE + " waiting for chunk " + jobId + "/" + chunkId
                + " to reach status " + status + ", it sat at " + seen + ". " + diagnostics(jobId, chunkId));
    }

    /**
     * What the sweep would have had to see, read back at the moment the wait gave up.
     * <p>
     * The sweep logs nothing when it moves a chunk, so a bare timeout says only that the chunk did
     * not move. This says whether the row still looked stale, and whether anything else in the table
     * was a candidate at the same time, which is what separates "the sweep never ran" from "the
     * sweep ran and skipped this row".
     */
    private String diagnostics(int jobId, int chunkId) throws SQLException {
        StringBuilder report = new StringBuilder();
        try (Connection connection = connectToDB(jobstoreDBContainer)) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT status, lastmodified, retries, now() AS db_now, " +
                            "       lastmodified < now() - interval '10 minutes' AS looks_stale " +
                            "  FROM dependencytracking WHERE jobid = ? AND chunkid = ?")) {
                statement.setInt(1, jobId);
                statement.setInt(2, chunkId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        report.append("row: status=").append(rs.getInt("status"))
                                .append(" lastmodified=").append(rs.getTimestamp("lastmodified"))
                                .append(" retries=").append(rs.getInt("retries"))
                                .append(" dbNow=").append(rs.getTimestamp("db_now"))
                                .append(" looksStale=").append(rs.getBoolean("looks_stale"));
                    } else {
                        report.append("row: gone");
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT status, count(*) FROM dependencytracking GROUP BY status ORDER BY status")) {
                try (ResultSet rs = statement.executeQuery()) {
                    report.append(" | table by status:");
                    while (rs.next()) {
                        report.append(' ').append(rs.getInt(1)).append('=').append(rs.getInt(2));
                    }
                }
            }
        }
        return report.toString();
    }

    private int addJob() throws Exception {
        JobInputStream jobInputStream = new JobInputStream(new JobSpecification()
                .withType(JobSpecification.Type.TRANSIENT)
                .withDataFile(FileStoreUrn.create("13613666").toString())
                .withPackaging("addi-xml")
                .withFormat("basis")
                .withCharset("utf8")
                .withDestination("stale-chunk-recovery-it")
                .withSubmitterId(SUBMITTER), true, 0);

        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(jobStoreBaseUrl())
                    .path("jobs/developer/ADDI_MARC_XML")
                    .request()
                    .post(Entity.entity(JSONB_CONTEXT.marshall(jobInputStream), MediaType.APPLICATION_JSON));
            assertThat("developer endpoint accepted the job", response.getStatus(),
                    is(Response.Status.CREATED.getStatusCode()));
            return JSONB_CONTEXT.unmarshall(response.readEntity(String.class),
                    dk.dbc.dataio.jobstore.types.JobInfoSnapshot.class).getJobId();
        }
    }

    private void awaitPartitioned(int jobId, String description) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(2));
        while (Instant.now().isBefore(deadline)) {
            List<dk.dbc.dataio.jobstore.types.JobInfoSnapshot> snapshots =
                    jobStoreServiceConnector.listJobs("job:id = " + jobId);
            if (!snapshots.isEmpty()
                    && snapshots.getFirst().getState().phaseIsDone(State.Phase.PARTITIONING)) {
                return;
            }
            Thread.sleep(500);
        }
        fail("timed out waiting for partitioning to finish: " + description);
    }

    private static String jobStoreBaseUrl() {
        return "http://" + jobStoreServiceContainer.getHost() + ":"
                + jobStoreServiceContainer.getMappedPort(8080) + "/dataio/job-store-service";
    }
}
