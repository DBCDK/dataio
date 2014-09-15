package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import org.junit.Test;

import java.util.ArrayList;
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

}
