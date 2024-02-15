package dk.dbc.dataio.jobstore.service.ejb;

import java.io.Serializable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JobSchedulerSinkStatus implements Serializable {
    final QueueStatus processingStatus = new QueueStatus();
    final QueueStatus deliveringStatus = new QueueStatus();

    public QueueStatus getProcessingStatus() {
        return processingStatus;
    }

    public QueueStatus getDeliveringStatus() {
        return deliveringStatus;
    }

    public void mergeCounters(JobSchedulerSinkStatus status) {
        processingStatus.enqueued.addAndGet(status.processingStatus.enqueued.get());
        processingStatus.ready.addAndGet(status.processingStatus.ready.get());
        deliveringStatus.enqueued.addAndGet(status.deliveringStatus.enqueued.get());
        deliveringStatus.ready.addAndGet(status.deliveringStatus.ready.get());
    }

    boolean isProcessingModeDirectSubmit() {
        return processingStatus.isDirectSubmitMode();
    }

    boolean isDeliveringModeDirectSubmit() {
        return deliveringStatus.isDirectSubmitMode();
    }

    @Override
    public String toString() {
        return "status{" +
                "processing=" + processingStatus +
                ", delivering=" + deliveringStatus +
                '}';
    }

    // Status for a single JMS queue..
    public static class QueueStatus implements Serializable {
        private JobSchedulerBean.QueueSubmitMode queueSubmitMode = JobSchedulerBean.QueueSubmitMode.DIRECT;
        public final AtomicInteger ready = new AtomicInteger(0);
        public final AtomicInteger enqueued = new AtomicInteger(0);
        final transient ReadWriteLock modeLock = new ReentrantReadWriteLock();

        // owned and updated by singleton JobSchedulerBulkSubmitterBean
        public Future<Integer> lastAsyncPushResult = null;
        public int bulkToDirectCleanUpPushes;

        public void setMode(JobSchedulerBean.QueueSubmitMode newMode) {
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

        @Override
        public String toString() {
            return "Queue{" +
                    "mode=" + queueSubmitMode +
                    ", ready=" + ready +
                    ", enqueued=" + enqueued +
                    '}';
        }
    }
}
