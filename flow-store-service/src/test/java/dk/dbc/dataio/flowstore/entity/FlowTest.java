package dk.dbc.dataio.flowstore.entity;

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

     @Test(expected = InvalidJsonException.class)
     public void setData_jsonDataArgDoesNotContainNameMember_throwsInvalidJsonException() throws Exception {
         final Flow flow = new Flow();
         flow.setData("{\"description\": \"text\"}");
     }

     @Test(expected = InvalidJsonException.class)
     public void setData_jsonDataArgNameMemberIsNull_throwsInvalidJsonException() throws Exception {
         final Flow flow = new Flow();
         flow.setData("{\"name\": null}");
     }

    @Test(expected = InvalidJsonException.class)
    public void setData_jsonDataArgNameMemberIsEmpty_throwsInvalidJsonException() throws Exception {
        final Flow flow = new Flow();
        flow.setData("{\"name\": \"\"}");
    }

    @Test(expected = InvalidJsonException.class)
    public void setData_jsonDataArgIsInvalidJson_throwsInvalidJsonException() throws Exception {
        final Flow flow = new Flow();
        flow.setData("<not_json/>");
    }

    @Test(expected = InvalidJsonException.class)
    public void setData_jsonDataArgIsEmpty_throwsInvalidJsonException() throws Exception {
        final Flow flow = new Flow();
        flow.setData("");
    }

    @Test(expected = InvalidJsonException.class)
    public void setData_jsonDataArgIsNull_throwsInvalidJsonException() throws Exception {
        final Flow flow = new Flow();
        flow.setData(null);
    }

}
