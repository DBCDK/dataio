package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.HarvesterException;

public interface RecordHarvestTaskQueue {
    RawRepoRecordHarvestTask peek() throws HarvesterException;

    RawRepoRecordHarvestTask poll() throws HarvesterException;

    boolean isEmpty() throws HarvesterException;

    int estimatedSize();

    int basedOnJob();

    void commit();
}
