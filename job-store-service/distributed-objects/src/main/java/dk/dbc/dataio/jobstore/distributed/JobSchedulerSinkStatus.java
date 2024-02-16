package dk.dbc.dataio.jobstore.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
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

    public boolean isProcessingModeDirectSubmit() {
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
        private QueueSubmitMode queueSubmitMode = QueueSubmitMode.DIRECT;
        public final AtomicInteger ready = new AtomicInteger(0);
        public final AtomicInteger enqueued = new AtomicInteger(0);
        transient ReadWriteLock modeLock = new ReentrantReadWriteLock();

        // owned and updated by singleton JobSchedulerBulkSubmitterBean
        public Future<Integer> lastAsyncPushResult = null;
        public int bulkToDirectCleanUpPushes;

        public void setMode(QueueSubmitMode newMode) {
            modeLock.writeLock().lock();
            try {
                queueSubmitMode = newMode;
            } finally {
                modeLock.writeLock().unlock();
            }
        }

        public QueueSubmitMode getMode() {
            modeLock.readLock().lock();
            try {
                return queueSubmitMode;
            } finally {
                modeLock.readLock().unlock();
            }
        }

        private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
            ois.defaultReadObject();
            modeLock = new ReentrantReadWriteLock();
        }

        public boolean isDirectSubmitMode() {
            return getMode() != QueueSubmitMode.BULK;
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
