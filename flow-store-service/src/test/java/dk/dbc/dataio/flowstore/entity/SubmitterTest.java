package dk.dbc.dataio.flowstore.entity;

import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

/**
 * Submitter unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class SubmitterTest {

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsInvalidSubmitterContent_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("{}");
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("{");
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent(null);
    }
}
