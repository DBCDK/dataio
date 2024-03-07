package dk.dbc.dataio.jobstore.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.Future;
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

    public boolean isProcessingModeDirectSubmit() {
        return processingStatus.isDirectSubmitMode();
    }

    public boolean isDeliveringModeDirectSubmit() {
        return deliveringStatus.isDirectSubmitMode();
    }

    @Override
    public String toString() {
        return "status{" +
                "processing=" + processingStatus +
                ", delivering=" + deliveringStatus +
                '}';
    }

    public void bulk() {
        processingStatus.setMode(QueueSubmitMode.BULK);
        deliveringStatus.setMode(QueueSubmitMode.BULK);
    }

    // Status for a single JMS queue..
    public static class QueueStatus implements Serializable {
        private QueueSubmitMode queueSubmitMode = QueueSubmitMode.DIRECT;
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
                    '}';
        }
    }
}
