package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.types.HarvesterException;

public interface RecordHarvestTaskQueue {
    RawRepoRecordHarvestTask peek() throws HarvesterException;

    RawRepoRecordHarvestTask poll() throws HarvesterException;

    boolean isEmpty() throws HarvesterException;

    int estimatedSize();

    int basedOnJob();

    void commit();
}
