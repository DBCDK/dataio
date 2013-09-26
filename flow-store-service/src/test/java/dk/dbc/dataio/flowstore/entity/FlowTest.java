package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.integrationtest.ITUtil;
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
    public void setContent_jsonDataArgIsValidFlowContentJson_setsNameIndexValue() throws Exception {
        final String name = "testflow";
        final String flowContent = new ITUtil.FlowContentJsonBuilder()
                .setName(name)
                .build();

        final Flow flow = new Flow();
        flow.setContent(flowContent);
        assertThat(flow.getNameIndexValue(), is(name));
    }

     @Test(expected = JsonException.class)
     public void setContent_jsonDataArgIsInvalidFlowContent_throws() throws Exception {
         final Flow flow = new Flow();
         flow.setContent("{}");
     }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent("{");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent("");
    }

    @Test(expected = NullPointerException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final Flow flow = new Flow();
        flow.setContent(null);
    }

}
