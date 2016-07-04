package dk.dbc.dataio.jobstore.service.ejb;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by ja7 on 03-07-16.
 *
 * Handling of pr Sink Status.
 *
 */
class JobSchedulerPrSinkQueueStatus {
    public JobSchedulerPrSinkQueueStatus() {
        readyForProcessing = new AtomicInteger(0);
        jmsEnqueuedToProcessing = new AtomicInteger(0);
        processingMode = JobSchedulerBean.QueueMode.directSubmit;

        readyForDelivering = new AtomicInteger(0);
        jmsEnqueuedToDelivering = new AtomicInteger(0);
        deliveringMode = JobSchedulerBean.QueueMode.directSubmit;
    }

    private JobSchedulerBean.QueueMode deliveringMode;
    final AtomicInteger readyForDelivering;
    final AtomicInteger jmsEnqueuedToDelivering;

    private JobSchedulerBean.QueueMode processingMode;
    final AtomicInteger readyForProcessing;
    final AtomicInteger jmsEnqueuedToProcessing;

    final ReadWriteLock modeLock = new ReentrantReadWriteLock();

    void setProcessingMode(JobSchedulerBean.QueueMode newMode) {
        modeLock.writeLock().lock();
        try {
            processingMode = newMode;
        } finally {
            modeLock.writeLock().unlock();
        }
    }

    JobSchedulerBean.QueueMode getProcessingMode() {
        modeLock.readLock().lock();
        try {
            return processingMode;
        } finally {
            modeLock.readLock().unlock();
        }
    }

    boolean isProcessingModeDirectSubmit() {
        return getProcessingMode() == JobSchedulerBean.QueueMode.directSubmit;
    }

    void setDeliveringMode(JobSchedulerBean.QueueMode newMode) {
        modeLock.writeLock().lock();
        try {
            deliveringMode = newMode;
        } finally {
            modeLock.writeLock().unlock();
        }
    }

    JobSchedulerBean.QueueMode getDeliveringMode() {
        modeLock.readLock().lock();
        try {
            return deliveringMode;
        } finally {
            modeLock.readLock().unlock();
        }
    }

    boolean isDeliveringModeDirectSubmit() {
        return getDeliveringMode() == JobSchedulerBean.QueueMode.directSubmit;
    }
}
