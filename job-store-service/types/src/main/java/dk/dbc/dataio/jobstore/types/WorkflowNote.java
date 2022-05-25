package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

public class WorkflowNote implements Serializable {

    private static final long serialVersionUID = -5906791615171663485L;
    private final boolean processed;
    private final String assignee;
    private final String description;

    /**
     * Class constructor
     *
     * @param processed   determines if the workflow has finished manual processing
     * @param assignee    defining who is assigned to process the workflow. Assignee cannot be null or empty
     * @param description containing any relevant information regarding the workflow
     */
    @JsonCreator
    public WorkflowNote(@JsonProperty("processed") boolean processed,
                        @JsonProperty("assignee") String assignee,
                        @JsonProperty("description") String description) throws NullPointerException, IllegalArgumentException {

        this.processed = processed;
        this.assignee = InvariantUtil.checkNotNullNotEmptyOrThrow(assignee, "assignee");
        this.description = description;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowNote)) return false;

        WorkflowNote that = (WorkflowNote) o;

        return processed == that.processed
                && assignee.equals(that.assignee)
                && !(description != null ? !description.equals(that.description) : that.description != null);

    }

    @Override
    public int hashCode() {
        int result = processed ? 1 : 0;
        result = 31 * result + assignee.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
