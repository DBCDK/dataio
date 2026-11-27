package dk.dbc.dataio.jobstore;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.RoutingMode;
import com.hazelcast.core.HazelcastInstance;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jobstore.service.util.RemotePartitioning;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.State;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Partitioning that a non-master job-store hands to the master over Hazelcast.
 * <p>
 * A job added on a non-master pod is partitioned by sending a {@code RemotePartitioning} task to the
 * master's executor service, where it runs on a Hazelcast thread outside any application component.
 * The test takes the part of the non-master pod: it connects to the container's application
 * Hazelcast instance as a client and submits the task there. The single container is its own
 * master, so the task arrives exactly as it would from a second pod.
 * <p>
 * A job is held WAITING by an IN_PROGRESS queue entry for an earlier job of the same submitter,
 * which the seize query's prior-entry guard respects. Removing that entry and sending the task is
 * then the only thing that can partition the job.
 */
public class RemotePartitioningIT extends AbstractJobStoreServiceContainerTest {
    private static final long SUBMITTER = 820030;
    private static final int DEVELOPER_SINK_ID = 1;
    private static final Duration DEADLINE = Duration.ofSeconds(60);
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    private static HazelcastInstance hazelcastClient;

    @BeforeClass
    public static void connectHazelcastClient() {
        ClientConfig config = new ClientConfig();
        config.getNetworkConfig()
                .addAddress(jobStoreServiceContainer.getHost() + ":"
                        + jobStoreServiceContainer.getMappedPort(HAZELCAST_DATA_PORT))
                .getClusterRoutingConfig().setRoutingMode(RoutingMode.SINGLE_MEMBER);
        config.getConnectionStrategyConfig().getConnectionRetryConfig().setClusterConnectTimeoutMillis(30_000);
        hazelcastClient = HazelcastClient.newHazelcastClient(config);
    }

    @AfterClass
    public static void shutdownHazelcastClient() {
        if (hazelcastClient != null) {
            hazelcastClient.shutdown();
        }
    }

    @Test(timeout = 180_000)
    public void remotePartitioningRequestPartitionsWaitingJob() throws Exception {
        int blockingJobId = addJob();
        awaitPartitioned(blockingJobId);
        awaitNoQueueEntry(blockingJobId);

        int blockerEntryId = insertInProgressQueueEntry(blockingJobId);
        try {
            int waitingJobId = addJob();
            Thread.sleep(2_000);
            assertThat("job is held back by the IN_PROGRESS entry",
                    queueEntryState(waitingJobId), is(Optional.of("WAITING")));

            deleteQueueEntry(blockerEntryId);

            Future<Void> result = hazelcastClient.<Void>getExecutorService("default")
                    .submit(new RemotePartitioning(developerSink()));
            result.get(30, TimeUnit.SECONDS);

            awaitPartitioned(waitingJobId);
        } finally {
            deleteQueueEntry(blockerEntryId);
        }
    }

    private static Sink developerSink() {
        return new Sink(DEVELOPER_SINK_ID, 1, new SinkContent("sink 1", "sinkqueue1", "remote partitioning IT"));
    }

    private int addJob() throws Exception {
        JobInputStream jobInputStream = new JobInputStream(new JobSpecification()
                .withType(JobSpecification.Type.TRANSIENT)
                .withDataFile(FileStoreUrn.create("13613666").toString())
                .withPackaging("addi-xml")
                .withFormat("basis")
                .withCharset("utf8")
                .withDestination("remote-partitioning-it")
                .withSubmitterId(SUBMITTER), true, 0);

        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(jobStoreBaseUrl())
                    .path("jobs/developer/ADDI_MARC_XML")
                    .request()
                    .post(Entity.entity(JSONB_CONTEXT.marshall(jobInputStream), MediaType.APPLICATION_JSON));
            assertThat("developer endpoint accepted the job", response.getStatus(),
                    is(Response.Status.CREATED.getStatusCode()));
            return JSONB_CONTEXT.unmarshall(response.readEntity(String.class), JobInfoSnapshot.class).getJobId();
        }
    }

    private void awaitPartitioned(int jobId) throws Exception {
        Instant deadline = Instant.now().plus(DEADLINE);
        while (Instant.now().isBefore(deadline)) {
            if (isPartitioned(jobId)) {
                return;
            }
            Thread.sleep(200);
        }
        fail("job " + jobId + " was not partitioned within " + DEADLINE
                + ", queue entry state is " + queueEntryState(jobId));
    }

    private boolean isPartitioned(int jobId) throws Exception {
        List<JobInfoSnapshot> snapshots = jobStoreServiceConnector.listJobs("job:id = " + jobId);
        return !snapshots.isEmpty() && snapshots.getFirst().getState().phaseIsDone(State.Phase.PARTITIONING);
    }

    private void awaitNoQueueEntry(int jobId) throws Exception {
        Instant deadline = Instant.now().plus(DEADLINE);
        while (Instant.now().isBefore(deadline)) {
            if (queueEntryState(jobId).isEmpty()) {
                return;
            }
            Thread.sleep(200);
        }
        fail("queue entry for job " + jobId + " was not removed within " + DEADLINE);
    }

    private static Optional<String> queueEntryState(int jobId) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT state FROM jobqueue WHERE jobid = ?")) {
            statement.setInt(1, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getString(1)) : Optional.empty();
            }
        }
    }

    private static int insertInProgressQueueEntry(int jobId) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO jobqueue (timeofentry, sinkid, jobid, state, recordsplittertype, retries) " +
                             "VALUES (now(), ?, ?, 'IN_PROGRESS', 'ADDI_MARC_XML', 0) RETURNING id")) {
            statement.setInt(1, DEVELOPER_SINK_ID);
            statement.setInt(2, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static void deleteQueueEntry(int entryId) throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM jobqueue WHERE id = ?")) {
            statement.setInt(1, entryId);
            statement.executeUpdate();
        }
    }

    private static String jobStoreBaseUrl() {
        return "http://" + jobStoreServiceContainer.getHost() + ":"
                + jobStoreServiceContainer.getMappedPort(8080) + "/dataio/job-store-service";
    }
}
