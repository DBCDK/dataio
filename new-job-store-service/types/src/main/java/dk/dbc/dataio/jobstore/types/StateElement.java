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

    public Date getBeginDate() {
        return beginDate;
    }

    public Date getEndDate() {
        return endDate;
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
        this.beginDate = beginDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
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
