package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JavaScriptBuilder;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * FlowComponentModelMapper unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowComponentModelMapperTest {
    private static final long   ID = 434L;
    private static final long   VERSION = 215L;
    private static final String NAME = "Flow Component Model Navn";
    private static final String SVN_PROJECT = "Svn Project";
    private static final long   SVN_REVISION_LONG = 747L;
    private static final String SVN_REVISION_STR = String.valueOf(SVN_REVISION_LONG);
    private static final String JAVASCRIPT_NAME = "Javascript Name";
    private static final String INVOCATION_METHOD = "Invocation Method";
    private static final String JAVASCRIPT_1 = "javascript code no. 1";
    private static final String JAVASCRIPT_2 = "javascript code no. 2";
    private static final String MODULE_NAME_1 = "module name no. 1";
    private static final String MODULE_NAME_2 = "module name no. 2";

    @Test(expected = NullPointerException.class)
    public void toModel_nullInput_throws() {
        FlowComponentModelMapper.toModel(null);
    }

    @Test
    public void toModel_validInputNoJavascripts_returnsValidModelNoJavascripts() {
        // Build a FlowComponent containing no javascripts
        FlowComponentContent flowComponentContent = new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION_LONG, JAVASCRIPT_NAME, new ArrayList<JavaScript>(), INVOCATION_METHOD);
        FlowComponent flowComponent = new FlowComponent(ID, VERSION, flowComponentContent);

        FlowComponentModel model = FlowComponentModelMapper.toModel(flowComponent);

        assertThat(model.getId(), is(ID));
        assertThat(model.getVersion(), is(VERSION));
        assertThat(model.getName(), is(NAME));
        assertThat(model.getSvnProject(), is(SVN_PROJECT));
        assertThat(model.getSvnRevision(), is(SVN_REVISION_STR));
        assertThat(model.getInvocationJavascript(), is(JAVASCRIPT_NAME));
        assertThat(model.getJavascriptModules().size(), is(0));
        assertThat(model.getInvocationMethod(), is(INVOCATION_METHOD));
    }

    @Test
    public void toModel_validInput_returnsValidModel() {
        // Build a FlowComponent containing two javascripts
        List<JavaScript> javaScriptList = new ArrayList<JavaScript>();
        javaScriptList.add(new JavaScript(JAVASCRIPT_1, MODULE_NAME_1));
        javaScriptList.add(new JavaScript(JAVASCRIPT_2, MODULE_NAME_2));

        FlowComponentContent flowComponentContent = new FlowComponentContent(NAME, SVN_PROJECT, SVN_REVISION_LONG, JAVASCRIPT_NAME, javaScriptList, INVOCATION_METHOD);
        FlowComponent flowComponent = new FlowComponent(ID, VERSION, flowComponentContent);

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

    @Test(expected = NullPointerException.class)
    public void toFlowComponentContent_nullInput_throwsNullPointerException() {
        FlowComponentModelMapper.toFlowComponentContent(null);
    }

    private FlowComponentModel buildValidFlowComponentModelContainingTwoJavascripts() {
        List<String> javascriptList = new ArrayList<String>();
        javascriptList.add(MODULE_NAME_1);
        javascriptList.add(MODULE_NAME_2);
        return new FlowComponentModel(ID, VERSION, NAME, SVN_PROJECT, SVN_REVISION_STR, JAVASCRIPT_NAME, INVOCATION_METHOD, javascriptList);
    }

    @Test
    public void toFlowComponentContent_validInput_returnsValidFlowComponentContent() {
        FlowComponentModel model = buildValidFlowComponentModelContainingTwoJavascripts();

        FlowComponentContent flowComponentContent = FlowComponentModelMapper.toFlowComponentContent(model);

        assertThat(flowComponentContent.getName(), is(NAME));
        assertThat(flowComponentContent.getSvnProjectForInvocationJavascript(), is(SVN_PROJECT));
        assertThat(flowComponentContent.getSvnRevision(), is(Long.parseLong(SVN_REVISION_STR)));
        assertThat(flowComponentContent.getInvocationJavascriptName(), is(JAVASCRIPT_NAME));
        assertThat(flowComponentContent.getInvocationMethod(), is(INVOCATION_METHOD));
        assertThat(flowComponentContent.getJavascripts().get(0).getModuleName(), is(MODULE_NAME_1));
        assertThat(flowComponentContent.getJavascripts().get(1).getModuleName(), is(MODULE_NAME_2));
    }

    @Test
    public void toFlowComponent_validInput_returnsValidFlowComponent() {
        FlowComponentModel model = buildValidFlowComponentModelContainingTwoJavascripts();

        FlowComponent flowComponent = FlowComponentModelMapper.toFlowComponent(model);

        assertThat(flowComponent.getId(), is(model.getId()));
        assertThat(flowComponent.getVersion(), is(model.getVersion()));
        assertThat(flowComponent.getContent().getName(), is(NAME));
        assertThat(flowComponent.getContent().getSvnProjectForInvocationJavascript(), is(SVN_PROJECT));
        assertThat(flowComponent.getContent().getSvnRevision(), is(Long.parseLong(SVN_REVISION_STR)));
        assertThat(flowComponent.getContent().getInvocationJavascriptName(), is(JAVASCRIPT_NAME));
        assertThat(flowComponent.getContent().getInvocationMethod(), is(INVOCATION_METHOD));
        assertThat(flowComponent.getContent().getJavascripts().get(0).getModuleName(), is(MODULE_NAME_1));
        assertThat(flowComponent.getContent().getJavascripts().get(1).getModuleName(), is(MODULE_NAME_2));
    }

    @Test
    public void toListOfFlowComponentModels_validInput_returnsValidListOfFlowComponentModels() {

        final long ID_1 = 4;
        final long VERSION_1 = 5;
        final String NAME_1 = "Name of second flow component";
        final String SVN_PROJECT_1 = "Svn Project_1";
        final long   SVN_REVISION_1 = 888L;
        final String JAVASCRIPT_NAME_1 = "Javascript Name_1";
        final String INVOCATION_METHOD_1 = "Invocation Method_1";
        final String JAVASCRIPT_1 = "javascript code no. 1";
        final String JAVASCRIPT_2 = "javascript code no. 2";

        List<JavaScript> javaScriptList1  = Arrays.asList(new JavaScriptBuilder().setModuleName(JAVASCRIPT_1).build());
        List<JavaScript> javaScriptList2  = Arrays.asList(new JavaScriptBuilder().setModuleName(JAVASCRIPT_2).build());

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
        FlowComponent flowComponent1 = new FlowComponentBuilder().setId(ID).setVersion(VERSION).setContent(flowComponentContent1).build();
        FlowComponent flowComponent2 = new FlowComponentBuilder().setId(ID_1).setVersion(VERSION_1).setContent(flowComponentContent2).build();

        // Create new List and add flow components
        List<FlowComponent> flowComponents = new ArrayList<FlowComponent>();
        flowComponents.add(flowComponent1);
        flowComponents.add(flowComponent2);

        // Convert the list containing 2 backend flowComponent objects to a list containing 2 model objects
        List<FlowComponentModel> flowComponentModules = FlowComponentModelMapper.toListOfFlowComponentModels(flowComponents);

        // Assert that the values have been correctly mapped between the 2 lists
        assertFlowComponentModelEquals(flowComponents.get(0), flowComponentModules.get(0));
        assertFlowComponentModelEquals(flowComponents.get(1), flowComponentModules.get(1));
    }

    @Test
    public void toListOfFlowComponents_validInput_returnsValidListOfFlowComponents() {

        final long ID_1 = 4;
        final long VERSION_1 = 5;
        final String NAME_1 = "Name of second flow component";
        final String SVN_PROJECT_1 = "Svn Project_1";
        final long   SVN_REVISION_1 = 888L;
        final String JAVASCRIPT_NAME_1 = "Javascript Name_1";
        final String INVOCATION_METHOD_1 = "Invocation Method_1";
        final String JAVASCRIPT_1 = "javascript code no. 1";
        final String JAVASCRIPT_2 = "javascript code no. 2";

        // Create two FlowComponentModels
        FlowComponentModel flowComponentModel1 =
                new FlowComponentModel(ID, VERSION, NAME, SVN_PROJECT, SVN_REVISION_STR, JAVASCRIPT_NAME, INVOCATION_METHOD, Arrays.asList(JAVASCRIPT_1));

        FlowComponentModel flowComponentModel2 =
                new FlowComponentModel(ID_1, VERSION_1, NAME_1, SVN_PROJECT_1, Long.toString(SVN_REVISION_1), JAVASCRIPT_NAME_1, INVOCATION_METHOD_1,Arrays.asList(JAVASCRIPT_2));

        //Create new list and add models
        List<FlowComponentModel> flowComponentModules = new ArrayList<FlowComponentModel>();
        flowComponentModules.add(flowComponentModel1);
        flowComponentModules.add(flowComponentModel2);

        // Convert the list containing 2 model objects to a list containing 2 flowComponent objects
        List<FlowComponent> flowComponents = FlowComponentModelMapper.toListOfFlowComponents(flowComponentModules);

        // Assert that the values have been correctly mapped between the 2 objects
        assertFlowComponentModelEquals(flowComponents.get(0), flowComponentModules.get(0));
        assertFlowComponentModelEquals(flowComponents.get(1), flowComponentModules.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_emptyName_throwsIllegalArgumentException() {
        FlowComponentModel model = buildValidFlowComponentModelContainingTwoJavascripts();
        model.setName("");  // Invalidate model

        FlowComponentModelMapper.toFlowComponentContent(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_emptySvnProject_throwsIllegalArgumentException() {
        FlowComponentModel model = buildValidFlowComponentModelContainingTwoJavascripts();
        model.setSvnProject("");  // Invalidate model

        FlowComponentModelMapper.toFlowComponentContent(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_emptySvnRevision_throwsIllegalArgumentException() {
        FlowComponentModel model = buildValidFlowComponentModelContainingTwoJavascripts();
        model.setSvnRevision("");  // Invalidate model

        FlowComponentModelMapper.toFlowComponentContent(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_emptyInvocationJavascript_throwsIllegalArgumentException() {
        FlowComponentModel model = buildValidFlowComponentModelContainingTwoJavascripts();
        model.setInvocationJavascript("");  // Invalidate model

        FlowComponentModelMapper.toFlowComponentContent(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_emptyInvocationMethod_throwsIllegalArgumentException() {
        FlowComponentModel model = buildValidFlowComponentModelContainingTwoJavascripts();
        model.setInvocationMethod("");  // Invalidate model

        FlowComponentModelMapper.toFlowComponentContent(model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowComponentContent_emptyJavascriptModules_throwsIllegalArgumentException() {
        FlowComponentModel model = buildValidFlowComponentModelContainingTwoJavascripts();
        model.setJavascriptModules(new ArrayList<String>());  // Invalidate model

        FlowComponentModelMapper.toFlowComponentContent(model);
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
        for(int i = 0; i < javaScripts.size(); i++) {
            assertThat(javaScripts.get(i).getModuleName(), is(javaScriptModules.get(i)));
        }
    }

}
