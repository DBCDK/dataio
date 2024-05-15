package dk.dbc.dataio.cli;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowManagerTest {

    /* To update the 'simple.jsar' archive, simply change directory into the
       'src/test/resources/jsar/simple.files' directory, edit the necessary files
       and run 'zip -r ../simple.jsar * && cp ../simple.jsar ../simple.old.jsar && zip -j ../simple.old.jsar ../simple.old.files/entrypoint.js' */

    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final String flowName = "AccTestRunnerTest";
    private final Path simpleJsar = Path.of("src", "test", "resources", "jsar", "simple.jsar");

    @Test
    void getFlow_fromJsar_flowExists() throws FlowStoreServiceConnectorException, IOException {
        Flow existingFlow = new FlowBuilder().setId(123).setVersion(456).build();
        when(flowStoreServiceConnector.findFlowByName(flowName)).thenReturn(existingFlow);

        FlowManager flowManager = new FlowManager(flowStoreServiceConnector);
        Flow flow = flowManager.getFlow(simpleJsar);
        assertThat("Flow ID", flow.getId(), is(existingFlow.getId()));
        assertThat("Flow version", flow.getVersion(), is(existingFlow.getVersion()));
        assertThat("Flow content", flow.getContent().getJsar(), is(notNullValue()));
        assertThat("foundFlowByName flag", flowManager.foundFlowByName(), is(true));
    }

    @Test
    void getFlow_fromJsar_notFound() throws FlowStoreServiceConnectorException, IOException {
        when(flowStoreServiceConnector.findFlowByName(flowName))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("not found", 404));

        FlowManager flowManager = new FlowManager(flowStoreServiceConnector);
        Flow flow = flowManager.getFlow(simpleJsar);
        assertThat("Flow ID", flow.getId(), is(1L));
        assertThat("Flow version", flow.getVersion(), is(1L));
        assertThat("Flow content", flow.getContent().getJsar(), is(notNullValue()));
        assertThat("foundFlowByName flag", flowManager.foundFlowByName(), is(false));
    }

    @Test
    void getFlow_fromJobSpecification_flowExists() throws FlowStoreServiceConnectorException {
        JobSpecification jobSpecification = new JobSpecification()
                .withPackaging("pkg")
                .withFormat("fmt")
                .withCharset("chrset")
                .withDestination("dest")
                .withSubmitterId(123456);
        FlowBinder flowBinder = new FlowBinderBuilder().build();
        when(flowStoreServiceConnector.getFlowBinder(jobSpecification.getPackaging(), jobSpecification.getFormat(),
                jobSpecification.getCharset(), jobSpecification.getSubmitterId(), jobSpecification.getDestination()))
                .thenReturn(flowBinder);

        Flow existingFlow = new FlowBuilder().setId(123).setVersion(456).build();
        when(flowStoreServiceConnector.getFlow(flowBinder.getContent().getFlowId())).thenReturn(existingFlow);

        FlowManager flowManager = new FlowManager(flowStoreServiceConnector);
        Flow flow = flowManager.getFlow(jobSpecification);
        assertThat("Flow ID", flow.getId(), is(existingFlow.getId()));
        assertThat("Flow version", flow.getVersion(), is(existingFlow.getVersion()));
        assertThat("foundFlowByName flag", flowManager.foundFlowByName(), is(false));
    }

    @Test
    void getFlow_fromJobSpecification_notFound() throws FlowStoreServiceConnectorException {
        JobSpecification jobSpecification = new JobSpecification()
                .withPackaging("pkg")
                .withFormat("fmt")
                .withCharset("chrset")
                .withDestination("dest")
                .withSubmitterId(123456);
        when(flowStoreServiceConnector.getFlowBinder(jobSpecification.getPackaging(), jobSpecification.getFormat(),
                jobSpecification.getCharset(), jobSpecification.getSubmitterId(), jobSpecification.getDestination()))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("not found", 404));

        FlowManager flowManager = new FlowManager(flowStoreServiceConnector);
        Flow flow = flowManager.getFlow(jobSpecification);
        assertThat("flow", flow, is(nullValue()));
        assertThat("foundFlowByName flag", flowManager.foundFlowByName(), is(false));
    }
}