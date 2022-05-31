package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JavaScriptBuilder;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher.fetchRequiredJavaScriptResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * FlowComponentModelMapper unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowComponentModelMapperTest {
    private static final long ID = 434L;
    private static final long VERSION = 215L;
    private static final String NAME = "Flow Component Model Navn";
    private static final String NEXT_NAME = "Next Flow Component Model Navn";
    private static final String DESCRIPTION = "description";
    private static final String NEXT_DESCRIPTION = "next description";
    private static final String SVN_PROJECT = "Svn Project";
    private static final String NEXT_SVN_PROJECT = "Next Svn Project";
    private static final long SVN_REVISION_LONG = 747L;
    private static final long NEXT_SVN_REVISION_LONG = 748L;
    private static final String SVN_REVISION_STR = String.valueOf(SVN_REVISION_LONG);
    private static final String NEXT_SVN_REVISION_STR = String.valueOf(NEXT_SVN_REVISION_LONG);
    private static final String JAVASCRIPT_NAME = "Javascript Name";
    private static final String NEXT_JAVASCRIPT_NAME = "Next Javascript Name";
    private static final String INVOCATION_METHOD = "Invocation Method";
    private static final String NEXT_INVOCATION_METHOD = "Next Invocation Method";
    private static final String JAVASCRIPT_1 = "javascript code no. 1";
    private static final String NEXT_JAVASCRIPT_1 = "next javascript code no. 1";
    private static final String JAVASCRIPT_2 = "javascript code no. 2";
    private static final String NEXT_JAVASCRIPT_2 = "next javascript code no. 2";
    private static final String MODULE_NAME_1 = "module name no. 1";
    private static final String NEXT_MODULE_NAME_1 = "next module name no. 1";
    private static final String MODULE_NAME_2 = "module name no. 2";
    private static final String NEXT_MODULE_NAME_2 = "next module name no. 2";

    @Test
    public void toModel_validInputNoJavascripts_returnsValidModelNoJavascripts() {
        // Build a FlowComponent containing no javascripts
        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(NAME)
                .setDescription(DESCRIPTION)
                .setSvnProjectForInvocationJavascript(SVN_PROJECT)
                .setSvnRevision(SVN_REVISION_LONG)
                .setInvocationJavascriptName(JAVASCRIPT_NAME)
                .setInvocationMethod(INVOCATION_METHOD)
                .setJavascripts(Collections.emptyList())
                .build();
        FlowComponentContent nextFlowComponentContent = new FlowComponentContentBuilder()
                .setSvnRevision(NEXT_SVN_REVISION_LONG)
                .build();

        FlowComponent flowComponent = new FlowComponentBuilder()
                .setId(ID)
                .setVersion(VERSION)
                .setContent(flowComponentContent)
                .setNext(nextFlowComponentContent)
                .build();

        FlowComponentModel model = FlowComponentModelMapper.toModel(flowComponent);

        assertThat(model.getId(), is(ID));
        assertThat(model.getVersion(), is(VERSION));
        assertThat(model.getName(), is(NAME));
        assertThat(model.getSvnProject(), is(SVN_PROJECT));
        assertThat(model.getSvnRevision(), is(SVN_REVISION_STR));
        assertThat(model.getInvocationJavascript(), is(JAVASCRIPT_NAME));
        assertThat(model.getJavascriptModules().size(), is(0));
        assertThat(model.getInvocationMethod(), is(INVOCATION_METHOD));
        assertThat(model.getSvnNext(), is(NEXT_SVN_REVISION_STR));
    }

    @Test
    public void toModel_validInput_returnsValidModel() {
        // Build a FlowComponent containing two javascripts
        List<JavaScript> javaScriptList = new ArrayList<>();
        javaScriptList.add(new JavaScriptBuilder().setJavascript(JAVASCRIPT_1).setModuleName(MODULE_NAME_1).build());
        javaScriptList.add(new JavaScriptBuilder().setJavascript(JAVASCRIPT_2).setModuleName(MODULE_NAME_2).build());

        List<JavaScript> nextJavaScriptList = new ArrayList<>();
        nextJavaScriptList.add(new JavaScriptBuilder().setJavascript(NEXT_JAVASCRIPT_1).setModuleName(NEXT_MODULE_NAME_1).build());
        nextJavaScriptList.add(new JavaScriptBuilder().setJavascript(NEXT_JAVASCRIPT_2).setModuleName(NEXT_MODULE_NAME_2).build());

        FlowComponentContent flowComponentContent = new FlowComponentContentBuilder()
                .setName(NAME)
                .setDescription(DESCRIPTION)
                .setSvnProjectForInvocationJavascript(SVN_PROJECT)
                .setSvnRevision(SVN_REVISION_LONG)
                .setInvocationJavascriptName(JAVASCRIPT_NAME)
                .setInvocationMethod(INVOCATION_METHOD)
                .setJavascripts(javaScriptList)
                .build();

        FlowComponentContent nextFlowComponentContent = new FlowComponentContentBuilder()
                .setName(NEXT_NAME)
                .setDescription(NEXT_DESCRIPTION)
                .setSvnProjectForInvocationJavascript(NEXT_SVN_PROJECT)
                .setSvnRevision(NEXT_SVN_REVISION_LONG)
                .setInvocationJavascriptName(NEXT_JAVASCRIPT_NAME)
                .setInvocationMethod(NEXT_INVOCATION_METHOD)
                .setJavascripts(nextJavaScriptList)
                .build();

        FlowComponent flowComponent = new FlowComponentBuilder()
                .setId(ID)
                .setVersion(VERSION)
                .setContent(flowComponentContent)
                .setNext(nextFlowComponentContent)
                .build();

        FlowComponentModel model = FlowComponentModelMapper.toModel(flowComponent);

        assertThat(model.getId(), is(ID));
        assertThat(model.getVersion(), is(VERSION));
        assertThat(model.getName(), is(NAME));
        assertThat(model.getSvnProject(), is(SVN_PROJECT));
        assertThat(model.getSvnRevision(), is(SVN_REVISION_STR));
        assertThat(model.getInvocationJavascript(), is(JAVASCRIPT_NAME));
        assertThat(model.getJavascriptModules().size(), is(2));
        assertThat(model.getInvocationMethod(), is(INVOCATION_METHOD));
        assertThat(model.getJavascriptModules().get(0), is(MODULE_NAME_1));
        assertThat(model.getJavascriptModules().get(1), is(MODULE_NAME_2));
    }

    @Test
    public void toListOfFlowComponentModels_validInput_returnsValidListOfFlowComponentModels() {

        final long ID_1 = 4;
        final long VERSION_1 = 5;
        final String NAME_1 = "Name of second flow component";
        final String SVN_PROJECT_1 = "Svn Project_1";
        final long SVN_REVISION_1 = 888L;
        final String JAVASCRIPT_NAME_1 = "Javascript Name_1";
        final String INVOCATION_METHOD_1 = "Invocation Method_1";
        final String JAVASCRIPT_1 = "javascript code no. 1";
        final String JAVASCRIPT_2 = "javascript code no. 2";

        List<JavaScript> javaScriptList1 = Collections.singletonList(new JavaScriptBuilder().setModuleName(JAVASCRIPT_1).build());
        List<JavaScript> javaScriptList2 = Collections.singletonList(new JavaScriptBuilder().setModuleName(JAVASCRIPT_2).build());

        // Create flow component content
        FlowComponentContent flowComponentContent1 =
                new FlowComponentContentBuilder()
                        .setName(NAME)
                        .setSvnProjectForInvocationJavascript(SVN_PROJECT)
                        .setSvnRevision(SVN_REVISION_LONG)
                        .setInvocationJavascriptName(JAVASCRIPT_NAME)
                        .setJavascripts(javaScriptList1)
                        .setInvocationMethod(INVOCATION_METHOD)
                        .build();


        FlowComponentContent flowComponentContent2 =
                new FlowComponentContentBuilder()
                        .setName(NAME_1)
                        .setSvnProjectForInvocationJavascript(SVN_PROJECT_1)
                        .setSvnRevision(SVN_REVISION_1)
                        .setInvocationJavascriptName(JAVASCRIPT_NAME_1)
                        .setJavascripts(javaScriptList2)
                        .setInvocationMethod(INVOCATION_METHOD_1)
                        .build();

        // Create two flow components
        FlowComponent flowComponent1 = new FlowComponentBuilder().
                setId(ID).
                setVersion(VERSION).
                setContent(flowComponentContent1).
                setNext(new FlowComponentContentBuilder().setSvnRevision(NEXT_SVN_REVISION_LONG).build()).
                build();
        FlowComponent flowComponent2 = new FlowComponentBuilder().
                setId(ID_1).
                setVersion(VERSION_1).
                setContent(flowComponentContent2).
                setNext(new FlowComponentContentBuilder().setSvnRevision(NEXT_SVN_REVISION_LONG).build()).
                build();

        // Create new List and add flow components
        List<FlowComponent> flowComponents = new ArrayList<>();
        flowComponents.add(flowComponent1);
        flowComponents.add(flowComponent2);

        // Convert the list containing 2 backend flowComponent objects to a list containing 2 model objects
        List<FlowComponentModel> flowComponentModules = FlowComponentModelMapper.toListOfFlowComponentModels(flowComponents);

        // Assert that the values have been correctly mapped between the 2 lists
        assertFlowComponentModelEquals(flowComponents.get(0), flowComponentModules.get(0));
        assertFlowComponentModelEquals(flowComponents.get(1), flowComponentModules.get(1));
    }

    @Test(expected = NullPointerException.class)
    public void toFlowComponentContent_nullInput_throwsNullPointerException() {
        FlowComponentModelMapper.toFlowComponentContent(null, createTestFetchRequiredJavaScriptResult());
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_listOfJavaScriptsAreNull_throwsIllegalArgumentException() {
        FlowComponentModel model = new FlowComponentModelBuilder().build();
        FlowComponentModelMapper.toFlowComponentContent(model, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_listOfJavaScriptsAreEmpty_throwsIllegalArgumentException() {
        FlowComponentModel model = new FlowComponentModelBuilder().build();
        FlowComponentModelMapper.toFlowComponentContent(model, new fetchRequiredJavaScriptResult(new ArrayList<>(), null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_validInputEmptyField_throwsIllegalArgumentException() {
        FlowComponentModel model = new FlowComponentModelBuilder().setName("").build();
        FlowComponentModelMapper.toFlowComponentContent(model, createTestFetchRequiredJavaScriptResult());
    }

    @Test
    public void toFlowComponentContent_invalidFlowComponentName_throwsIllegalArgumentException() {
        final String flowComponentName = "*%(Illegal)_&Name - €";
        final String expectedIllegalCharacters = "[*], [%], [(], [)], [&], [€]";

        final FlowComponentModel flowComponentModel = new FlowComponentModelBuilder().setName(flowComponentName).build();

        try {
            FlowComponentModelMapper.toFlowComponentContent(flowComponentModel, createTestFetchRequiredJavaScriptResult());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage().contains(expectedIllegalCharacters), is(true));
        }
    }

    @Test
    public void toFlowComponentContent_validInput_returnsValidFlowComponentContent() {
        JavaScript javaScript = new JavaScriptBuilder().build();
        FlowComponentModel flowComponentModel = new FlowComponentModelBuilder().build();
        FlowComponentContent content = FlowComponentModelMapper.toFlowComponentContent(flowComponentModel, createTestFetchRequiredJavaScriptResult());

        assertThat(content.getName(), is(flowComponentModel.getName()));
        assertThat(content.getSvnProjectForInvocationJavascript(), is(flowComponentModel.getSvnProject()));
        assertThat(content.getSvnRevision(), is(Long.valueOf(flowComponentModel.getSvnRevision())));
        assertThat(content.getInvocationJavascriptName(), is(flowComponentModel.getInvocationJavascript()));
        assertThat(content.getInvocationMethod(), is(flowComponentModel.getInvocationMethod()));
        assertThat(content.getJavascripts().size(), is(1));
        assertThat(content.getJavascripts().get(0).getModuleName(), is(javaScript.getModuleName()));
        assertThat(content.getJavascripts().get(0).getJavascript(), is(javaScript.getJavascript()));
    }

    private void assertFlowComponentModelEquals(FlowComponent flowComponent, FlowComponentModel flowComponentModel) {
        assertThat(flowComponent.getId(), is(flowComponentModel.getId()));
        assertThat(flowComponent.getVersion(), is(flowComponentModel.getVersion()));
        assertFlowComponentContent(flowComponent.getContent(), flowComponentModel);
    }

    private void assertFlowComponentContent(FlowComponentContent flowComponentContent, FlowComponentModel flowComponentModel) {
        assertThat(flowComponentContent.getName(), is(flowComponentModel.getName()));
        assertThat(flowComponentContent.getSvnProjectForInvocationJavascript(), is(flowComponentModel.getSvnProject()));
        assertThat(Long.toString(flowComponentContent.getSvnRevision()), is(flowComponentModel.getSvnRevision()));
        assertThat(flowComponentContent.getInvocationJavascriptName(), is(flowComponentModel.getInvocationJavascript()));
        assertThat(flowComponentContent.getInvocationMethod(), is(flowComponentModel.getInvocationMethod()));
        assertJavaScriptsEquals(flowComponentContent.getJavascripts(), flowComponentModel.getJavascriptModules());
    }

    private void assertJavaScriptsEquals(List<JavaScript> javaScripts, List<String> javaScriptModules) {
        assertThat(javaScripts.size(), is(javaScriptModules.size()));
        for (int i = 0; i < javaScripts.size(); i++) {
            assertThat(javaScripts.get(i).getModuleName(), is(javaScriptModules.get(i)));
        }
    }

    private fetchRequiredJavaScriptResult createTestFetchRequiredJavaScriptResult() {
        return new fetchRequiredJavaScriptResult(Collections.singletonList(new JavaScriptBuilder().build()), null);
    }
}
