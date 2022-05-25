package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.util.Format;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * FlowModelMapper unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowModelMapperTest {
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Format.LONG_DATE_TIME_FORMAT);
    private static final long ID = 746L;
    private static final long VERSION = 8483L;
    private static final String NAME = "the name";
    private static final String DESCRIPTION = "the description";
    private static final Date TIME_OF_FLOW_COMPONENT_UPDATE = new Date();

    private static final long FLOW_COMPONENT_ID_1 = 364L;
    private static final long FLOW_COMPONENT_VERSION_1 = 156L;

    private static final long FLOW_COMPONENT_ID_2 = 227884L;
    private static final long FLOW_COMPONENT_VERSION_2 = 74L;

    @Test
    public void toModel_validInputNoFlowComponents_returnsValidModelNoFlowComponents() {

        List<FlowComponent> components = new ArrayList<>();
        FlowContent flowContent = new FlowContent(NAME, DESCRIPTION, components, TIME_OF_FLOW_COMPONENT_UPDATE);
        Flow flow = new Flow(ID, VERSION, flowContent);

        FlowModel model = FlowModelMapper.toModel(flow);
        assertThat(model.getId(), is(ID));
        assertThat(model.getVersion(), is(VERSION));
        assertThat(model.getFlowName(), is(NAME));
        assertThat(model.getDescription(), is(DESCRIPTION));
        assertThat(model.getTimeOfFlowComponentUpdate(), is(simpleDateFormat.format(TIME_OF_FLOW_COMPONENT_UPDATE)));
        assertThat(model.getFlowComponents().size(), is(0));
    }

    @Test
    public void toModel_validInput_returnsValidModel() throws ParseException {

        List<FlowComponent> components = new ArrayList<>();
        components.add(new FlowComponentBuilder()
                .setId(FLOW_COMPONENT_ID_1)
                .build());

        components.add(new FlowComponentBuilder()
                .setId(FLOW_COMPONENT_ID_2)
                .build());

        FlowContent flowContent = new FlowContentBuilder()
                .setName(NAME)
                .setDescription(DESCRIPTION)
                .setTimeOfFlowComponentUpdate(TIME_OF_FLOW_COMPONENT_UPDATE)
                .setComponents(components)
                .build();

        Flow flow = new FlowBuilder()
                .setId(ID)
                .setVersion(VERSION)
                .setContent(flowContent)
                .build();

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
        assertThat(simpleDateFormat.format(flow.getContent().getTimeOfFlowComponentUpdate()), is(model.getTimeOfFlowComponentUpdate()));
        assertThat(flow.getContent().getComponents().size(), is(2));
        assertFlowComponentEquals(flow.getContent().getComponents().get(0), model.getFlowComponents().get(0), null);
        assertFlowComponentEquals(flow.getContent().getComponents().get(1), model.getFlowComponents().get(1), null);
    }

    @Test(expected = NullPointerException.class)
    public void toFlowContent_nullInput_throwsNullPointerException() {
        FlowModelMapper.toFlowContent(null, Collections.singletonList(new FlowComponentBuilder().build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowContent_validInputNoFlowComponents_throwsIllegalArgumentException() {
        // Build a FlowModel containing no flow components
        FlowModel model = new FlowModelBuilder().setComponents(new ArrayList<>()).build();
        FlowModelMapper.toFlowContent(model, Collections.singletonList(new FlowComponentBuilder().build()));
    }

    @Test
    public void toFlowContent_invalidFlowName_throwsIllegalArgumentException() {
        FlowComponent flowComponent = getValidFlowComponent();

        final String flowName = "*%(Illegal)_&Name - €";
        final String expectedIllegalCharacters = "[*], [%], [(], [)], [&], [€]";
        FlowModel model = new FlowModelBuilder().setName(flowName).build();

        try {
            FlowModelMapper.toFlowContent(model, Collections.singletonList(flowComponent));
            fail("Illegal flow name not detected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage().contains(expectedIllegalCharacters), is(true));
        }
    }

    @Test
    public void toFlowContent_validInput_returnsValidFlowContent() {

        final String CONTENT_NAME_1 = "content navn nr. 1";
        final String CONTENT_NAME_2 = "content navn nr. 2";
        final String SVN_PROJECT_1 = "svn projekt nr. 1";
        final String SVN_PROJECT_2 = "svn projekt nr. 2";
        final long SVN_REVISION_1 = 89;
        final long SVN_REVISION_2 = 43;
        final String INVOCATION_NAME_1 = "invocation navn nr. 1";
        final String INVOCATION_NAME_2 = "invocation navn nr. 2";
        final String INVOCATION_METHOD_1 = "invocation method nr. 1";
        final String INVOCATION_METHOD_2 = "invocation method nr. 2";
        final String MODULE_NAME_1 = "JavaScript 1";
        final String MODULE_NAME_2 = "JavaScript 2";

        List<FlowComponentModel> components = new ArrayList<>();
        FlowComponentModel flowComponentModel1 =
                new FlowComponentModelBuilder().
                        setId(FLOW_COMPONENT_ID_1).
                        setVersion(FLOW_COMPONENT_VERSION_1).
                        setName(CONTENT_NAME_1).
                        setSvnProject(SVN_PROJECT_1).
                        setSvnRevision(Long.toString(SVN_REVISION_1)).
                        setInvocationJavascript(INVOCATION_NAME_1).
                        setInvocationMethod(INVOCATION_METHOD_1).
                        setJavascriptModules(Collections.singletonList(MODULE_NAME_1)).
                        setDescription(DESCRIPTION).
                        build();

        FlowComponentModel flowComponentModel2 =
                new FlowComponentModelBuilder().
                        setId(FLOW_COMPONENT_ID_2).
                        setVersion(FLOW_COMPONENT_VERSION_2).
                        setName(CONTENT_NAME_2).
                        setSvnProject(SVN_PROJECT_2).
                        setSvnRevision(Long.toString(SVN_REVISION_2)).
                        setInvocationJavascript(INVOCATION_NAME_2).
                        setInvocationMethod(INVOCATION_METHOD_2).
                        setJavascriptModules(Collections.singletonList(MODULE_NAME_2)).
                        setDescription(DESCRIPTION).
                        build();

        components.add(flowComponentModel1);
        components.add(flowComponentModel2);

        JavaScript javaScript1 = new JavaScript("Something something", MODULE_NAME_1);
        JavaScript javaScript2 = new JavaScript("Also Something something", MODULE_NAME_2);

        FlowComponent flowComponent1 = new FlowComponentBuilder()
                .setId(FLOW_COMPONENT_ID_1)
                .setVersion(FLOW_COMPONENT_VERSION_1)
                .setContent(new FlowComponentContentBuilder()
                        .setName(CONTENT_NAME_1)
                        .setSvnProjectForInvocationJavascript(SVN_PROJECT_1)
                        .setSvnRevision(SVN_REVISION_1)
                        .setInvocationJavascriptName(INVOCATION_NAME_1)
                        .setInvocationMethod(INVOCATION_METHOD_1)
                        .setJavascripts(Collections.singletonList(javaScript1))
                        .build())
                .build();

        FlowComponent flowComponent2 = new FlowComponentBuilder()
                .setId(FLOW_COMPONENT_ID_2)
                .setVersion(FLOW_COMPONENT_VERSION_2)
                .setContent(new FlowComponentContentBuilder()
                        .setName(CONTENT_NAME_2)
                        .setSvnProjectForInvocationJavascript(SVN_PROJECT_2)
                        .setSvnRevision(SVN_REVISION_2)
                        .setInvocationJavascriptName(INVOCATION_NAME_2)
                        .setInvocationMethod(INVOCATION_METHOD_2)
                        .setJavascripts(Collections.singletonList(javaScript2))
                        .build())
                .build();

        List<FlowComponent> flowComponents = new ArrayList<>();
        flowComponents.add(flowComponent1);
        flowComponents.add(flowComponent2);

        FlowModel model = new FlowModelBuilder().setId(ID).setVersion(VERSION).setName(NAME).setDescription(DESCRIPTION).setTimeOfFlowComponentUpdate("").setComponents(components).build();

        // Convert model to flow content
        FlowContent flowContent = FlowModelMapper.toFlowContent(model, flowComponents);

        // Verify that the correct values have been set in the flow content
        // (The flow component specific values have been tested in flowComponentModelMapperTest)
        assertThat(flowContent.getName(), is(NAME));
        assertThat(flowContent.getDescription(), is(DESCRIPTION));

        // Verify that the mapping between flow model object and flow object is correct
        assertThat(flowContent.getName(), is(model.getFlowName()));
        assertThat(flowContent.getDescription(), is(model.getDescription()));
        assertThat(flowContent.getTimeOfFlowComponentUpdate(), is(nullValue()));
        assertThat(flowContent.getComponents().size(), is(2));
        assertFlowComponentEquals(flowContent.getComponents().get(0), flowComponentModel1, javaScript1.getJavascript());
        assertFlowComponentEquals(flowContent.getComponents().get(1), flowComponentModel2, javaScript2.getJavascript());
    }

    @Test(expected = NullPointerException.class)
    public void toListOfFlowModels_nullInput_throws() {
        FlowModelMapper.toListOfFlowModels(null);
    }

    @Test
    public void toListOfFlowModels_emptyInputList_returnsEmptyList() {
        List<FlowModel> flowModels = FlowModelMapper.toListOfFlowModels(new ArrayList<>());
        assertThat(flowModels.size(), is(0));
    }

    @Test
    public void toListOfFlowModels_twoValidFlows_twoValidFlowModelsReturned() {
        Flow flow1 = new FlowBuilder()
                .setId(ID)
                .build();
        Flow flow2 = new FlowBuilder()
                .setId(ID + 1)
                .build();

        List<Flow> flows = new ArrayList<>(2);
        flows.add(flow1);
        flows.add(flow2);

        List<FlowModel> flowModels = FlowModelMapper.toListOfFlowModels(flows);

        assertThat(flowModels.size(), is(2));
        assertThat(flowModels.get(0).getId(), is(flow1.getId()));
        assertThat(flowModels.get(1).getId(), is(flow2.getId()));
    }

    //------------------------------------------------------------------------------------------------------------------

    private void assertFlowComponentEquals(FlowComponent flowComponent, FlowComponentModel flowComponentModel, String javaScript) {
        assertThat(flowComponent.getId(), is(flowComponentModel.getId()));
        assertThat(flowComponent.getVersion(), is(flowComponentModel.getVersion()));
        assertThat(Long.toString(flowComponent.getContent().getSvnRevision()), is(flowComponentModel.getSvnRevision()));
        assertThat(flowComponent.getContent().getName(), is(flowComponentModel.getName()));
        assertThat(flowComponent.getContent().getInvocationJavascriptName(), is(flowComponentModel.getInvocationJavascript()));
        assertThat(flowComponent.getContent().getSvnProjectForInvocationJavascript(), is(flowComponentModel.getSvnProject()));
        assertThat(flowComponent.getContent().getInvocationMethod(), is(flowComponentModel.getInvocationMethod()));
        assertThat(flowComponent.getContent().getJavascripts().size(), is(flowComponentModel.getJavascriptModules().size()));
        assertJavaScriptsEquals(flowComponent.getContent().getJavascripts(), flowComponentModel.getJavascriptModules(), javaScript);

    }

    private void assertJavaScriptsEquals(List<JavaScript> javaScripts, List<String> javaScriptModules, String javaScript) {
        assertThat(javaScripts.size(), is(javaScriptModules.size()));
        for (int i = 0; i < javaScripts.size(); i++) {
            assertThat(javaScripts.get(i).getModuleName(), is(javaScriptModules.get(i)));
            if (javaScript != null) {
                assertThat(javaScripts.get(i).getJavascript(), is(javaScript));
            }
        }
    }

    private FlowComponent getValidFlowComponent() {
        return new FlowComponentBuilder().setId(FLOW_COMPONENT_ID_1).setVersion(FLOW_COMPONENT_VERSION_1)
                .setContent(new FlowComponentContentBuilder()
                        .setName("content navn")
                        .setSvnProjectForInvocationJavascript("svn projekt")
                        .setSvnRevision(89)
                        .setInvocationJavascriptName("invocation navn")
                        .setInvocationMethod("invocation method")
                        .setJavascripts(Collections.singletonList(new JavaScript("Javascript", "Javascript 1")))
                        .build())
                .build();
    }

}
