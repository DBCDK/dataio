package dk.dbc.dataio.jobstore.types;

import java.util.Date;

public class StateElement {
    private Date beginDate;
    private Date endDate;
    private int pending;
    private int active;
    private int done;
    private int succeeded;
    private int failed;
    private int ignored;

    public StateElement() {
        this.beginDate = null;
        this.endDate = null;
        this.pending = 0;
        this.active = 0;
        this.done = 0;
        this.succeeded = 0;
        this.failed = 0;
        this.ignored = 0;
    }

    public StateElement(StateElement stateElement) {
        this.beginDate = stateElement.getBeginDate();
        this.endDate = stateElement.getEndDate();
        this.pending = stateElement.getPending();
        this.active = stateElement.getActive();
        this.done = stateElement.getDone();
        this.succeeded = stateElement.getSucceeded();
        this.failed = stateElement.getFailed();
        this.ignored = stateElement.getIgnored();
    }

    public Date getBeginDate() {
        return this.beginDate == null ? null : new Date(this.beginDate.getTime());
    }

    public Date getEndDate() {
        return this.endDate == null ? null : new Date(this.endDate.getTime());
    }

    public int getPending() {
        return pending;
    }

    public int getActive() {
        return active;
    }

    public int getDone() {
        return done;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public int getIgnored() {
        return ignored;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate == null? null : new Date(beginDate.getTime());
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate == null? null : new Date(endDate.getTime());
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public void setDone(int done) {
        this.done = done;
    }

    public void setSucceeded(int succeeded) {
        this.succeeded = succeeded;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public void setIgnored(int ignored) {
        this.ignored = ignored;
    }

}
