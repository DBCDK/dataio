/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.types;

import dk.dbc.invariant.InvariantUtil;

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
     * @return the begin date of the state change
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
     * @return the end date of the state change
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
     * @param succeeded number (must be equal to or larger than 0)
     * @return the succeeded count
     */
    public StateChange setSucceeded(int succeeded) throws IllegalArgumentException {
        this.succeeded = InvariantUtil.checkIntLowerBoundOrThrow(succeeded, "succeeded", 0);
        return this;
    }

    /**
     * Increments succeeded count
     * @param delta increment (must be equal to or larger than 0)
     * @return the incremented succeeded counter
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
     * @param failed number (must be equal to or larger than 0)
     * @return the failed count
     */
    public StateChange setFailed(int failed) throws IllegalArgumentException {
        this.failed = InvariantUtil.checkIntLowerBoundOrThrow(failed, "failed", 0);
        return this;
    }

    /**
     * Increments failed count
     * @param delta increment (must be equal to or larger than 0)
     * @return the incremented failed counter
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
     * @param ignored number (must be equal to or larger than 0)
     * @return the ignored count
     */
    public StateChange setIgnored(int ignored) throws IllegalArgumentException {
        this.ignored = InvariantUtil.checkIntLowerBoundOrThrow(ignored, "ignored", 0);
        return this;
    }

    /**
     * Increments ignored count
     * @param delta increment (must be equal to or larger than 0)
     * @return the incremented ignored count
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
     * @return the requested phase
     */
    public StateChange setPhase(State.Phase phase) {
        this.phase = phase;
        return this;
    }
}
