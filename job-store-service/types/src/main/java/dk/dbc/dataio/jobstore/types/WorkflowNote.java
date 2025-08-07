package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

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
        this.assignee = assignee;
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
    public boolean equals(Object object) {
        if (!(object instanceof WorkflowNote that)) return false;
        return processed == that.processed && Objects.equals(assignee, that.assignee) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processed, assignee, description);
    }
}
