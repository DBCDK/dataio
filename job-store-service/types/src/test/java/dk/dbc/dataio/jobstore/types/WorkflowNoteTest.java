package dk.dbc.dataio.jobstore.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class WorkflowNoteTest {

    final static boolean PROCESSED = false;
    final static String ASSIGNEE = "initials";
    final static String DESCRIPTION = "description";

    @Test
    public void constructor_assigneeArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new WorkflowNote(PROCESSED, null, DESCRIPTION));
    }

    @Test
    public void constructor_assigneeArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new WorkflowNote(PROCESSED, "", DESCRIPTION));
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
