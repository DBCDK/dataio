package dk.dbc.dataio.jobstore.types;

import org.junit.jupiter.api.Test;

public class WorkflowNoteTest {

    final static boolean PROCESSED = false;
    final static String ASSIGNEE = "initials";
    final static String DESCRIPTION = "description";

    @Test
    public void constructor_descriptionArgIsNull_WorkflowNoteCreated() {
        new WorkflowNote(PROCESSED, ASSIGNEE, null);
    }

    @Test
    public void constructor_descriptionArgIsEmpty_WorkflowNoteCreated() {
        new WorkflowNote(PROCESSED, ASSIGNEE, null);
    }

    @Test
    public void constructor_allArgsAreValid_WorkflowNoteCreated() {
        new WorkflowNote(PROCESSED, ASSIGNEE, DESCRIPTION);
    }
}
