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
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * The transaction boundaries the per-job gate depends on, exercised through a deployed service.
 * <p>
 * These cover what no bean-level test can. {@code JobGateBean.insertDataChunkRow} and
 * {@code JobGateBean.sweepScope} are both {@code REQUIRES_NEW}, and that annotation is the whole
 * mechanism: the advisory lock releases at commit, so taken in a caller's long transaction it is
 * held for that transaction rather than for the gate work. A test that builds the beans by hand
 * cannot see any of it, because one connection takes the advisory lock reentrantly and passes
 * whatever the annotation says. Every bean-level test in the job-store suite would still be green
 * with both annotations deleted.
 * <p>
 * <b>The defect these guard against is a hang, not an exception.</b> PostgreSQL sees no lock cycle,
 * because the waiting session is blocked on an EJB call rather than on a database lock, and no
 * {@code lock_timeout} is set. So every wait here runs against a deadline and <i>fails</i> when it
 * expires. A test that merely waits reports nothing.
 * <p>
 * Jobs are created through the developer endpoint, which builds its own sink and needs no flow-store
 * stub, and which takes the sink type as a query parameter so a full-width job can be asked for
 * directly. The submitter is this suite's own, so jobs from the other suites in this module fall
 * outside the barrier scope under test.
 */
public class JobGateBoundaryIT extends AbstractJobStoreServiceContainerTest {
    /**
     * One submitter per test, all distinct from every other suite in this module.
     * <p>
     * The barrier scope is (sink, submitter), and a full-width job's barrier stands until its
     * termination chunk is delivered, which none of these tests does. So a submitter shared between
     * two tests here would leave the second one's jobs queued behind the first one's standing
     * barrier, and a test asserting that a gate opens would fail for a reason that has nothing to do
     * with what it is testing. Sharing one submitter across the class made
     * {@link #abortLiftsTheBarrierAndOpensLaterJobs()} order dependent in exactly that way.
     */
    private static final long SUBMITTER_DEADLOCK = 820010;
    private static final long SUBMITTER_SWEEP = 820011;
    private static final long SUBMITTER_ABORT = 820012;
    private static final long SUBMITTER_RECHECK = 820013;

    /**
     * How long any single wait here is given. Generous next to the work involved, which is
     * partitioning fifteen records, and short next to the failure it detects, which is unbounded.
     */
    private static final long DEADLINE_MS = 90_000;

    private static final Duration DEADLINE = Duration.ofMillis(DEADLINE_MS);

    /**
     * The outer net, in case a test blocks somewhere that is not one of its own deadlines, such as
     * inside an HTTP call to the service. A multiple of {@link #DEADLINE_MS} because a single test
     * makes up to three waits, so the per-wait deadline has to be the one that fires first and names
     * what it was waiting for. Must stay a constant expression to be usable in the annotation.
     */
    private static final long TEST_TIMEOUT_MS = 4 * DEADLINE_MS;

    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    /**
     * Job B partitions while job A's barrier stands, which it cannot do if the gate write joins the
     * partitioning transaction.
     * <p>
     * Both jobs are full width on one submitter and sink, so every chunk of B is inserted with a
     * closed gate. Were {@code insertDataChunkRow} to run in its caller's transaction, the
     * first of those would take the barrier scope's advisory lock and hold it for the rest of B's
     * partitioning. {@code markJobAsPartitioned} is {@code REQUIRES_NEW}, so the insert of B's own
     * termination chunk would then ask for the same lock on a second connection and wait for a
     * transaction that cannot commit until that call returns. B would never finish partitioning.
     * <p>
     * The gate assertion is not decoration. Without it the test would also pass if the width
     * mechanism had quietly stopped closing gates at all, which is the other way this could break.
     */
    @Test(timeout = TEST_TIMEOUT_MS)
    public void dataChunkGateCloseDoesNotDeadlockAgainstTheTerminationInsert() throws Exception {
        int jobA = addFullWidthJob(SUBMITTER_DEADLOCK);
        awaitPartitioned(jobA, "job A");
        assertThat("job A's barrier stands, so job B's chunks have something to wait for",
                terminationBarrierLifted(jobA), is(false));

        int jobB = addFullWidthJob(SUBMITTER_DEADLOCK);
        awaitPartitioned(jobB, "job B, which deadlocks here if the gate write joins the "
                + "partitioning transaction");

        assertThat("job B's first data chunk was held back", gateOpen(jobB, 0), is(false));
    }

    /**
     * The sweep runs to completion in the same call that has just lifted a barrier in the scope it
     * then sweeps.
     * <p>
     * This is the order the hourly recheck uses, barriers before gates, and it is the order that
     * makes the boundary load bearing: the lift takes the scope's advisory lock, and if it were to
     * keep it, the nested per-scope transaction would ask for that lock on a second connection and
     * wait for the transaction that holds it. Aborting job A is what leaves a job standing with an
     * unlifted barrier and no termination row, which is precisely what the barrier half of the sweep
     * looks for.
     */
    @Test(timeout = TEST_TIMEOUT_MS)
    public void gateSweepDoesNotDeadlockAgainstItsOwnBarrierLift() throws Exception {
        int jobA = addFullWidthJob(SUBMITTER_SWEEP);
        awaitPartitioned(jobA, "job A");
        int jobB = addFullWidthJob(SUBMITTER_SWEEP);
        awaitPartitioned(jobB, "job B");
        assertThat("job B's data chunk is closed behind job A", gateOpen(jobB, 0), is(false));

        // Drops job A's rows without lifting its barrier, which is the state the sweep repairs.
        clearTerminationBarrierLifted(jobA);
        removeTrackingRows(jobA);

        Response response = triggerGateSweep();

        assertThat("the sweep returned rather than hanging on a lock it already held",
                response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("and it opened job B on the way", gateOpen(jobB, 0), is(true));
    }

    /**
     * The whole hourly recheck completes when it has both a job to drop and a scope to sweep.
     * <p>
     * This is the case the gate sweep on its own cannot reach. {@code recheckBlocks} drops the
     * scheduling rows of a completed job through {@code DependencyTrackingService.removeJobId},
     * which takes a row lock on every one of that job's rows whatever its gate, and then nests the
     * barrier lift and the gate sweep, both of which run on a second connection and ask for rows in
     * the same scope. {@code removeJobId} is {@code REQUIRES_NEW} precisely so its locks are
     * released before anything nested asks for them.
     * <p>
     * <b>What this proves, measured rather than assumed.</b> It proves the recheck runs end to end
     * through the deployed service with both halves of its work present, which nothing did before,
     * the hourly timer having been its only caller. It does <b>not</b> prove the
     * {@code REQUIRES_NEW} is needed: deleting that annotation leaves this test green, because the
     * row sets here turn out to be disjoint. The sweep matches only
     * {@code NOT gate_open AND NOT is_termination} rows with no earlier unlifted barrier, and this
     * job's data-chunk gates are open while its termination row is excluded by the predicate.
     * Producing the overlap needs a removed job whose own data-chunk gates are closed and every
     * barrier ahead of it lifted, which is a narrower interleaving than this sets up.
     * <p>
     * The annotation is covered instead by
     * {@code JobGateIT.droppingAJobsRowsHoldsLocksTheGateSweepWaitsFor}, which holds an uncommitted
     * {@code deleteByJob} open and asserts a {@code sweepScope} on another connection waits for it,
     * together with that test's reflective assertion that the annotation is present. Between the two
     * the mechanism has a test and the wiring has a test.
     * <p>
     * The deadline is still this test's assertion, because the failure it would catch is a hang and
     * not an exception. PostgreSQL sees no cycle when one side of it is an EJB call, so there is no
     * deadlock to detect and no {@code lock_timeout} to fire.
     */
    @Test(timeout = TEST_TIMEOUT_MS)
    public void recheckCompletesWithBothAJobToDropAndAScopeToSweep() throws Exception {
        int jobA = addFullWidthJob(SUBMITTER_RECHECK);
        awaitPartitioned(jobA, "job A");
        int jobB = addFullWidthJob(SUBMITTER_RECHECK);
        awaitPartitioned(jobB, "job B");
        assertThat("job B's data chunk is closed behind job A", gateOpen(jobB, 0), is(false));

        // What makes the recheck drop job A's rows rather than leave them: it removes the rows of
        // every tracked job that is gone or already completed. Job A keeps its rows and its barrier
        // until this point, so the drop and the sweep land in one call, which is the whole case.
        markCompleted(jobA);

        Response response = triggerRecheckBlocks();

        assertThat("the recheck returned rather than hanging on locks it held itself",
                response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("and job B was released on the way", gateOpen(jobB, 0), is(true));
    }

    /**
     * Aborting a job lifts the barrier it imposed and releases the jobs queued behind it.
     * <p>
     * An aborted job's termination chunk is never delivered, so the delivery-side lift never fires,
     * and the abort drops the tracking rows the lift would have fired on. Without the lift on this
     * path every later job on the submitter and sink is held permanently. Covered here rather than
     * at bean level because the abort path goes through Artemis and Hazelcast, both of which the
     * deployed service has and a unit test does not.
     * <p>
     * <b>This test spent time disabled against a defect it had found, and that is the point of it.</b>
     * {@code abortJob} answered 500 and aborted nothing in the deployed service, because
     * {@code JobProcessorMessageProducerBean.resolveProcessorQueue} was package private and
     * {@code @PostConstruct} had bound it as a method reference on the generated no-interface view.
     * Nothing in the bean-level suite could see it. Making the method public is the whole fix.
     */
    @Test(timeout = TEST_TIMEOUT_MS)
    public void abortLiftsTheBarrierAndOpensLaterJobs() throws Exception {
        int jobA = addFullWidthJob(SUBMITTER_ABORT);
        awaitPartitioned(jobA, "job A");
        int jobB = addFullWidthJob(SUBMITTER_ABORT);
        awaitPartitioned(jobB, "job B");
        assertThat("job B is held behind job A", gateOpen(jobB, 0), is(false));

        jobStoreServiceConnector.abortJob(jobA);

        awaitGateOpen(jobB, 0, "job B's data chunk after job A was aborted");
        assertThat("job A's barrier reads as lifted", terminationBarrierLifted(jobA), is(true));
    }

    /**
     * A full-width job on this suite's submitter, created through the developer endpoint so that no
     * flow-store stub has to exist for a tickle sink.
     *
     * @return the new job's id
     */
    private int addFullWidthJob(long submitter) throws Exception {
        JobInputStream jobInputStream = new JobInputStream(new JobSpecification()
                .withType(JobSpecification.Type.TRANSIENT)
                .withDataFile(FileStoreUrn.create("13613666").toString())
                .withPackaging("addi-xml")
                .withFormat("basis")
                .withCharset("utf8")
                .withDestination("gate-boundary-it")
                .withSubmitterId(submitter), true, 0);

        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(jobStoreBaseUrl())
                    .path("jobs/developer/ADDI_MARC_XML")
                    .queryParam("sinkType", "TICKLE")
                    .request()
                    .post(Entity.entity(JSONB_CONTEXT.marshall(jobInputStream), MediaType.APPLICATION_JSON));
            assertThat("developer endpoint accepted the job", response.getStatus(),
                    is(Response.Status.CREATED.getStatusCode()));
            return JSONB_CONTEXT.unmarshall(response.readEntity(String.class),
                    dk.dbc.dataio.jobstore.types.JobInfoSnapshot.class).getJobId();
        }
    }

    private Response triggerRecheckBlocks() {
        try (Client client = ClientBuilder.newClient()) {
            return client.target(jobStoreBaseUrl())
                    .path("dependency/recheck_blocks")
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_JSON));
        }
    }

    private Response triggerGateSweep() {
        try (Client client = ClientBuilder.newClient()) {
            return client.target(jobStoreBaseUrl())
                    .path("dependency/gate_sweep")
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_JSON));
        }
    }

    private static String jobStoreBaseUrl() {
        return "http://" + jobStoreServiceContainer.getHost() + ":"
                + jobStoreServiceContainer.getMappedPort(8080) + "/dataio/job-store-service";
    }

    /**
     * Waits for the job's partitioning phase to finish, and fails on the deadline rather than
     * returning, since not finishing is the defect under test.
     */
    private void awaitPartitioned(int jobId, String description) throws Exception {
        Instant deadline = Instant.now().plus(DEADLINE);
        while (Instant.now().isBefore(deadline)) {
            List<dk.dbc.dataio.jobstore.types.JobInfoSnapshot> snapshots =
                    jobStoreServiceConnector.listJobs("job:id = " + jobId);
            if (!snapshots.isEmpty()
                    && snapshots.getFirst().getState().phaseIsDone(State.Phase.PARTITIONING)) {
                return;
            }
            Thread.sleep(500);
        }
        fail("timed out after " + DEADLINE + " waiting for partitioning to finish: " + description);
    }

    private void awaitGateOpen(int jobId, int chunkId, String description) throws Exception {
        Instant deadline = Instant.now().plus(DEADLINE);
        while (Instant.now().isBefore(deadline)) {
            if (Boolean.TRUE.equals(gateOpen(jobId, chunkId))) {
                return;
            }
            Thread.sleep(500);
        }
        fail("timed out after " + DEADLINE + " waiting for the gate to open: " + description);
    }

    /**
     * @return the chunk's {@code gate_open}, or null when it has no dependency tracking row, an
     * absent row being an open gate
     */
    private Boolean gateOpen(int jobId, int chunkId) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT gate_open FROM dependencytracking WHERE jobid = ? AND chunkid = ?")) {
            statement.setInt(1, jobId);
            statement.setInt(2, chunkId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBoolean(1) : null;
            }
        }
    }

    private Boolean terminationBarrierLifted(int jobId) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT termination_barrier_lifted FROM job WHERE id = ?")) {
            statement.setInt(1, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                boolean value = resultSet.getBoolean(1);
                return resultSet.wasNull() ? null : value;
            }
        }
    }

    /**
     * Makes the recheck treat the job as finished, which is what puts its rows up for removal.
     * <p>
     * The JPA second level cache is evicted afterwards, and without that the write is invisible:
     * the recheck asks {@code getJobEntityById}, which answers from the cached entity and reports no
     * completion time, so it finds nothing to drop and the test passes for the wrong reason.
     */
    private void markCompleted(int jobId) throws Exception {
        executeUpdate("UPDATE job SET timeofcompletion = now() WHERE id = " + jobId);
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(jobStoreBaseUrl()).path("cache/clear").request().get();
            assertThat("jpa cache evicted", response.getStatus(),
                    is(Response.Status.OK.getStatusCode()));
        }
    }

    private void clearTerminationBarrierLifted(int jobId) throws SQLException {
        executeUpdate("UPDATE job SET termination_barrier_lifted = false WHERE id = " + jobId);
    }

    private void removeTrackingRows(int jobId) throws SQLException {
        executeUpdate("DELETE FROM dependencytracking WHERE jobid = " + jobId);
    }

    private void executeUpdate(String sql) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
