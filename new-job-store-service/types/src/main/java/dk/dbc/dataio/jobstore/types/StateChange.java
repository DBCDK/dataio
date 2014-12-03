package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.Date;

public class StateChange {
    private Date beginDate;
    private Date endDate;
    private int succeeded;
    private int failed;
    private int ignored;
    private int pending;
    private int active;
    private JobState.OperationalState operationalState;

    public StateChange() {
        this.beginDate = null;
        this.endDate = null;
        this.succeeded = 0;
        this.failed = 0;
        this.ignored = 0;
        this.pending = 0;
        this.active = 0;
        this.operationalState = null;
    }

    /**
     * @return the date marking the begin time stamp
     */
    public Date getBeginDate() {
        return beginDate;
    }

    /**
     * Sets the begin date
     * @param beginDate marking the start time
     */
    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    /**
     * @return the date marking the end time stamp (null if end is not reached)
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date
     * @param endDate marking the end time
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * @return the increment of chunks marked as succeeded
     */
    public int getSucceeded() {
        return succeeded;
    }

    /**
     * Sets the number which status succeeded is to be incremented with
     * (must be equal or larger than 0)
     * @param succeeded number
     */
    public void setSucceeded(int succeeded) {
        this.succeeded = (int)InvariantUtil.checkLowerBoundOrThrow(succeeded, "succeeded", 0);
    }

    /**
     * @return the increment of chunks marked as failed
     */
    public int getFailed() {
        return failed;
    }

    /**
     * Sets the number which status failed is to be incremented with
     * (must be equal or larger than 0)
     * @param failed number
     */
    public void setFailed(int failed) {
        this.failed = (int)InvariantUtil.checkLowerBoundOrThrow(failed, "failed", 0);
    }

    /**
     * @return the increment of chunks marked as ignored
     */
    public int getIgnored() {
        return ignored;
    }

    /**
     * Sets the number which status ignored is to be incremented with
     * (must be equal or larger than 0)
     * @param ignored number
     */
    public void setIgnored(int ignored) {
        this.ignored = (int)InvariantUtil.checkLowerBoundOrThrow(ignored, "ignored", 0);
    }

    /**
     * @return the increment of chunks with status pending
     */
    public int getPending() {
        return pending;
    }

    /**
     * Sets the number which the lifecycle pending is to be incremented with
     * @param pending number
     */
    public void setPending(int pending) {
        this.pending = pending;
    }

    /**
     * @return the increment of chunks with status active
     */
    public int getActive() {
        return active;
    }

    /**
     * Sets the number which the lifecycle active is to be incremented with
     * @param active number
     */
    public void setActive(int active) {
        this.active = active;
    }

    /**
     * @return the operationalState (chunkifying, processing, delivering)
     */
    public JobState.OperationalState getOperationalState() {
        return operationalState;
    }

    /**
     * /**
     * Sets the operational state
     * @param operationalState (Chunkifying, processing, ignored)
     */
    public void setOperationalState(JobState.OperationalState operationalState) {
        this.operationalState = operationalState;
    }

    /*
     * Private methods
     */
    private int validateStatus(int number, String parameterName) {
        return (int)InvariantUtil.checkLowerBoundOrThrow(number, parameterName, 0);
    }
}
