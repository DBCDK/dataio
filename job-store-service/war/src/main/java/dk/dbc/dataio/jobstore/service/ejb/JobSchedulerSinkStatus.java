package dk.dbc.dataio.jobstore.service.ejb;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class JobSchedulerSinkStatus {
    final QueueStatus processingStatus = new QueueStatus();
    final QueueStatus deliveringStatus = new QueueStatus();

    boolean isProcessingModeDirectSubmit() {
        return processingStatus.isDirectSubmitMode();
    }

    boolean isDeliveringModeDirectSubmit() {
        return deliveringStatus.isDirectSubmitMode();
    }

    // Status for a single JMS queue..
    static class QueueStatus {
        private JobSchedulerBean.QueueSubmitMode queueSubmitMode = JobSchedulerBean.QueueSubmitMode.DIRECT;
        final AtomicInteger ready = new AtomicInteger(0);
        final AtomicInteger enqueued = new AtomicInteger(0);
        final ReadWriteLock modeLock = new ReentrantReadWriteLock();

        // owned and updated by singleton JobSchedulerBulkSubmitterBean
        public Future<Integer> lastAsyncPushResult = null;
        public int bulkToDirectCleanUpPushes;

        void setMode(JobSchedulerBean.QueueSubmitMode newMode) {
            modeLock.writeLock().lock();
            try {
                queueSubmitMode = newMode;
            } finally {
                modeLock.writeLock().unlock();
            }
        }

        JobSchedulerBean.QueueSubmitMode getMode() {
            modeLock.readLock().lock();
            try {
                return queueSubmitMode;
            } finally {
                modeLock.readLock().unlock();
            }
        }

        boolean isDirectSubmitMode() {
            return getMode() != JobSchedulerBean.QueueSubmitMode.BULK;
        }
    }
}
