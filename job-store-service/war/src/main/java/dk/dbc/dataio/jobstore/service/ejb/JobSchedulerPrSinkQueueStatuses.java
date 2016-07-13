package dk.dbc.dataio.jobstore.service.ejb;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class JobSchedulerPrSinkQueueStatuses {
    final QueueStatus processingStatus=new QueueStatus();
    final QueueStatus deliveringStatus=new QueueStatus();


    // Transitional helper classes
    boolean isProcessingModeDirectSubmit() {
        return processingStatus.isDirectSubmitMode();

    }


    boolean isDeliveringModeDirectSubmit() {
        return deliveringStatus.isDirectSubmitMode();

    }


    /**
     * Created by ja7 on 03-07-16.
     *
     * Handling of pr Sink Status.
     *
     */

    /// Status for a single JMS queue..
    static class QueueStatus {
        final AtomicInteger readyForQueue = new AtomicInteger(0);
        final AtomicInteger jmsEnqueued = new AtomicInteger(0);
        private JobSchedulerBean.QueueSubmitMode queueSubmitMode = JobSchedulerBean.QueueSubmitMode.DIRECT;
        final ReadWriteLock modeLock = new ReentrantReadWriteLock();

        //
        // owned by and updaded by Singleton JobSchedulerBulkSubmitterBean
        public Future<Integer> lastAsyncPushResult=null;
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
