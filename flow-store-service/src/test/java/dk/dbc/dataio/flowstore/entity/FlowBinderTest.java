package dk.dbc.dataio.flowstore.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * FlowBinder unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowBinderTest {
    @Test
    public void setContent_jsonDataArgIsValidFlowBinderContentJson_setsFlowId() throws Exception {
        final long flowId = 42L;
        final String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .build();

        final FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        assertThat(binder.getFlowId(), is(flowId));
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsInvalidFlowBinderContentJson_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("{}");
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("{");
    }

    @Test(expected = JSONBException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final FlowBinder binder = new FlowBinder();
        binder.setContent(null);
    }
}
