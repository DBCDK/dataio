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
    public void setData_jsonDataArgIsValidFlowJson_setsFlownameIndexValue() {
        final String flowname = "testflow";
        final String description = "test description";
        final String jsonData = String.format("{\"flowname\": \"%s\", \"description\": \"%s\"}", flowname, description);

        Flow flow = new Flow();
        flow.setData(jsonData);
        assertThat(flow.getFlownameIndexValue(), is(flowname));
    }
}
