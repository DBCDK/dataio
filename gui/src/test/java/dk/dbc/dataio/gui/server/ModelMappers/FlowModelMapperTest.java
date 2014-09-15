package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * FlowModelMapper unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowModelMapperTest {

    private static final long   ID = 746L;
    private static final long   VERSION = 8483L;
    private static final String NAME = "the name";
    private static final String DESCRIPTION = "the description";

    private static final long   FLOW_COMPONENT_ID_1 = 364L;
    private static final long   FLOW_COMPONENT_VERSION_1 = 156L;

    private static final long   FLOW_COMPONENT_ID_2 = 227884L;
    private static final long   FLOW_COMPONENT_VERSION_2 = 74L;

    @Test(expected = NullPointerException.class)
    public void toModel_nullInput_throws() {
        FlowModelMapper.toModel(null);
    }

    @Test
    public void toModel_validInputNoFlowComponents_returnsValidModelNoFlowComponents() {

        List<FlowComponent> components = new ArrayList<FlowComponent>();
        FlowContent flowContent = new FlowContent(NAME, DESCRIPTION, components);
        Flow flow = new Flow(ID, VERSION, flowContent);

        FlowModel model = FlowModelMapper.toModel(flow);
        assertThat(model.getId(), is(ID));
        assertThat(model.getVersion(), is(VERSION));
        assertThat(model.getFlowName(), is(NAME));
        assertThat(model.getDescription(), is(DESCRIPTION));
        assertThat(model.getFlowComponents().size(), is(0));
    }

    @Test
    public void toModel_validInput_returnsValidModel() {

        List<FlowComponent> components = new ArrayList<FlowComponent>();
        components.add(new FlowComponent(
                FLOW_COMPONENT_ID_1, FLOW_COMPONENT_VERSION_1, new FlowComponentContentBuilder().build()));

        components.add(new FlowComponent(
                FLOW_COMPONENT_ID_2, FLOW_COMPONENT_VERSION_2, new FlowComponentContentBuilder().build()));

        FlowContent flowContent = new FlowContent(NAME, DESCRIPTION, components);
        Flow flow = new Flow(ID, VERSION, flowContent);

        // Convert flow content to model
        FlowModel model = FlowModelMapper.toModel(flow);

        // Verify that the flow specific values have been set in the flow model.
        // (The flow component specific values have been tested in flowComponentModelMapperTest)
        assertThat(model.getId(), is(ID));
        assertThat(model.getVersion(), is(VERSION));
        assertThat(model.getFlowName(), is(NAME));
        assertThat(model.getDescription(), is(DESCRIPTION));

        // Verify that the mapping between flow model object and flow object is correct
        assertThat(flow.getId(), is(model.getId()));
        assertThat(flow.getVersion(), is(model.getVersion()));
        assertThat(flow.getContent().getName(), is(model.getFlowName()));
        assertThat(flow.getContent().getDescription(), is(model.getDescription()));
        assertThat(flow.getContent().getComponents().size(), is(2));
        assertFlowComponentEquals(flow.getContent().getComponents().get(0), model.getFlowComponents().get(0));
        assertFlowComponentEquals(flow.getContent().getComponents().get(1), model.getFlowComponents().get(1));
    }

    @Test(expected = NullPointerException.class)
    public void toFlowContent_nullInput_throwsNullPointerException() {
        FlowModelMapper.toFlowContent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowContent_validInputNoFlowComponents_throwsIllegalArgumentException() {
        // Build a FlowModel containing no flow components
        FlowModel model = new FlowModel(ID, VERSION, NAME, DESCRIPTION, new ArrayList<FlowComponentModel>());
        FlowModelMapper.toFlowContent(model);
    }

    @Test
    public void toFlowContent_validInput_returnsValidFlowContent() {

        List<FlowComponentModel> components = new ArrayList<FlowComponentModel>();
        FlowComponentModel flowComponentModel1 =
                new FlowComponentModel(FLOW_COMPONENT_ID_1,
                        FLOW_COMPONENT_VERSION_1,
                        "content navn nr. 1",
                        "svn projekt nr. 1",
                        "8957",
                        "invocation navn nr. 1",
                        "invocation method nr. 1",
                        Arrays.asList("JavaScript 1"));

        FlowComponentModel flowComponentModel2 =
                new FlowComponentModel(FLOW_COMPONENT_ID_2,
                        FLOW_COMPONENT_VERSION_2,
                        "content navn nr. 2",
                        "svn projekt nr. 2",
                        "8884",
                        "invocation navn nr. 2",
                        "invocation method nr. 2",
                        Arrays.asList("JavaScript 2"));

        components.add(flowComponentModel1);
        components.add(flowComponentModel2);

        FlowModel model = new FlowModel(ID, VERSION, NAME, DESCRIPTION, components);

        // Convert model to flow content
        FlowContent flowContent = FlowModelMapper.toFlowContent(model);

        // Verify that the correct values have been set in the flow content
        // (The flow component specific values have been tested in flowComponentModelMapperTest)
        assertThat(flowContent.getName(), is(NAME));
        assertThat(flowContent.getDescription(), is(DESCRIPTION));

        // Verify that the mapping between flow model object and flow object is correct
        assertThat(flowContent.getName(), is(model.getFlowName()));
        assertThat(flowContent.getDescription(), is(model.getDescription()));
        assertThat(flowContent.getComponents().size(), is(2));
        assertFlowComponentEquals(flowContent.getComponents().get(0), flowComponentModel1);
        assertFlowComponentEquals(flowContent.getComponents().get(1), flowComponentModel2);
    }

    private void assertFlowComponentEquals(FlowComponent flowComponent, FlowComponentModel flowComponentModel) {
        assertThat(flowComponent.getId(), is(flowComponentModel.getId()));
        assertThat(flowComponent.getVersion(), is(flowComponentModel.getVersion()));
        assertThat(Long.toString(flowComponent.getContent().getSvnRevision()), is(flowComponentModel.getSvnRevision()));
        assertThat(flowComponent.getContent().getName(), is(flowComponentModel.getName()));
        assertThat(flowComponent.getContent().getInvocationJavascriptName(), is(flowComponentModel.getInvocationJavascript()));
        assertThat(flowComponent.getContent().getSvnProjectForInvocationJavascript(), is(flowComponentModel.getSvnProject()));
        assertThat(flowComponent.getContent().getInvocationMethod(), is(flowComponentModel.getInvocationMethod()));
        assertThat(flowComponent.getContent().getJavascripts().size(), is(flowComponentModel.getJavascriptModules().size()));
        assertJavaScriptsEquals(flowComponent.getContent().getJavascripts(), flowComponentModel.getJavascriptModules());

    }

    private void assertJavaScriptsEquals(List<JavaScript> javaScripts, List<String> javaScriptModules) {
        assertThat(javaScripts.size(), is(javaScriptModules.size()));
        for (int i = 0; i > javaScripts.size(); i ++) {
            assertThat(javaScripts.get(i).getJavascript(), is(javaScriptModules.get(i)));
        }
    }

}
