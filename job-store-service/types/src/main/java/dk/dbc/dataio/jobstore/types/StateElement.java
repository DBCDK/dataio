package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;

public class StateElement implements Serializable {
    private Date beginDate;
    private Date endDate;
    private int succeeded;
    private int failed;
    private int ignored;

    public StateElement() {
        this.beginDate = null;
        this.endDate = null;
        this.succeeded = 0;
        this.failed = 0;
        this.ignored = 0;
    }

    public StateElement(StateElement stateElement) {
        this.beginDate = stateElement.getBeginDate();
        this.endDate = stateElement.getEndDate();
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

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public int getIgnored() {
        return ignored;
    }

    public StateElement withBeginDate(Date beginDate) {
        this.beginDate = beginDate == null ? null : new Date(beginDate.getTime());
        return this;
    }

    public StateElement withEndDate(Date endDate) {
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
        return this;
    }

    public StateElement withSucceeded(int succeeded) {
        this.succeeded = succeeded;
        return this;
    }

    public StateElement withFailed(int failed) {
        this.failed = failed;
        return this;
    }

    public StateElement withIgnored(int ignored) {
        this.ignored = ignored;
        return this;
    }

    @JsonIgnore
    public int getNumberOfItems() {
        return succeeded + failed + ignored;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StateElement)) return false;

        StateElement that = (StateElement) o;

        if (failed != that.failed) return false;
        if (ignored != that.ignored) return false;
        if (succeeded != that.succeeded) return false;
        if (beginDate != null ? !beginDate.equals(that.beginDate) : that.beginDate != null) return false;
        if (endDate != null ? !endDate.equals(that.endDate) : that.endDate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = beginDate != null ? beginDate.hashCode() : 0;
        result = 31 * result + (endDate != null ? endDate.hashCode() : 0);
        result = 31 * result + succeeded;
        result = 31 * result + failed;
        result = 31 * result + ignored;
        return result;
    }

    @Override
    public String toString() {
        return "StateElement{" +
                "beginDate=" + beginDate +
                ", endDate=" + endDate +
                ", succeeded=" + succeeded +
                ", failed=" + failed +
                ", ignored=" + ignored +
                '}';
    }
}
