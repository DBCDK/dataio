package dk.dbc.dataio.jobstore.types;

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
    private State.Phase phase;

    public StateChange() {
        this.beginDate = null;
        this.endDate = null;
        this.succeeded = 0;
        this.failed = 0;
        this.ignored = 0;
        this.pending = 0;
        this.active = 0;
        this.phase = null;
    }

    /**
     * @return the date marking the begin time stamp
     */
    public Date getBeginDate() {
        return beginDate != null ? new Date(beginDate.getTime()) : null;
    }

    /**
     * Sets the begin date
     * @param beginDate marking the start time
     */
    public StateChange setBeginDate(Date beginDate) {
        this.beginDate = new Date(beginDate.getTime());
        return this;
    }

    /**
     * @return the date marking the end time stamp (null if end is not reached)
     */
    public Date getEndDate() {
        return endDate != null ? new Date(endDate.getTime()) : null;
    }

    /**
     * Sets the end date
     * @param endDate marking the end time
     */
    public StateChange setEndDate(Date endDate) {
        this.endDate = new Date(endDate.getTime());
        return this;
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
    public StateChange setSucceeded(int succeeded) {
        this.succeeded = (int)InvariantUtil.checkLowerBoundOrThrow(succeeded, "succeeded", 0);
        return this;
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
    public StateChange setFailed(int failed) {
        this.failed = (int)InvariantUtil.checkLowerBoundOrThrow(failed, "failed", 0);
        return this;
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
    public StateChange setIgnored(int ignored) {
        this.ignored = (int)InvariantUtil.checkLowerBoundOrThrow(ignored, "ignored", 0);
        return this;
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
    public StateChange setPending(int pending) {
        this.pending = pending;
        return this;
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
    public StateChange setActive(int active) {
        this.active = active;
        return this;
    }

    /**
     * @return the phase (partitioning, processing, delivering)
     */
    public State.Phase getPhase() {
        return phase;
    }

    /**
     * /**
     * Sets the phase
     * @param phase (partitioning, processing, delivering)
     */
    public StateChange setPhase(State.Phase phase) {
        this.phase = phase;
        return this;
    }
}
