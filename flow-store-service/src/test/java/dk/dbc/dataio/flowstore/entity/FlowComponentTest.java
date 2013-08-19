package dk.dbc.dataio.flowstore.entity;

import dk.dbc.dataio.flowstore.util.json.JsonException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
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
    public void setContent_jsonDataArgIsValidFlowComponentJson_setsContent() throws Exception {
        final String name = "testComponent";
        final String jsonData = String.format("{\"name\": \"%s\"}", name);

        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent(jsonData);
        assertThat(flowComponent.getContent(), is(jsonData));
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsInvalidJson_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent("<not_json/>");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsEmpty_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent("");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgIsNull_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent(null);
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgDoesNotContainNameMember_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent("{\"description\": \"text\"}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgNameMemberIsNull_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent("{\"name\": null}");
    }

    @Test(expected = JsonException.class)
    public void setContent_jsonDataArgNameMemberIsEmpty_throws() throws Exception {
        final FlowComponent flowComponent = new FlowComponent();
        flowComponent.setContent("{\"name\": \"\"}");
    }
}
