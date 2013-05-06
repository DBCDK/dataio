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
    public void setData_jsonDataArgIsValidFlowJson_setsFlownameIndexValue() throws Exception {
        final String flowname = "testflow";
        final String description = "test description";
        final String jsonData = String.format("{\"flowname\": \"%s\", \"description\": \"%s\"}", flowname, description);

        Flow flow = new Flow();
        flow.setData(jsonData);
        assertThat(flow.getFlownameIndexValue(), is(flowname));
    }

     @Test(expected=InvalidJsonException.class)
     public void setData_jsonDataArgDoesNotContainFlownameMember_throwsInvalidJsonException() throws Exception {
         Flow flow = new Flow();
         flow.setData("{\"description\": \"text\"}");
     }

     @Test(expected=InvalidJsonException.class)
     public void setData_jsonDataArgFlownameMemberIsNull_throwsInvalidJsonException() throws Exception {
         Flow flow = new Flow();
         flow.setData("{\"flowname\": null}");
     }

    @Test(expected=InvalidJsonException.class)
    public void setData_jsonDataArgFlownameMemberIsEmpty_throwsInvalidJsonException() throws Exception {
        Flow flow = new Flow();
        flow.setData("{\"flowname\": \"\"}");
    }

    @Test(expected=InvalidJsonException.class)
    public void setData_jsonDataArgIsInvalidJson_throwsInvalidJsonException() throws Exception {
        Flow flow = new Flow();
        flow.setData("<not_json/>");
    }

    @Test(expected=InvalidJsonException.class)
    public void setData_jsonDataArgIsEmpty_throwsInvalidJsonException() throws Exception {
        Flow flow = new Flow();
        flow.setData("");
    }

    @Test(expected=InvalidJsonException.class)
    public void setData_jsonDataArgIsNull_throwsInvalidJsonException() throws Exception {
        Flow flow = new Flow();
        flow.setData(null);
    }

}
