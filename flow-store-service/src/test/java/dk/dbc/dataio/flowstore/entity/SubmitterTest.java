package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.flowstore.util.json.JsonException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Submitter unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SubmitterTest {
    @Test
    public void setContent_jsonDataArgIsValidSubmitterJson_setsNameIndexValue() throws Exception {
        final String name = "testsubmitter";
        final long number = 42;
        final String jsonData = String.format("{\"name\": \"%s\", \"number\": %d}", name, number);

        final Submitter submitter = new Submitter();
        submitter.setContent(jsonData);
        assertThat(submitter.getNameIndexValue(), is(name));
    }

    @Test
    public void setContent_jsonDataArgIsValidSubmitterJson_setsNumberIndexValue() throws Exception {
        final String name = "testsubmitter";
        final long number = 42;
        final String jsonData = String.format("{\"name\": \"%s\", \"number\": %d}", name, number);

        final Submitter submitter = new Submitter();
        submitter.setContent(jsonData);
        assertThat(submitter.getNumberIndexValue(), is(number));
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgDoesNotContainNameMember_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("{\"number\": 42}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgDoesNotContainNumberMember_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("{\"name\": \"test\"}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgNameMemberIsNull_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("{\"name\": null, \"number\": 42}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgNameMemberIsEmpty_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("{\"name\": \"\", \"number\": 42}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgNumberMemberIsNull_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("{\"name\": \"test\", \"number\": null}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("<not_json/>");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent("");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final Submitter submitter = new Submitter();
        submitter.setContent(null);
    }
}
