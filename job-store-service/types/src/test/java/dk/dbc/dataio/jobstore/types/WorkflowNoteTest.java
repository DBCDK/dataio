package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

public class WorkflowNoteTest {

    final static boolean PROCESSED = false;
    final static String ASSIGNEE = "initials";
    final static String DESCRIPTION = "description";

    @Test(expected = NullPointerException.class)
    public void constructor_assigneeArgIsNull_throws() {
        new WorkflowNote(PROCESSED, null, DESCRIPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_assigneeArgIsEmpty_throws() {
        new WorkflowNote(PROCESSED, "", DESCRIPTION);
    }

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
