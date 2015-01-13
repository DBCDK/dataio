package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.Date;

public class StateChange {
    private Date beginDate;
    private Date endDate;
    private int succeeded;
    private int failed;
    private int ignored;
    private State.Phase phase;

    public StateChange() {
        this.beginDate = null;
        this.endDate = null;
        this.succeeded = 0;
        this.failed = 0;
        this.ignored = 0;
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
        this.beginDate = beginDate == null ? null : new Date(beginDate.getTime());
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
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
        return this;
    }

    /**
     * @return the succeeded count
     */
    public int getSucceeded() {
        return succeeded;
    }

    /**
     * Sets succeeded count
     * @param succeeded number (must be equal or larger than 0)
     */
    public StateChange setSucceeded(int succeeded) throws IllegalArgumentException {
        this.succeeded = InvariantUtil.checkIntLowerBoundOrThrow(succeeded, "succeeded", 0);
        return this;
    }

    /**
     * Increments succeeded count
     * @param delta increment (must be equal or larger than 0)
     */
    public StateChange incSucceeded(int delta) throws IllegalArgumentException {
        succeeded += InvariantUtil.checkIntLowerBoundOrThrow(delta, "delta", 0);
        return this;
    }

    /**
     * @return the failed count
     */
    public int getFailed() {
        return failed;
    }

    /**
     * Sets failed count
     * @param failed number (must be equal or larger than 0)
     */
    public StateChange setFailed(int failed) throws IllegalArgumentException {
        this.failed = InvariantUtil.checkIntLowerBoundOrThrow(failed, "failed", 0);
        return this;
    }

    /**
     * Increments failed count
     * @param delta increment (must be equal or larger than 0)
     */
    public StateChange incFailed(int delta) throws IllegalArgumentException {
        failed += InvariantUtil.checkIntLowerBoundOrThrow(delta, "delta", 0);
        return this;
    }

    /**
     * @return the ignored count
     */
    public int getIgnored() {
        return ignored;
    }

    /**
     * Sets ignored count
     * @param ignored number (must be equal or larger than 0)
     */
    public StateChange setIgnored(int ignored) throws IllegalArgumentException {
        this.ignored = InvariantUtil.checkIntLowerBoundOrThrow(ignored, "ignored", 0);
        return this;
    }

    /**
     * Increments ignored count
     * @param delta increment (must be equal or larger than 0)
     */
    public StateChange incIgnored(int delta) throws IllegalArgumentException {
        ignored += InvariantUtil.checkIntLowerBoundOrThrow(delta ,"delta", 0);
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
