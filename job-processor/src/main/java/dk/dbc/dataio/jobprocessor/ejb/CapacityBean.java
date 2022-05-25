package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Constants;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

/**
 * The purpose of this singleton bean is to maintain a flag indicating whether or not this processor has exceeded its
 * maximum capacity
 */
@Singleton
public class CapacityBean {
    public final static int MAXIMUM_TIME_TO_PROCESS_IN_MILLISECONDS = 180000;

    private boolean capacityExceeded = false;
    private String shardId;

    @PostConstruct
    public void initialize() {
        shardId = System.getenv().get(Constants.PROCESSOR_SHARD_ENV_VARIABLE);
    }

    @Lock(LockType.READ)
    public String getShardId() {
        return shardId;
    }

    @Lock(LockType.READ)
    public boolean isCapacityExceeded() {
        return capacityExceeded;
    }

    @Lock(LockType.WRITE)
    public void signalCapacityExceeded() {
        capacityExceeded = true;
    }
}
