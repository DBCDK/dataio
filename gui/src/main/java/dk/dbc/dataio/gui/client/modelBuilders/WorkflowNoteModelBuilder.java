package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;

public class WorkflowNoteModelBuilder {

    private boolean processed = false;
    private String assignee = "assignee";
    private String description = "description";

    /**
     * Sets the processed indicator for the workflow note
     *
     * @param processed boolean
     * @return The WorkflowNoteModelBuilder object itself (for chaining)
     */
    public WorkflowNoteModelBuilder setProcessed(boolean processed) {
        this.processed = processed;
        return this;
    }

    /**
     * Sets the assignee for the workflow note
     *
     * @param assignee the person assigned
     * @return The WorkflowNoteModelBuilder object itself (for chaining)
     */
    public WorkflowNoteModelBuilder setAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    /**
     * Sets the description for the workflow note
     *
     * @param description the description
     * @return The WorkflowNoteModelBuilder object itself (for chaining)
     */
    public WorkflowNoteModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Build the WorkflowNoteModel object
     *
     * @return The WorkflowNoteModel object
     */
    public WorkflowNoteModel build() {
        return new WorkflowNoteModel(processed, assignee, description);
    }
}
