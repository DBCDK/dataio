package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskQueueTest {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final TaskRepo taskRepo = new TaskRepo(entityManager);
    @SuppressWarnings("unchecked")
    private final TypedQuery<HarvestTask> query = mock(TypedQuery.class);
    private final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()
            .withConsumerId("consumerId"));

    @BeforeEach
    public void setupMocks() {
        when(entityManager.createNamedQuery(HarvestTask.QUERY_FIND_NEXT, HarvestTask.class)).thenReturn(query);
        when(query.setParameter("configId", config.getId())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
    }

    @Test
    public void poll_noWaitingTasks_returnsNull() throws HarvesterException {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        TaskQueue queue = createQueue();
        assertThat(queue.poll(), is(nullValue()));
        assertThat("queue.estimatedSize()", queue.estimatedSize(), is(0));
    }

    @Test
    public void peek_noWaitingTasks_returnsNull() throws HarvesterException {
        when(query.getResultList()).thenReturn(Collections.emptyList());
        TaskQueue queue = createQueue();
        assertThat(queue.peek(), is(nullValue()));
        assertThat("queue.estimatedSize()", queue.estimatedSize(), is(0));
    }

    @Test
    public void poll_removesHead() throws HarvesterException {
        final int submitterNumber = 123456;
        RawRepoRecordHarvestTask expectedRecordHarvestTask1 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id1", submitterNumber))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id1")
                        .withSubmitterNumber(submitterNumber));
        RawRepoRecordHarvestTask expectedRecordHarvestTask2 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id2", submitterNumber))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id2")
                        .withSubmitterNumber(submitterNumber));
        HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Arrays.asList(
                expectedRecordHarvestTask1.getAddiMetaData(),
                expectedRecordHarvestTask2.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        TaskQueue queue = createQueue();
        assertThat("queue is empty before first poll", queue.isEmpty(), is(false));
        RawRepoRecordHarvestTask recordHarvestTask1 = queue.poll();
        assertThat("queue is empty after first poll", queue.isEmpty(), is(false));
        RawRepoRecordHarvestTask recordHarvestTask2 = queue.poll();
        assertThat("queue is empty after second poll", queue.isEmpty(), is(true));
        assertThat("recordHarvestTask1", recordHarvestTask1, is(expectedRecordHarvestTask1));
        assertThat("recordHarvestTask2", recordHarvestTask2, is(expectedRecordHarvestTask2));
    }

    @Test
    public void peek_headRemains() throws HarvesterException {
        final int submitterNumber = 123456;
        RawRepoRecordHarvestTask expectedRecordHarvestTask = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", submitterNumber))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(submitterNumber));
        HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Collections.singletonList(expectedRecordHarvestTask.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        TaskQueue queue = createQueue();
        assertThat("queue is empty before first peek", queue.isEmpty(), is(false));
        RawRepoRecordHarvestTask recordHarvestTask1 = queue.peek();
        assertThat("queue is empty after first peek", queue.isEmpty(), is(false));
        RawRepoRecordHarvestTask recordHarvestTask2 = queue.peek();
        assertThat("queue is empty after second peek", queue.isEmpty(), is(false));
        assertThat("recordHarvestTask1", recordHarvestTask1, is(expectedRecordHarvestTask));
        assertThat("recordHarvestTask2", recordHarvestTask2, is(expectedRecordHarvestTask));
    }

    @Test
    public void poll_skipsWhereRecordIdIsNull() throws HarvesterException {
        RawRepoRecordHarvestTask expectedRecordHarvestTask = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", 123456))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(123456));
        HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Arrays.asList(
                new AddiMetaData().withBibliographicRecordId("missingAgencyId"),
                expectedRecordHarvestTask.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        TaskQueue queue = createQueue();
        assertThat("queue is empty before first poll", queue.isEmpty(), is(false));
        RawRepoRecordHarvestTask recordHarvestTask = queue.poll();
        assertThat("queue is empty after first poll", queue.isEmpty(), is(true));
        assertThat("recordHarvestTask", recordHarvestTask, is(expectedRecordHarvestTask));
    }

    @Test
    public void peek_skipsWhereRecordIdIsNull() throws HarvesterException {
        RawRepoRecordHarvestTask expectedRecordHarvestTask = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", 123456))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(123456));
        HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Arrays.asList(
                new AddiMetaData().withBibliographicRecordId("missingAgencyId"),
                expectedRecordHarvestTask.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        TaskQueue queue = createQueue();
        assertThat("queue is empty before first peek", queue.isEmpty(), is(false));
        RawRepoRecordHarvestTask recordHarvestTask = queue.peek();
        assertThat("queue is empty after first peek", queue.isEmpty(), is(false));
        assertThat("recordHarvestTask", recordHarvestTask, is(expectedRecordHarvestTask));
    }

    @Test
    public void interpolates870970TasksForDbc() throws HarvesterException {
        interpolatesTasksForDbcWithSubmitter(870970);
    }

    @Test
    public void interpolates190004TasksForDbc() throws HarvesterException {
        interpolatesTasksForDbcWithSubmitter(190004);
    }

    public void interpolatesTasksForDbcWithSubmitter(int submitter) throws HarvesterException {
        RawRepoRecordHarvestTask expectedRecordHarvestTask1 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", submitter))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(submitter));
        RawRepoRecordHarvestTask expectedRecordHarvestTask2 = new RawRepoRecordHarvestTask()
                .withRecordId(new RecordIdDTO("id", 191919))
                .withAddiMetaData(new AddiMetaData()
                        .withBibliographicRecordId("id")
                        .withSubmitterNumber(submitter));
        HarvestTask harvestTask = new HarvestTask();
        harvestTask.setRecords(Collections.singletonList(expectedRecordHarvestTask1.getAddiMetaData()));
        when(query.getResultList()).thenReturn(Collections.singletonList(harvestTask));

        TaskQueue queue = createQueue();
        assertThat("queue is empty before first poll", queue.isEmpty(), is(false));
        RawRepoRecordHarvestTask recordHarvestTask = queue.poll();
        assertThat("queue is empty after first poll", queue.isEmpty(), is(false));
        assertThat("recordHarvestTask", recordHarvestTask, is(expectedRecordHarvestTask1));
        recordHarvestTask = queue.poll();
        assertThat("queue is empty after second poll", queue.isEmpty(), is(true));
        assertThat("recordHarvestTask", recordHarvestTask, is(expectedRecordHarvestTask2));
    }

    private TaskQueue createQueue() {
        return new TaskQueue(config, taskRepo);
    }
}
