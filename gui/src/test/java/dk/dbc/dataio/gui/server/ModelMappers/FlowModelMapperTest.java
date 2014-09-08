package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Test(expected = NullPointerException.class)
    public void toModel_nullInput_throws() {
        FlowModelMapper.toModel(null);
    }

    @Test
    public void toModel_validInputNoFlowComponents_returnsValidModelNoFlowComponents() {
        // Build a Flow containing no flow components
        final String NAME = "the name";
        final String DESCRIPTION = "the description";
        final long   ID = 746L;
        final long   VERSION = 8483L;

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
        // Build a Flow containing two flow components
        final String NAME = "the name";
        final String DESCRIPTION = "the description";
        final long   ID = 746L;
        final long   VERSION = 8483L;
        final long   FLOW_COMPONENT_ID_1 = 364L;
        final long   FLOW_COMPONENT_VERSION_1 = 156L;
        final String CONTENT_NAME_1 = "content navn nr. 1";
        final String SVN_PROJECT_1 = "svn projekt nr. 1";
        final long   SVN_REVISION_1 = 8957L;
        final String INVOCATION_NAME_1 = "invocation navn nr. 1";
        final String INVOCATION_METHOD_1 = "invocation method nr. 1";
        final long   FLOW_COMPONENT_ID_2 = 227884L;
        final long   FLOW_COMPONENT_VERSION_2 = 74L;
        final String CONTENT_NAME_2 = "content navn nr. 2";
        final String SVN_PROJECT_2 = "svn projekt nr. 2";
        final long   SVN_REVISION_2 = 8884L;
        final String INVOCATION_NAME_2 = "invocation navn nr. 2";
        final String INVOCATION_METHOD_2 = "invocation method nr. 2";

        List<FlowComponent> components = new ArrayList<FlowComponent>();
        components.add(new FlowComponent(
                FLOW_COMPONENT_ID_1, FLOW_COMPONENT_VERSION_1,
                new FlowComponentContent(CONTENT_NAME_1, SVN_PROJECT_1, SVN_REVISION_1, INVOCATION_NAME_1, new ArrayList<JavaScript>(), INVOCATION_METHOD_1)
        ));
        components.add(new FlowComponent(
                FLOW_COMPONENT_ID_2, FLOW_COMPONENT_VERSION_2,
                new FlowComponentContent(CONTENT_NAME_2, SVN_PROJECT_2, SVN_REVISION_2, INVOCATION_NAME_2, new ArrayList<JavaScript>(), INVOCATION_METHOD_2)
        ));
        FlowContent flowContent = new FlowContent(NAME, DESCRIPTION, components);
        Flow flow = new Flow(ID, VERSION, flowContent);

        FlowModel model = FlowModelMapper.toModel(flow);
        assertThat(model.getId(), is(ID));
        assertThat(model.getVersion(), is(VERSION));
        assertThat(model.getFlowName(), is(NAME));
        assertThat(model.getDescription(), is(DESCRIPTION));
        Map<String, String> comps = model.getFlowComponents();
        assertThat(comps.size(), is(2));
        assertThat(comps.get(Long.toString(FLOW_COMPONENT_ID_1)), is(CONTENT_NAME_1));
        assertThat(comps.get(Long.toString(FLOW_COMPONENT_ID_2)), is(CONTENT_NAME_2));
    }

    @Test(expected = NullPointerException.class)
    public void toFlowContent_nullInput_throwsNullPointerException() {
        FlowModelMapper.toFlowContent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFlowContent_validInputNoFlowComponents_throwsIllegalArgumentException() {
        // Build a FlowModel containing no flow components
        final long   ID = 234L;
        final long   VERSION = 23L;
        final String NAME = "Flow Model Navn";
        final String DESCRIPTION = "Flow Model Description";
        FlowModel model = new FlowModel(ID, VERSION, NAME, DESCRIPTION, new HashMap<String, String>());

        FlowContent flowContent = FlowModelMapper.toFlowContent(model);
    }

    @Test
    public void toFlowContent_validInput_returnsValidModel() {
        // Build a FlowModel containing two flow components
        final long   ID = 234L;
        final long   VERSION = 23L;
        final String NAME = "Flow Model Navn";
        final String DESCRIPTION = "Flow Model Description";
        final String FLOW_COMPONENT_ID_1 = "435";
        final String FLOW_COMPONENT_NAME_1 = "Flow Component Name 1";
        final String FLOW_COMPONENT_ID_2 = "4444";
        final String FLOW_COMPONENT_NAME_2 = "Flow Component Name 2";

        Map<String, String> compontents = new HashMap<String, String>();
        compontents.put(FLOW_COMPONENT_ID_1, FLOW_COMPONENT_NAME_1);
        compontents.put(FLOW_COMPONENT_ID_2, FLOW_COMPONENT_NAME_2);
        FlowModel model = new FlowModel(ID, VERSION, NAME, DESCRIPTION, compontents);

        FlowContent flowContent = FlowModelMapper.toFlowContent(model);
        assertThat(flowContent.getName(), is(NAME));
        assertThat(flowContent.getDescription(), is(DESCRIPTION));
        assertThat(flowContent.getComponents().size(), is (2));
        List<FlowComponent> flowComponents = flowContent.getComponents();
        assertThat(Long.toString(flowComponents.get(0).getId()), is(FLOW_COMPONENT_ID_1));
        assertThat(flowComponents.get(0).getContent().getName(), is(FLOW_COMPONENT_NAME_1));
        assertThat(Long.toString(flowComponents.get(1).getId()), is(FLOW_COMPONENT_ID_2));
        assertThat(flowComponents.get(1).getContent().getName(), is(FLOW_COMPONENT_NAME_2));
    }

}
