package dk.dbc.rawrepo;

import java.sql.Timestamp;

public class MockedQueueJob extends QueueJob {
    public MockedQueueJob(String id, int library, String worker, Timestamp queued) {
        super(id, library, worker, queued);
    }
}
