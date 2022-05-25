package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.jobstore.types.WorkflowNote;

public class WorkflowNoteModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private WorkflowNoteModelMapper() {
    }

    public static WorkflowNote toWorkflowNote(WorkflowNoteModel workflowNoteModel) {
        if (workflowNoteModel != null) {
            return new WorkflowNote(workflowNoteModel.isProcessed(), workflowNoteModel.getAssignee(), workflowNoteModel.getDescription());
        } else {
            return null;
        }
    }

    public static WorkflowNoteModel toWorkflowNoteModel(WorkflowNote workflowNote) {
        if (workflowNote != null) {
            return new WorkflowNoteModel(workflowNote.isProcessed(), workflowNote.getAssignee(), workflowNote.getDescription());
        } else {
            return new WorkflowNoteModel();
        }
    }
}
