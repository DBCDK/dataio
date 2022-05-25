package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.jobstore.types.WorkflowNote;

public class WorkflowNoteBuilder {

    private boolean processed = false;
    private String assignee = "initials";
    private String description = "description";

    public WorkflowNoteBuilder setProcessed(boolean processed) {
        this.processed = processed;
        return this;
    }

    public WorkflowNoteBuilder setAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    public WorkflowNoteBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public WorkflowNote build() {
        return new WorkflowNote(processed, assignee, description);
    }
}
