package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.dto.RecordIdDTO;

import java.util.Collections;

/**
 * Abstraction layer for outstanding harvest tasks transforming a single harvest task
 * into a queue of record harvest tasks.
 * This class is not thread safe.
 */
public class TaskQueue implements RecordHarvestTaskQueue {
    private final HarvestTask harvestTask;
    private final TaskRepo taskRepo;
    private int cursor;
    private RawRepoRecordHarvestTask head;
    private RawRepoRecordHarvestTask interpolated;

    public TaskQueue(RRHarvesterConfig config, TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
        harvestTask = taskRepo.findNextHarvestTask(config.getId()).orElseGet(() -> {
            final HarvestTask ht = new HarvestTask();
            ht.setRecords(Collections.emptyList());
            return ht;
        });
        this.cursor = 0;
        this.head = null;
    }

    /**
     * Retrieves, but does not remove, the head of this task queue, or returns null if this task queue is empty.
     *
     * @return the head of this task queue, or null if this task queue is empty
     * @throws HarvesterException on error while retrieving a task
     */
    @Override
    public RawRepoRecordHarvestTask peek() throws HarvesterException {
        if (head == null) {
            if (interpolated == null) {
                head = head();
            } else {
                head = interpolated;
            }
        }
        return head;
    }

    /**
     * Retrieves and removes the head of this task queue, or returns null if this task queue is empty.
     *
     * @return the head of this task queue, or null if this task queue is empty
     * @throws HarvesterException on error while retrieving a task
     */
    @Override
    public RawRepoRecordHarvestTask poll() throws HarvesterException {
        final RawRepoRecordHarvestTask peek = peek();
        if (peek == interpolated) {
            interpolated = null;
        }
        if (peek != null && interpolated == null) {
            cursor++;
        }
        head = null;
        return peek;
    }

    /**
     * Due to interpolation of DBC library records because of special delete record handling
     * in HarvestOperation the estimated size of a task queue can be up to double the actual
     * number and it may abruptly jump from a size greater than one to zero.
     *
     * @return estimated size of this task queue
     */
    @Override
    public int estimatedSize() {
        if (isEmpty()) {
            return 0;
        }
        return 2 * harvestTask.getRecords().size() - cursor;
    }

    @Override
    public int basedOnJob() {
        return harvestTask.getBasedOnJob() != null ? harvestTask.getBasedOnJob() : 0;
    }

    @Override
    public void commit() {
        taskRepo.getEntityManager().remove(harvestTask);
    }

    @Override
    public boolean isEmpty() {
        return interpolated == null && cursor >= harvestTask.getRecords().size();
    }

    private RawRepoRecordHarvestTask head() throws HarvesterException {
        RawRepoRecordHarvestTask recordHarvestTask = null;
        while (recordHarvestTask == null && !isEmpty()) {
            final AddiMetaData addiMetaData = harvestTask.getRecords().get(cursor);
            final RecordIdDTO recordId = toRecordId(addiMetaData);
            if (recordId == null) {
                cursor++;
                continue;
            }
            if (HarvestOperation.DBC_COMMUNITY.contains(recordId.getAgencyId())) {
                // Due to special delete record handling in HarvestOperation
                // a DBC library record task is interpolated into this queue
                interpolated = new RawRepoRecordHarvestTask()
                        .withRecordId(new RecordIdDTO(addiMetaData.bibliographicRecordId(), HarvestOperation.DBC_LIBRARY))
                        .withAddiMetaData(addiMetaData);
            }
            recordHarvestTask = new RawRepoRecordHarvestTask()
                    .withRecordId(recordId)
                    .withAddiMetaData(addiMetaData);
        }
        return recordHarvestTask;
    }

    private RecordIdDTO toRecordId(AddiMetaData addiMetaData) {
        if (addiMetaData != null
                && addiMetaData.submitterNumber() != null
                && addiMetaData.bibliographicRecordId() != null) {
            return new RecordIdDTO(addiMetaData.bibliographicRecordId(), addiMetaData.submitterNumber());
        }
        return null;
    }
}
