package dk.dbc.dataio.jobstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.commons.types.JobSpecification;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * The same-submitter partitioning order, under the concurrency it is meant to survive.
 * <p>
 * Jobs for one submitter and sink must partition one at a time and in job id order. The guarantee is
 * upheld by three cooperating predicates in
 * {@code JobQueueEntity.NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER}: a {@code state = 'WAITING'} filter
 * that makes EvalPlanQual reject a row another transaction has just seized, a prior-entry
 * {@code NOT EXISTS} guard that is snapshot-stable, and a submitter-exclusion subquery. On top of
 * those the seize runs {@code FOR UPDATE ... SKIP LOCKED} in a {@code REQUIRES_NEW} transaction.
 * <p>
 * <b>Every one of those could be deleted and the whole bean-level suite would stay green</b>, because
 * {@code JobQueueRepositoryIT} drives {@code seizeHeadOfQueueIfWaiting} one call at a time. The race
 * needs two transactions on two connections seizing at once, which needs the deployed service rather
 * than a hand-built bean.
 * <p>
 * <b>What this does and does not catch, measured rather than assumed.</b> Deleting the prior-entry
 * guard fails
 * {@link #concurrentSubmissionsForOneSubmitterPartitionOneAtATimeInIdOrder()} on the concurrency
 * count, two of one submitter's jobs partitioning at once. Deleting the {@code state = 'WAITING'}
 * filter on its own does <b>not</b> fail either test: the prior-entry guard still upholds the
 * ordering without it, and the PostgreSQL-to-EclipseLink cycle that filter exists to prevent did not
 * reproduce at this level of contention. So this suite guards the ordering guarantee, and the
 * deadlock predicate stands on its comment rather than on a test.
 * <p>
 * <b>The failure is a hang or a silent reordering, not an exception.</b> Both tests run against a
 * deadline and fail when it expires, and both assert the order rather than merely that the work
 * finished.
 * <p>
 * Jobs go in through the developer endpoint, which needs no flow-store stub and puts every job on
 * sink 1. Each test uses its own submitters so that neither the other tests here nor the other
 * suites in this module share a barrier or queue scope with it.
 */
public class JobQueueOrderingIT extends AbstractJobStoreServiceContainerTest {
    private static final long SUBMITTER_ORDER = 820020;
    private static final long SUBMITTER_PAIR_A = 820021;
    private static final long SUBMITTER_PAIR_B = 820022;

    /** Enough jobs that a lost ordering shows up, few enough to partition well inside the deadline. */
    private static final int JOB_COUNT = 6;

    private static final long DEADLINE_MS = 120_000;
    private static final Duration DEADLINE = Duration.ofMillis(DEADLINE_MS);
    private static final long TEST_TIMEOUT_MS = 2 * DEADLINE_MS;

    /**
     * Tight enough to see a second concurrent seize, which holds its row for the length of one job's
     * partitioning, and loose enough not to load the database it is watching.
     */
    private static final long SAMPLE_INTERVAL_MS = 10;

    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    /**
     * Jobs submitted together for one submitter partition strictly one at a time, in id order.
     * <p>
     * Each {@code addJob} fires its own asynchronous {@code partitionNextJobForSinkIfAvailable}, so
     * submitting the batch at once puts that many seizes on the queue's head at the same moment.
     * That is the interleaving the predicates exist for, and it does not arise from a sequential
     * submission.
     */
    @Test(timeout = TEST_TIMEOUT_MS)
    public void concurrentSubmissionsForOneSubmitterPartitionOneAtATimeInIdOrder() throws Exception {
        QueueSampler sampler = new QueueSampler();
        sampler.start();
        List<Integer> jobIds;
        try {
            jobIds = addJobsConcurrently(JOB_COUNT, SUBMITTER_ORDER);
            awaitAllPartitioned(jobIds);
        } finally {
            sampler.stop();
        }

        assertThat("never two of one submitter's jobs partitioning at once, saw "
                        + sampler.concurrentSightings(SUBMITTER_ORDER),
                sampler.maxConcurrent(SUBMITTER_ORDER), is(1));
        assertThat("the sampler saw every job, so the order below is asserted over the whole batch",
                Set.copyOf(sampler.seizeOrder(SUBMITTER_ORDER)), is(Set.copyOf(jobIds)));
        assertThat("partitioned in job id order",
                sampler.seizeOrder(SUBMITTER_ORDER), is(ascending(jobIds)));
    }

    /**
     * One submitter's queue does not hold up another's.
     * <p>
     * {@code SKIP LOCKED} is what lets a seize step over a head row another transaction holds and
     * take a different submitter's head instead. Without it the second submitter queues behind the
     * first one's lock, and with the prior-entry guard removed the first submitter's own ordering
     * breaks. Asserting both here keeps the two from being traded off against each other.
     * <p>
     * The assertion is that nothing is starved and that each submitter's own order holds, rather
     * than that the two actually overlapped in time. Overlap depends on how long a fifteen record
     * partitioning takes relative to the sampler, which is not a property worth pinning.
     */
    @Test(timeout = TEST_TIMEOUT_MS)
    public void oneSubmittersQueueDoesNotStarveAnother() throws Exception {
        QueueSampler sampler = new QueueSampler();
        sampler.start();
        List<Integer> submitterA;
        List<Integer> submitterB;
        try {
            List<Integer> all = addJobsConcurrently(JOB_COUNT, SUBMITTER_PAIR_A, SUBMITTER_PAIR_B);
            awaitAllPartitioned(all);
            submitterA = jobsOf(SUBMITTER_PAIR_A);
            submitterB = jobsOf(SUBMITTER_PAIR_B);
        } finally {
            sampler.stop();
        }

        assertThat("submitter A partitioned every job", submitterA.size(), is(JOB_COUNT));
        assertThat("submitter B partitioned every job", submitterB.size(), is(JOB_COUNT));
        assertThat("the sampler saw every job of A", Set.copyOf(sampler.seizeOrder(SUBMITTER_PAIR_A)),
                is(Set.copyOf(submitterA)));
        assertThat("the sampler saw every job of B", Set.copyOf(sampler.seizeOrder(SUBMITTER_PAIR_B)),
                is(Set.copyOf(submitterB)));
        assertThat("submitter A kept its own order",
                sampler.seizeOrder(SUBMITTER_PAIR_A), is(ascending(submitterA)));
        assertThat("submitter B kept its own order",
                sampler.seizeOrder(SUBMITTER_PAIR_B), is(ascending(submitterB)));
        assertThat("A never doubled up on itself", sampler.maxConcurrent(SUBMITTER_PAIR_A), is(1));
        assertThat("B never doubled up on itself", sampler.maxConcurrent(SUBMITTER_PAIR_B), is(1));
    }

    // ---------------------------------------------------------------- observation

    /**
     * Watches {@code jobqueue} from outside the service and records, per submitter, which jobs were
     * seen {@code IN_PROGRESS} and how many at once.
     * <p>
     * The order assertion is exact equality against the whole batch, so a job the sampler never saw
     * fails the test. That is deliberate rather than fragile: a job is {@code IN_PROGRESS} for as
     * long as its partitioning takes, which is hundreds of milliseconds for this fixture against a
     * {@value #SAMPLE_INTERVAL_MS} millisecond poll. Each test asserts the sampler's coverage before
     * asserting the order, so a genuine miss reports itself as a miss instead of as a reordering.
     */
    private static final class QueueSampler {
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Map<Long, List<Integer>> firstSeen = new LinkedHashMap<>();
        private final Map<Long, Integer> maxConcurrent = new LinkedHashMap<>();
        private final List<String> concurrentSightings = new CopyOnWriteArrayList<>();
        private Thread thread;

        synchronized void record(long submitter, List<Integer> inProgress) {
            List<Integer> seen = firstSeen.computeIfAbsent(submitter, s -> new ArrayList<>());
            for (Integer jobId : inProgress) {
                if (!seen.contains(jobId)) {
                    seen.add(jobId);
                }
            }
            int previous = maxConcurrent.getOrDefault(submitter, 0);
            if (inProgress.size() > previous) {
                maxConcurrent.put(submitter, inProgress.size());
            }
            if (inProgress.size() > 1) {
                concurrentSightings.add(submitter + " -> " + inProgress);
            }
        }

        void start() {
            thread = new Thread(() -> {
                while (running.get()) {
                    try {
                        sampleInProgressBySubmitter().forEach(this::record);
                        Thread.sleep(SAMPLE_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (SQLException e) {
                        throw new IllegalStateException("sampling jobqueue failed", e);
                    }
                }
            }, "jobqueue-sampler");
            thread.setDaemon(true);
            thread.start();
        }

        void stop() throws InterruptedException {
            running.set(false);
            if (thread != null) {
                thread.join(TimeUnit.SECONDS.toMillis(10));
            }
        }

        synchronized List<Integer> seizeOrder(long submitter) {
            return List.copyOf(firstSeen.getOrDefault(submitter, List.of()));
        }

        synchronized int maxConcurrent(long submitter) {
            return maxConcurrent.getOrDefault(submitter, 0);
        }

        List<String> concurrentSightings() {
            return List.copyOf(concurrentSightings);
        }

        String concurrentSightings(long submitter) {
            return concurrentSightings().stream()
                    .filter(s -> s.startsWith(submitter + " -> "))
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    /**
     * @param submitted job ids as submitted
     * @return the same ids ascending, which is the order they must have partitioned in
     */
    private static List<Integer> ascending(List<Integer> submitted) {
        return submitted.stream().sorted().toList();
    }

    private static Map<Long, List<Integer>> sampleInProgressBySubmitter() throws SQLException {
        Map<Long, List<Integer>> inProgress = new LinkedHashMap<>();
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT jq.jobid, (job.specification->>'submitterId')::bigint AS submitter " +
                             "  FROM jobqueue jq INNER JOIN job ON jq.jobid = job.id " +
                             " WHERE jq.state = 'IN_PROGRESS' " +
                             " ORDER BY jq.id")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    inProgress.computeIfAbsent(resultSet.getLong(2), s -> new ArrayList<>())
                            .add(resultSet.getInt(1));
                }
            }
        }
        return inProgress;
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * Submits one job per submitter per round, all released together, so the seizes collide.
     *
     * @param perSubmitter how many jobs each submitter gets
     * @param submitters   the submitters to submit for
     * @return every job id created, in submission order
     */
    private List<Integer> addJobsConcurrently(int perSubmitter, long... submitters) throws Exception {
        int total = perSubmitter * submitters.length;
        ExecutorService executor = Executors.newFixedThreadPool(total);
        try {
            List<Callable<Integer>> submissions = new ArrayList<>();
            for (int i = 0; i < perSubmitter; i++) {
                for (long submitter : submitters) {
                    submissions.add(() -> addJob(submitter));
                }
            }
            List<Integer> jobIds = new ArrayList<>();
            for (Future<Integer> future : executor.invokeAll(submissions)) {
                jobIds.add(future.get());
            }
            return jobIds;
        } finally {
            executor.shutdownNow();
        }
    }

    private int addJob(long submitter) throws Exception {
        JobInputStream jobInputStream = new JobInputStream(new JobSpecification()
                .withType(JobSpecification.Type.TRANSIENT)
                .withDataFile(FileStoreUrn.create("13613666").toString())
                .withPackaging("addi-xml")
                .withFormat("basis")
                .withCharset("utf8")
                .withDestination("jobqueue-ordering-it")
                .withSubmitterId(submitter), true, 0);

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

    /**
     * @param submitter submitter to look up
     * @return the submitter's job ids, ascending, which is also the order they must partition in
     */
    private List<Integer> jobsOf(long submitter) throws SQLException {
        List<Integer> jobIds = new ArrayList<>();
        try (Connection connection = connectToDB(jobstoreDBContainer);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM job WHERE (specification->>'submitterId')::bigint = ? ORDER BY id")) {
            statement.setLong(1, submitter);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    jobIds.add(resultSet.getInt(1));
                }
            }
        }
        return jobIds;
    }

    /**
     * Waits for every job's partitioning phase to finish, and fails on the deadline rather than
     * returning, since not finishing is one of the two failures under test.
     */
    private void awaitAllPartitioned(List<Integer> jobIds) throws Exception {
        Instant deadline = Instant.now().plus(DEADLINE);
        while (Instant.now().isBefore(deadline)) {
            if (partitionedCount(jobIds) == jobIds.size()) {
                return;
            }
            Thread.sleep(200);
        }
        fail("timed out after " + DEADLINE + " with only " + partitionedCount(jobIds) + " of "
                + jobIds.size() + " jobs partitioned, which is the hang this guards against");
    }

    private long partitionedCount(List<Integer> jobIds) throws Exception {
        long done = 0;
        for (Integer jobId : jobIds) {
            List<dk.dbc.dataio.jobstore.types.JobInfoSnapshot> snapshots =
                    jobStoreServiceConnector.listJobs("job:id = " + jobId);
            if (!snapshots.isEmpty()
                    && snapshots.getFirst().getState().phaseIsDone(State.Phase.PARTITIONING)) {
                done++;
            }
        }
        return done;
    }

    private static String jobStoreBaseUrl() {
        return "http://" + jobStoreServiceContainer.getHost() + ":"
                + jobStoreServiceContainer.getMappedPort(8080) + "/dataio/job-store-service";
    }
}
