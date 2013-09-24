package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.json.JsonException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

 /**
  * Flow unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class FlowTest {
    @Test
    public void setContent_jsonDataArgIsValidFlowJson_setsNameIndexValue() throws Exception {
        final String name = "testflow";
        final String description = "test description";
        final String jsonData = String.format("{\"name\": \"%s\", \"description\": \"%s\"}", name, description);

        final Flow flow = new Flow();
        flow.setContent(jsonData);
        assertThat(flow.getNameIndexValue(), is(name));
    }

     @Test(expected = JsonException.class)
     public void setContent_jsonDataArgDoesNotContainNameMember_throws() throws Exception {
         final Flow flow = new Flow();
         flow.setContent("{\"description\": \"text\"}");
     }

     @Test(expected = JsonException.class)
     public void setContent_jsonDataArgNameMemberIsNull_throws() throws Exception {
         final Flow flow = new Flow();
         flow.setContent("{\"name\": null}");
     }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgNameMemberIsEmpty_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent("{\"name\": \"\"}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent("<not_json/>");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent("");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent(null);
    }

}
