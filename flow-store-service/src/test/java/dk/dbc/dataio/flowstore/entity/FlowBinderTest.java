package dk.dbc.dataio.flowstore.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        String flowBinderContent = new FlowBinderContentJsonBuilder()
                .setFlowId(flowId)
                .build();

        FlowBinder binder = new FlowBinder();
        binder.setContent(flowBinderContent);
        assertThat(binder.getFlowId(), is(flowId));
    }

    @Test
    public void setContent_jsonDataArgIsInvalidFlowBinderContentJson_throws() {
        FlowBinder binder = new FlowBinder();
        assertThrows(JSONBException.class, () -> binder.setContent("{}"));
    }

    @Test
    public void setContent_jsonDataArgIsInvalidJson_throws() {
        FlowBinder binder = new FlowBinder();
        assertThrows(JSONBException.class, () -> binder.setContent("{"));
    }

    @Test
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        FlowBinder binder = new FlowBinder();
        assertThrows(JSONBException.class, () -> binder.setContent(""));
    }

    @Test
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        FlowBinder binder = new FlowBinder();
        assertThrows(IllegalArgumentException.class, () -> binder.setContent(null));
    }
}
