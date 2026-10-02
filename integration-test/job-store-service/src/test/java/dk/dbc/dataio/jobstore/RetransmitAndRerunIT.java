package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jms.JmsQueueTester;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
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
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.fail;

/**
 * The two operator recovery paths that re-drive work, through the deployed service.
 * <p>
 * Both are reached only by an operator calling them, both do their work through a seize in its own
 * transaction or an asynchronous invocation, and neither had any container coverage. The bean-level
 * suite has {@code RerunsRepositoryIT} and {@code JobRerunnerBeanIT}, which build the beans by hand
 * and so exercise neither the {@code REQUIRES_NEW} on the seize nor the {@code @Asynchronous} that
 * drives the rerun forward.
 * <p>
 * The assertions are on what reaches Artemis rather than on a status column, because a status the
 * scheduler is about to change again is a race and a queued message is not. Retransmit's whole
 * purpose is to put the chunk back on the queue, so that is the thing worth asserting.
 * <p>
 * This suite's own submitter keeps its jobs out of every other suite's barrier and queue scope.
 */
public class RetransmitAndRerunIT extends AbstractJobStoreServiceContainerTest {
    /**
     * The stubbed submitter and destination, not a private pair.
     * <p>
     * A rerun goes back through the ordinary {@code addJob} path and re-resolves the flow binder
     * against the flow-store, so it only works for a specification this module has a wiremock
     * mapping for. The developer endpoint the other suites use sidesteps that resolution by building
     * its own sink, which is exactly why it cannot be used here.
     */
    private static final long SUBMITTER = 870970;
    private static final String DESTINATION = "broend-cisterne";

    /** The fixture data file splits into this many chunks, which is what lands on the queue. */
    private static final int CHUNKS_PER_JOB = 2;

    private static final long QUEUE_WAIT_MS = 60_000;
    private static final Duration DEADLINE = Duration.ofMinutes(2);

    /**
     * Retransmit puts a chunk the processor never answered back on the processor queue.
     * <p>
     * The chunks sit in {@code QUEUED_FOR_PROCESSING} because this module runs no processor, which
     * is the same state a lost JMS message leaves. Draining the queue first is what makes the
     * assertion mean something: the messages that arrive afterwards can only have been sent by the
     * retransmit.
     */
    @Test
    public void retransmitPutsAnUnansweredChunkBackOnTheProcessorQueue() throws Exception {
        int jobId = addJob();
        awaitPartitioned(jobId, "the job to retransmit");
        jmsQueueServiceConnector.awaitQueueSize(
                JmsQueueTester.Queue.PROCESSING_BUSINESS, CHUNKS_PER_JOB, QUEUE_WAIT_MS);

        jmsQueueServiceConnector.emptyQueue(JmsQueueTester.Queue.PROCESSING_BUSINESS);
        assertThat("queue drained before the retransmit",
                jmsQueueServiceConnector.getQueueSize(JmsQueueTester.Queue.PROCESSING_BUSINESS), is(0));

        Response response = triggerRetransmit(jobId);
        assertThat("retransmit accepted", response.getStatus(),
                is(Response.Status.OK.getStatusCode()));

        jmsQueueServiceConnector.awaitQueueSize(
                JmsQueueTester.Queue.PROCESSING_BUSINESS, CHUNKS_PER_JOB, QUEUE_WAIT_MS);
    }

    /**
     * A rerun request produces a new job.
     * <p>
     * The request only enqueues a {@code rerun} row. What turns that into a job is
     * {@code RerunsRepository.seizeHeadOfQueueIfWaiting} in its own transaction and
     * {@code JobRerunnerBean.rerunNextIfAvailable} running asynchronously, so a new job appearing is
     * the only evidence that both happened. Neither is a thing a hand-built bean can be wrong about.
     */
    @Test
    public void rerunRequestProducesANewJob() throws Exception {
        int jobId = addJob();
        awaitPartitioned(jobId, "the job to rerun");
        int jobsBefore = jobCountForSubmitter();

        Response response = triggerRerun(jobId);
        assertThat("rerun accepted", response.getStatus(),
                is(Response.Status.CREATED.getStatusCode()));

        awaitJobCountAbove(jobsBefore);
    }

    // ---------------------------------------------------------------- fixtures

    private Response triggerRetransmit(int jobId) {
        try (Client client = ClientBuilder.newClient()) {
            return client.target(jobStoreBaseUrl())
                    .path("dependency/retransmit/" + jobId)
                    .request()
                    .post(Entity.entity("", MediaType.APPLICATION_JSON));
        }
    }

    private Response triggerRerun(int jobId) {
        try (Client client = ClientBuilder.newClient()) {
            return client.target(jobStoreBaseUrl())
                    .path("reruns")
                    .request()
                    .post(Entity.entity(String.valueOf(jobId), MediaType.TEXT_PLAIN));
        }
    }

    /**
     * Counted straight from the table, the list criteria having no filter on the submitter. Scoped by
     * destination as well, so the count is over the jobs this suite and the other stubbed suites
     * create rather than over everything, and the assertion is on the count rising.
     */
    private int jobCountForSubmitter() throws SQLException {
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT count(*) FROM job WHERE (specification->>'submitterId')::bigint = ? " +
                             "  AND specification->>'destination' = '" + DESTINATION + "'")) {
            statement.setLong(1, SUBMITTER);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private void awaitJobCountAbove(int before) throws Exception {
        Instant deadline = Instant.now().plus(DEADLINE);
        int seen = before;
        while (Instant.now().isBefore(deadline)) {
            seen = jobCountForSubmitter();
            if (seen > before) {
                assertThat("a job was created by the rerun", seen, is(greaterThan(before)));
                return;
            }
            Thread.sleep(500);
        }
        fail("timed out after " + DEADLINE + " waiting for the rerun to create a job, still "
                + seen + " jobs for this submitter");
    }

    private int addJob() throws Exception {
        JobInputStream jobInputStream = new JobInputStream(new JobSpecification()
                .withType(JobSpecification.Type.TRANSIENT)
                .withDataFile(FileStoreUrn.create("13613666").toString())
                .withPackaging("addi-xml")
                .withFormat("basis")
                .withCharset("utf8")
                .withDestination(DESTINATION)
                .withSubmitterId(SUBMITTER), true, 0);

        return jobStoreServiceConnector.addJob(jobInputStream).getJobId();
    }

    private void awaitPartitioned(int jobId, String description) throws Exception {
        Instant deadline = Instant.now().plus(DEADLINE);
        while (Instant.now().isBefore(deadline)) {
            List<JobInfoSnapshot> snapshots = jobStoreServiceConnector.listJobs("job:id = " + jobId);
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
