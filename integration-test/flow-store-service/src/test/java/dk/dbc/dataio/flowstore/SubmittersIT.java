package dk.dbc.dataio.flowstore;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.httpclient.HttpClient;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Integration tests for the submitters collection part of the flow store service
 */
public class SubmittersIT extends AbstractFlowStoreServiceContainerTest {
    /*
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the submitters path without an identifier
     * Then : a submitter it created and returned
     * And  : assert that the submitter created has an id, a version and contains
     *        the same information as the submitterContent given as input
     */
    @Test
    public void createSubmitter_ok() throws FlowStoreServiceConnectorException {
        // When...
        final SubmitterContent content = new SubmitterContentBuilder()
                .setName("SubmittersIT.createSubmitter_ok")
                .setNumber(new Date().getTime())
                .build();

        // Then...
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(content);

        // And...
        assertNotNull(submitter);
        assertThat(submitter.getId() != 0L, is(true));
        assertThat(submitter.getVersion(), is(1L));
        assertThat(submitter.getContent(), is(content));
    }

    @Test
    public void deleteSubmitter_ok() throws FlowStoreServiceConnectorException {
        final SubmitterContent content = new SubmitterContentBuilder()
                .setName("SubmittersIT.deleteSubmitter_ok")
                .setNumber(new Date().getTime())
                .build();

        final Submitter submitter = flowStoreServiceConnector.createSubmitter(content);
        long id = submitter.getId();
        long version = submitter.getVersion();

        flowStoreServiceConnector.deleteSubmitter(id, version);

        // Verify that the submitter is deleted
        try {
            flowStoreServiceConnector.getSubmitter(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // We expect this exception from getSubmitter(...) method when no submitter exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    @Test
    public void deleteSubmitter_noSubmitterToDelete() {
        final long nonExistingSubmitterId = new Date().getTime();
        try {
            flowStoreServiceConnector.deleteSubmitter(nonExistingSubmitterId, 1);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // We expect this exception from getSubmitter(...) method when no submitter exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    @Test
    public void deleteSubmitter_optimisticLocking() throws FlowStoreServiceConnectorException {
        final SubmitterContent content = new SubmitterContentBuilder()
                .setName("SubmittersIT.deleteSubmitter_optimisticLocking")
                .setNumber(new Date().getTime())
                .build();

        final Submitter submitter = flowStoreServiceConnector.createSubmitter(content);
        long id = submitter.getId();
        long firstVersion = submitter.getVersion();
        long secondVersion = firstVersion + 1;

        // Update submitter to bump version no.
        final SubmitterContent updatedContent = new SubmitterContentBuilder()
                .setName("SubmittersIT.deleteSubmitter_optimisticLocking.updated")
                .setNumber(new Date().getTime())
                .build();
        final Submitter updatedSubmitter =
                flowStoreServiceConnector.updateSubmitter(updatedContent, id, firstVersion);
        assertThat(updatedSubmitter.getVersion(), is(secondVersion));

        // Verify before delete
        Submitter submitterBeforeDelete = flowStoreServiceConnector.getSubmitter(id);
        assertThat(submitterBeforeDelete.getId(), is(id));
        assertThat(submitterBeforeDelete.getVersion(), is(secondVersion));

        try {
            // Subject Under Test
            flowStoreServiceConnector.deleteSubmitter(id, firstVersion);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));
        }
    }

    @Test
    public void deleteSubmitter_flowBinderExists() throws FlowStoreServiceConnectorException {
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName("SubmittersIT.deleteSubmitter_flowBinderExists")
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName("SubmittersIT.deleteSubmitter_flowBinderExists")
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName("SubmittersIT.deleteSubmitter_flowBinderExists")
                .setNumber(new Date().getTime())
                .build());
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName("SubmittersIT.deleteSubmitter_flowBinderExists")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();
        flowStoreServiceConnector.createFlowBinder(flowBinderContent);

        try {
            flowStoreServiceConnector.deleteSubmitter(submitter.getId(), submitter.getVersion());
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));
        }
    }

    /*
     * Given: a deployed flow-store service
     * When: JSON posted to the submitters path causes JSONBException
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createSubmitter_invalidJson_BadRequest() {
        // When...
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                "<invalid json />", flowStoreServiceBaseUrl, FlowStoreServiceConstants.SUBMITTERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /*
     * Given: a deployed flow-store service containing submitter resource
     * When : adding submitter with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createSubmitter_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final SubmitterContent content1 = new SubmitterContentBuilder()
                .setName("SubmittersIT.createSubmitter_duplicateName_NotAcceptable")
                .setNumber(new Date().getTime())
                .build();
        final SubmitterContent content2 = new SubmitterContentBuilder()
                .setName("SubmittersIT.createSubmitter_duplicateName_NotAcceptable")
                .setNumber(new Date().getTime() + 100000)
                .build();

        try {
            flowStoreServiceConnector.createSubmitter(content1);
            // When...
            flowStoreServiceConnector.createSubmitter(content2);
            fail("Primary key violation was not detected as input to createSubmitter().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

    /*
     * Given: a deployed flow-store service containing submitter resource
     * When : adding submitter with the same number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createSubmitter_duplicateNumber_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final long number = new Date().getTime();
        final SubmitterContent content1 = new SubmitterContentBuilder()
                .setName("SubmittersIT.createSubmitter_duplicateNumber_NotAcceptable.A")
                .setNumber(number)
                .build();
        final SubmitterContent content2 = new SubmitterContentBuilder()
                .setName("SubmittersIT.createSubmitter_duplicateNumber_NotAcceptable.B")
                .setNumber(number)
                .build();

        try {
            flowStoreServiceConnector.createSubmitter(content1);
            // When...
            flowStoreServiceConnector.createSubmitter(content2);
            fail("Primary key violation was not detected as input to createSubmitter().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the submitters path with a valid identifier
     * Then : a submitter is found and returned
     * And  : assert that the submitter found has an id, a version and contains the same information as the submitter created
     */
    @Test
    public void getSubmitter_ok() throws Exception {
        // When...
        final SubmitterContent submitterContent = new SubmitterContentBuilder()
                .setName("SubmittersIT.getSubmitter_ok")
                .setNumber(new Date().getTime())
                .build();
        Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent);

        // Then...
        Submitter submitterToGet = flowStoreServiceConnector.getSubmitter(submitter.getId());

        // And...
        assertThat(submitterToGet.getContent(), is(submitter.getContent()));
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a submitter with an unknown submitter id
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getSubmitter_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        try {
            // When...
            flowStoreServiceConnector.getSubmitter(new Date().getTime());

            fail("Invalid request to getSubmitter() was not detected");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the submitters path with a valid identifier
     * Then : a submitter is found and returned
     * And  : assert that the submitter found has an id, a version and contains the same information as the submitter created
     */
    @Test
    public void getSubmitterBySubmitterNumber_ok() throws FlowStoreServiceConnectorException {
        // When...
        final SubmitterContent submitterContent = new SubmitterContentBuilder()
                .setName("SubmittersIT.getSubmitterBySubmitterNumber_ok")
                .setNumber(new Date().getTime())
                .build();
        Submitter submitter = flowStoreServiceConnector.createSubmitter(submitterContent);

        // Then...
        Submitter submitterToGet = flowStoreServiceConnector.getSubmitterBySubmitterNumber(
                submitter.getContent().getNumber());

        // And...
        assertThat(submitterToGet.getContent(), is(submitter.getContent()));
    }

    /**
     * Given: a deployed flow-store service
     * When : Attempting to retrieve a submitter with an unknown submitter number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void getSubmitterBySubmitterNumber_WrongSubmitterNumber_NotFound() throws FlowStoreServiceConnectorException {
        try {
            // When...
            flowStoreServiceConnector.getSubmitterBySubmitterNumber(new Date().getTime());

            fail("Invalid request to getSubmitterBySubmitterNumber() was not detected.");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    /*
     * Given: a deployed flow-store service
     * And  : a valid submitter with given id is already stored
     * When : valid JSON is POSTed to the submitters path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the submitter has not changed
     * And  : assert that the version number has been updated
     */
    @Test
    public void updateSubmitter_ok() throws Exception {
        // Given...
        final SubmitterContent content = new SubmitterContentBuilder()
                .setName("SubmittersIT.updateSubmitter_ok")
                .setNumber(new Date().getTime())
                .build();
        Submitter submitter = flowStoreServiceConnector.createSubmitter(content);

        // When...
        final SubmitterContent updatedContent = new SubmitterContentBuilder()
                .setName("SubmittersIT.updateSubmitter_ok.updated")
                .setNumber(content.getNumber())
                .build();
        Submitter updatedSubmitter = flowStoreServiceConnector.updateSubmitter(
                updatedContent, submitter.getId(), submitter.getVersion());

        // Then...
        assertThat(updatedSubmitter.getContent(), is(updatedContent));

        // And...
        assertThat(updatedSubmitter.getId(), is(submitter.getId()));

        // And...
        assertThat(updatedSubmitter.getVersion(), is(submitter.getVersion() + 1));
    }

    /*
     * Given: a deployed flow-store service with a submitter
     * When : JSON posted to the submitters path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateSubmitter_invalidJson_BadRequest() throws FlowStoreServiceConnectorException {
        // Given ...
        final SubmitterContent content = new SubmitterContentBuilder()
                .setName("SubmittersIT.updateSubmitter_invalidJson_BadRequest")
                .setNumber(new Date().getTime())
                .build();
        Submitter submitter = flowStoreServiceConnector.createSubmitter(content);

        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, "1");  // Set version=1
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                headers, "<invalid json />", flowStoreServiceBaseUrl,
                FlowStoreServiceConstants.SUBMITTERS, Long.toString(submitter.getId()), "content");
        // Then...
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /*
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the submitters path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void updateSubmitter_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        try {
            // When...
            final SubmitterContent content = new SubmitterContentBuilder()
                    .setName("SubmittersIT.updateSubmitter_wrongIdNumber_NotFound")
                    .setNumber(new Date().getTime())
                    .build();
            flowStoreServiceConnector.updateSubmitter(content, new Date().getTime(), 1L);

            fail("Wrong submitter Id was not detected as input to updateSubmitter().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    /*
     * Given: a deployed flow-store service
     * And  : Two valid submitters are already stored
     * When : valid JSON is POSTed to the submitters path with an identifier (update)
     *        but with a submitter name that is already in use by another existing submitter
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void updateSubmitter_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        // Given...
        final String FIRST_SUBMITTER_NAME = "SubmittersIT.updateSubmitter_duplicateName_NotAcceptable.A";
        final String SECOND_SUBMITTER_NAME = "SubmittersIT.updateSubmitter_duplicateName_NotAcceptable.B";

        try {
            // And...
            final SubmitterContent content1 = new SubmitterContentBuilder()
                    .setName(FIRST_SUBMITTER_NAME)
                    .setNumber(new Date().getTime())
                    .build();
            flowStoreServiceConnector.createSubmitter(content1);

            final SubmitterContent content2 = new SubmitterContentBuilder()
                    .setName(SECOND_SUBMITTER_NAME)
                    .setNumber(new Date().getTime())
                    .build();
            Submitter submitter = flowStoreServiceConnector.createSubmitter(content2);

            // When... (Attempting to save the second submitter created with the same name as the first submitter created)
            flowStoreServiceConnector.updateSubmitter(content1, submitter.getId(), submitter.getVersion());

            fail("Primary key violation was not detected as input to updateSubmitter().");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
        }
    }

    /*
     * Given: a deployed flow-store service
     * And  : a valid submitter with given id is already stored and the submitter is opened for edit by two different users
     * And  : the first user updates the submitter, valid JSON is POSTed to the submitters path with an identifier (update)
     *        and correct version number
     * When : the second user attempts to update the original version of the submitter, valid JSON is POSTed to the submitters
     *        path with an identifier (update) and wrong version number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     */
    @Test
    public void updateSubmitter_wrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        // Given...
        final String SUBMITTER_NAME_FROM_FIRST_USER = "SubmittersIT.updateSubmitter_wrongVersion_Conflict.A";
        final String SUBMITTER_NAME_FROM_SECOND_USER = "SubmittersIT.updateSubmitter_wrongVersion_Conflict.B";
        long version = -21;

        try {
            // And...
            Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                    .setName("SubmittersIT.updateSubmitter_wrongVersion_Conflict")
                    .build());
            version = submitter.getVersion(); // stored for use in catch-clause

            // And... First user updates the submitter
            SubmitterContent content1 = new SubmitterContentBuilder()
                    .setName(SUBMITTER_NAME_FROM_FIRST_USER)
                    .setNumber(new Date().getTime())
                    .build();
            flowStoreServiceConnector.updateSubmitter(content1, submitter.getId(), submitter.getVersion());

            // When... Second user attempts to update the same submitter
            SubmitterContent content2 = new SubmitterContentBuilder()
                    .setName(SUBMITTER_NAME_FROM_SECOND_USER)
                    .setNumber(new Date().getTime())
                    .setNumber(99L)
                    .build();
            flowStoreServiceConnector.updateSubmitter(content2, submitter.getId(), submitter.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateSubmitter().");

            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(Response.Status.CONFLICT.getStatusCode()));
        }
    }

    /*
     * Given: a deployed flow-store service containing three submitters
     * When: GETing submitters collection
     * Then: request returns with 3 submitters
     * And: the submitters are sorted alphabetically by number
     */
    @Test
    public void findAllSubmitters_ok() throws FlowStoreServiceConnectorException {
        // Given...
        final SubmitterContent contentA = new SubmitterContentBuilder()
                .setName("SubmittersIT.findAllSubmitters_ok.1")
                .setNumber(1L)
                .setDescription("submitterA")
                .build();
        final SubmitterContent contentB = new SubmitterContentBuilder()
                .setName("SubmittersIT.findAllSubmitters_ok.2")
                .setNumber(2L)
                .setDescription("submitterB")
                .build();
        final SubmitterContent contentC = new SubmitterContentBuilder()
                .setName("SubmittersIT.findAllSubmitters_ok.3")
                .setNumber(3L)
                .setDescription("submitterC")
                .build();

        Submitter submitterSortsFirst = flowStoreServiceConnector.createSubmitter(contentA);
        Submitter submitterSortsSecond = flowStoreServiceConnector.createSubmitter(contentB);
        Submitter submitterSortsThird = flowStoreServiceConnector.createSubmitter(contentC);

        // When...
        List<Submitter> listOfSubmitters = flowStoreServiceConnector.findAllSubmitters();

        // Then...
        assertNotNull(listOfSubmitters);
        assertThat(listOfSubmitters.size() >= 3, is(true));

        // And...
        assertThat(listOfSubmitters.get(0).getContent().getName(),
                is(submitterSortsFirst.getContent().getName()));
        assertThat(listOfSubmitters.get(1).getContent().getName(),
                is(submitterSortsSecond.getContent().getName()));
        assertThat(listOfSubmitters.get(2).getContent().getName(),
                is(submitterSortsThird.getContent().getName()));
    }

    @Test
    public void querySubmitters() throws FlowStoreServiceConnectorException {
        // Given...
        final SubmitterContent contentA = new SubmitterContentBuilder()
                .setName("SubmittersIT.querySubmitters.A")
                .setNumber(65L)
                .setDescription("submitter A")
                .build();
        final SubmitterContent contentB = new SubmitterContentBuilder()
                .setName("SubmittersIT.querySubmitters.B")
                .setNumber(66L)
                .setDescription("submitter B")
                .build();
        final SubmitterContent contentC = new SubmitterContentBuilder()
                .setName("SubmittersIT.querySubmitters.C")
                .setNumber(67L)
                .setDescription("submitter C")
                .build();

        final Submitter submitterA = flowStoreServiceConnector.createSubmitter(contentA);
        final Submitter submitterB = flowStoreServiceConnector.createSubmitter(contentB);
        final Submitter submitterC = flowStoreServiceConnector.createSubmitter(contentC);

        // When...
        final List<Submitter> listOfSubmitters = flowStoreServiceConnector.querySubmitters(
                "submitters:content @> '{\"number\": " + submitterB.getContent().getNumber() + "}'");

        // Then...
        assertThat(listOfSubmitters.size(), is(1));

        // And...
        assertThat(listOfSubmitters.get(0).getContent().getName(),
                is(submitterB.getContent().getName()));
    }

    @Test
    public void getFlowBindersForSubmitter_emptyResult() throws FlowStoreServiceConnectorException {
        // When...
        final List<FlowBinderIdent> flowBinders =
                flowStoreServiceConnector.getFlowBindersForSubmitter(new Date().getTime());

        // Then...
        assertThat("result", flowBinders, is(notNullValue()));
        assertThat("size", flowBinders.size(), is(0));
    }

    /*
     * Given: a submitter attached to a number of flow-binders
     * When: resolving flow-binder for this submitter
     * Then: request returns all flow-binders to which the submitter is attached
     */
    @Test
    public void getFlowBindersForSubmitter() throws FlowStoreServiceConnectorException {
        // Given...
        final SubmitterContent content1 = new SubmitterContentBuilder()
                .setName("SubmittersIT.getFlowBindersForSubmitter.1")
                .setNumber(new Date().getTime())
                .build();
        final SubmitterContent content2 = new SubmitterContentBuilder()
                .setName("SubmittersIT.getFlowBindersForSubmitter.2")
                .setNumber(new Date().getTime() + 100000)
                .build();

        final Submitter submitterRequested = flowStoreServiceConnector.createSubmitter(content1);
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(content2);

        final FlowBinder flowBinder1 =
                createFlowBinderForSubmitter(submitterRequested,
                        "SubmittersIT.getFlowBindersForSubmitter.flowBinder1");
        final FlowBinder flowBinder2 =
                createFlowBinderForSubmitter(submitterRequested,
                        "SubmittersIT.getFlowBindersForSubmitter.flowBinder2");
        createFlowBinderForSubmitter(submitter,
                "SubmittersIT.getFlowBindersForSubmitter.flowBinder3");

        // When...
        final List<FlowBinderIdent> flowBinders =
                flowStoreServiceConnector.getFlowBindersForSubmitter(submitterRequested.getId());

        // Then...
        assertThat("result", flowBinders, is(notNullValue()));
        assertThat("size", flowBinders.size(), is(2));
        assertThat("1st flow-binder", flowBinders.get(0).getFlowBinderName(),
                is(flowBinder1.getContent().getName()));
        assertThat("2nd flow-binder", flowBinders.get(1).getFlowBinderName(),
                is(flowBinder2.getContent().getName()));
    }

    private FlowBinder createFlowBinderForSubmitter(Submitter submitter, String flowBinderName)
            throws FlowStoreServiceConnectorException {

        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(flowBinderName + "Flow")
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(flowBinderName + "Sink")
                .build());
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName(flowBinderName)
                .setFormat(flowBinderName + "Format")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        return flowStoreServiceConnector.createFlowBinder(flowBinderContent);
    }
}
