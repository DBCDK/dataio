package dk.dbc.rawrepo;

import dk.dbc.rawrepo.queue.QueueItem;

import java.sql.Timestamp;

public class MockedQueueItem extends QueueItem {
    public MockedQueueItem(String id, int library, String worker, Timestamp queued) {
        this(id, library, worker, queued, 1000);
    }
    public MockedQueueItem(String id, int library, String worker,
                           Timestamp queued, int priority) {
        super(id, library, worker, queued, priority);
    }
}
