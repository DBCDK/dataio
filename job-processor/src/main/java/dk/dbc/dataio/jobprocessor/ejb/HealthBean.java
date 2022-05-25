package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Constants;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

@Singleton
public class HealthBean {
    private boolean terminallyIll = false;
    private Throwable cause;
    private String shardId;

    @PostConstruct
    public void initialize() {
        shardId = System.getenv().get(Constants.PROCESSOR_SHARD_ENV_VARIABLE);
    }

    @Lock(LockType.READ)
    public boolean isTerminallyIll() {
        return terminallyIll;
    }

    @Lock(LockType.READ)
    public String getShardId() {
        return shardId;
    }

    @Lock(LockType.READ)
    public Throwable getCause() {
        return cause;
    }

    @Lock(LockType.WRITE)
    public void signalTerminallyIll() {
        signalTerminallyIll(null);
    }

    @Lock(LockType.WRITE)
    public void signalTerminallyIll(Throwable cause) {
        this.cause = cause;
        terminallyIll = true;
    }
}
