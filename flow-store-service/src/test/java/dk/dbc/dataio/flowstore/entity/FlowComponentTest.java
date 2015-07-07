package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * FlowComponent unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowComponentTest {
    @Test
    public void setContent_jsonDataArgIsValidFlowComponentContentJson_setsNameIndexValue() throws Exception {
        final String name = "testComponent";
        final String flowComponentContent = new FlowComponentContentJsonBuilder()
                .setName(name)
                .build();

        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent(flowComponentContent);
        assertThat(flowComponent.getNameIndexValue(), is(name));
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidFlowComponentContent_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent("{}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent("{");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent("");
    }

    @Test(expected = NullPointerException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent(null);
    }

    @Test
    public void setNext_jsonDataArgIsEmpty_setsNextToNull() {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setNext(new String());
        assertThat(flowComponent.getNext(), is(nullValue()));
    }

    @Test
    public void setNext_jsonDataArgIsNull_NextIsNull() {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setNext(null);
        assertThat(flowComponent.getNext(), is(nullValue()));
    }

    @Test
    public void setNext_jsonDataArgIsValidFlowComponentContentJson_setsNext() {
        final String flowComponentContent = new FlowComponentContentJsonBuilder()
                .build();
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setNext(flowComponentContent);
        assertThat(flowComponent.getNext(), is(flowComponentContent));
    }

}
