package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Constants;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import java.time.Duration;

/**
 * The purpose of this singleton bean is to maintain a flag indicating whether or not this processor has exceeded its
 * maximum capacity
 */
@Singleton
public class CapacityBean {
    public final static Duration MAXIMUM_TIME_TO_PROCESS = Duration.ofMinutes(3);

    private boolean timeout = false;
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
    public boolean isTimeout() {
        return timeout;
    }

    @Lock(LockType.WRITE)
    public void signalTimeout() {
        timeout = true;
    }
}
