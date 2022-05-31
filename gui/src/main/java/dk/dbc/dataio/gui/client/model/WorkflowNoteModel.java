package dk.dbc.dataio.gui.client.model;

import java.io.Serializable;

/**
 * WorkflowNoteModel holds all GUI data related to showing the WorkflowNote Model
 */
public class WorkflowNoteModel implements Serializable {
    private boolean processed;
    private String assignee;
    private String description;

    public WorkflowNoteModel(boolean processed, String assignee, String description) {
        this.processed = processed;
        this.assignee = assignee;
        this.description = description;
    }

    public WorkflowNoteModel() {
        this(false, "", "");
    }


    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowNoteModel)) return false;

        WorkflowNoteModel that = (WorkflowNoteModel) o;

        if (processed != that.processed) return false;
        if (assignee != null ? !assignee.equals(that.assignee) : that.assignee != null) return false;
        return !(description != null ? !description.equals(that.description) : that.description != null);

    }

    @Override
    public int hashCode() {
        int result = processed ? 1 : 0;
        result = 31 * result + (assignee != null ? assignee.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
