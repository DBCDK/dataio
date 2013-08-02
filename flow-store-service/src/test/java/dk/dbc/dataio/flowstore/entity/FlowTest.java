package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.flowstore.util.json.JsonException;
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
    public void setData_jsonDataArgIsValidFlowJson_setsNameIndexValue() throws Exception {
        final String name = "testflow";
        final String description = "test description";
        final String jsonData = String.format("{\"name\": \"%s\", \"description\": \"%s\"}", name, description);

        final Flow flow = new Flow();
        flow.setData(jsonData);
        assertThat(flow.getNameIndexValue(), is(name));
    }

     @Test(expected = JsonException.class)
     public void setData_jsonDataArgDoesNotContainNameMember_throws() throws Exception {
         final Flow flow = new Flow();
         flow.setData("{\"description\": \"text\"}");
     }

     @Test(expected = JsonException.class)
     public void setData_jsonDataArgNameMemberIsNull_throws() throws Exception {
         final Flow flow = new Flow();
         flow.setData("{\"name\": null}");
     }

    @Test(expected = JsonException.class)
    public void setData_jsonDataArgNameMemberIsEmpty_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setData("{\"name\": \"\"}");
    }

    @Test(expected = JsonException.class)
    public void setData_jsonDataArgIsInvalidJson_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setData("<not_json/>");
    }

    @Test(expected = JsonException.class)
    public void setData_jsonDataArgIsEmpty_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setData("");
    }

    @Test(expected = JsonException.class)
    public void setData_jsonDataArgIsNull_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setData(null);
    }

}
