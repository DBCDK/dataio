package dk.dbc.dataio.flowstore.entity;

import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Submitter unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class SubmitterTest {

    @Test
    public void setContent_jsonDataArgIsInvalidSubmitterContent_throws() {
        Submitter submitter = new Submitter();
        assertThrows(JSONBException.class, () -> submitter.setContent("{}"));
    }

    @Test
    public void setContent_jsonDataArgIsInvalidJson_throws() {
        Submitter submitter = new Submitter();
        assertThrows(JSONBException.class, () -> submitter.setContent("{"));
    }

    @Test
    public void setContent_jsonDataArgIsEmpty_throws() {
        Submitter submitter = new Submitter();
        assertThrows(JSONBException.class, () -> submitter.setContent(""));
    }

    @Test
    public void setContent_jsonDataArgIsNull_throws() {
        Submitter submitter = new Submitter();
        assertThrows(IllegalArgumentException.class, () -> submitter.setContent(null));
    }
}
