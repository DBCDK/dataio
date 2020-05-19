/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.flowstore;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowStoreError;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.httpclient.HttpClient;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration tests for the flow binders collection part of the flow store service
 */
public class FlowBindersIT extends AbstractFlowStoreServiceContainerTest {
    /**
     * Given: a deployed flow-store service with a flow, a sink and a submitter
     * When: valid JSON is POSTed to the flow binders path referencing the flow, sink and submitter
     * Then : a flow binder is created and returned
     * And  : The flow binder created has an id, a version and contains the same information as the flow binder content given as input
     * And  : assert that only one flow binder can be found in the underlying database
     * And  : assert that the same flow binder can be located through search keys
     */
    @Test
    public void createFlowBinder_ok() throws Exception {
        final String ns = "FlowBindersIT.createFlowBinder_ok";
        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

        // When...
        final FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        // Then...
        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(flowBinderContent);

        // And ...
        assertThat(flowBinder.getContent(), is(flowBinderContent));

        // And ...
        assertSearchKeysExist(flowBinder, submitter.getContent().getNumber());
    }

    /**
     * Given: a deployed flow-store service
     * When: JSON posted to the flow binders path causes JSONBException
     * Then: request returns with a BAD REQUEST http status code
     */
    @Test
    public void createFlowBinder_invalidJson_BadRequest() {
        // When...
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                "<invalid json />", flowStoreServiceBaseUrl, FlowStoreServiceConstants.FLOW_BINDERS);

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource and a flow, a sink and a submitter
     * When : adding flow binder with the same name
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void createFlowBinder_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.createFlowBinder_duplicateName_NotAcceptable";

        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

        final FlowBinderContent validFlowBinderContent = new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        final FlowBinderContent duplicateFlowBinderContent = new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(validFlowBinderContent);
        try {
            // When...
            flowStoreServiceConnector.createFlowBinder(duplicateFlowBinderContent);
            fail("Primary key violation was not detected as input to createFlowBinder()");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : adding flow binder which references non-existing submitter
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void createFlowBinder_referencedSubmitterNotFound_PreconditionFailed() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.createFlowBinder_referencedSubmitterNotFound_preconditionFailed";

        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());

        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(new Date().getTime()))
                .build();
        try {
            // When...
            flowStoreServiceConnector.createFlowBinder(flowBinderContent);
            fail("Failed pre-condition was not detected as input to createFlowBinder()");
        // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(412));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : adding flow binder which references non-existing flow
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void createFlowBinder_referencedFlowNotFound_PreconditionFailed() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.createFlowBinder_referencedFlowNotFound_PreconditionFailed";

        // Given...
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns)
                .setFlowId(new Date().getTime())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();
        try {
            // When...
            flowStoreServiceConnector.createFlowBinder(flowBinderContent);
            fail("Failed pre-condition was not detected as input to createFlowBinder()");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(412));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : adding flow binder which references non-existing sink
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void createFlowBinder_referencedSinkNotFound_PreconditionFailed() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.createFlowBinder_referencedSinkNotFound_PreconditionFailed";

        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

        FlowBinderContent flowBinderContent = new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(new Date().getTime())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();
        try {
            // When...
            flowStoreServiceConnector.createFlowBinder(flowBinderContent);
            fail("Failed pre-condition was not detected as input to createFlowBinder()");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(412));
        }
    }

    /**
     * Given: a deployed flow-store service containing flow binder resource
     * When: adding flow binder with different name but matching search key
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     * And  : assert that only the same flow binder can be located via search keys
     */
    @Test
    public void createFlowBinder_searchKeyExists_NotAcceptable() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.createFlowBinder_searchKeyExists_NotAcceptable";

        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());
        final Submitter submitterOther = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns + "_other")
                .setNumber(submitter.getContent().getNumber() + 1)
                .build());

        FlowBinderContent validFlowBinderContent = new FlowBinderContentBuilder()
                .setName(ns + "_1")
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Arrays.asList(submitter.getId(), submitterOther.getId()))
                .build();

        FlowBinderContent notAcceptableFlowBinderContent = new FlowBinderContentBuilder()
                .setName(ns + "_2")
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        FlowBinder flowBinder = flowStoreServiceConnector.createFlowBinder(validFlowBinderContent);
        try {
            // When...
            flowStoreServiceConnector.createFlowBinder(notAcceptableFlowBinderContent);
            fail("Unique constraint violation was not detected as input to createFlowBinder()");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));

            // And...
            assertSearchKeysExist(flowBinder, submitter.getContent().getNumber());
       }
    }

    /**
     * Given: a deployed flow-store service containing three flow binders
     * When: GETing flow binders collection
     * Then: request returns with 3 flow binders
     * And: the flow binders are sorted alphabetically by name
     */
    @Test
    public void findAllFlowBinders_ok() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.findAllFlowBinders_ok";

        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

       final FlowBinder flowBinderSortsThird = flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
               .setName("c_" + ns)
               .setDestination(ns + ".1")
               .setFlowId(flow.getId())
               .setSinkId(sink.getId())
               .setSubmitterIds(Collections.singletonList(submitter.getId()))
               .build());

        final FlowBinder flowBinderSortsFirst = flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
                .setName("a_" + ns)
                .setDestination(ns + ".2")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build());

        final FlowBinder flowBinderSortsSecond = flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
                .setName("b_" + ns)
                .setDestination(ns + ".3")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build());

        // When...
        List<FlowBinder> listOfFlowBinders = flowStoreServiceConnector.findAllFlowBinders();

        // Then...
        assertThat(listOfFlowBinders.size() >= 3, is(true));

        // And...
        assertThat(listOfFlowBinders.get(0).getContent().getName(),
                is(flowBinderSortsFirst.getContent().getName()));
        assertThat(listOfFlowBinders.get(1).getContent().getName(),
                is(flowBinderSortsSecond.getContent().getName()));
        assertThat(listOfFlowBinders.get(2).getContent().getName(),
                is(flowBinderSortsThird.getContent().getName()));
    }

    @Test
    public void queryFlowBinders() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.queryFlowBinders";

        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

        final FlowBinder flowBinderFirst = flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
                .setName("first_" + ns)
                .setDestination(ns + ".1")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build());

        final FlowBinder flowBinderSecond = flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
                .setName("second_" + ns)
                .setDestination(ns + ".2")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build());

        final FlowBinder flowBinderThird = flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
                .setName("third_" + ns)
                .setDestination(ns + ".3")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build());

        // When...
        final List<FlowBinder> listOfFlowBinders = flowStoreServiceConnector.queryFlowBinders(
                "flow_binders:content @> '{\"destination\": \"" + flowBinderSecond.getContent().getDestination() + "\"}'");

        // Then...
        assertThat("number of hits", listOfFlowBinders.size(), is(1));

        // And...
        assertThat(listOfFlowBinders.get(0).getContent().getName(),
                is(flowBinderSecond.getContent().getName()));
    }

    /**
     * Given: a deployed flow-store service containing one flow binder
     * When: GETing an existing flow binder
     * Then: request returns with 1 flow binder (the correct one)
     */
    @Test
    public void getFlowBinder_ok() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.getFlowBinder_ok";

        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

       final FlowBinder originalFlowBinder = flowStoreServiceConnector.createFlowBinder(new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build());

        // When...
        FlowBinder flowBinder = flowStoreServiceConnector.getFlowBinder(originalFlowBinder.getId());

        // Then...
        assertThat(flowBinder.getId(), is(originalFlowBinder.getId()));
        assertThat(flowBinder.getContent(), is(originalFlowBinder.getContent()));
    }

    /**
     * Given: a deployed flow-store service containing one flow binder
     * When: GETing a non existing flow binder
     * Then: an exception is thrown, and the status code is 404
     */
    @Test
    public void getFlowBinder_nonExistingId_NotFound() throws FlowStoreServiceConnectorException {
        // Given...
        try {
            // When...
            flowStoreServiceConnector.getFlowBinder(new Date().getTime());
            fail("It seems as if we do get a FlowBinder, though we didn't expect one!");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid flow binder with given id is already stored
     * When : valid JSON is POSTed to the flow binders path with an identifier (update)
     * Then : assert the correct fields have been set with the correct values
     * And  : assert that the id of the flow binder has not changed
     * And  : assert that the version number has been updated
     */
    @Test
    public void updateFlowBinder_ok() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.updateFlowBinder_ok";

        // Given ...
        final FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        final Submitter submitter = flowStoreServiceConnector.getSubmitter(
                flowBinder.getContent().getSubmitterIds().get(0));

        // Assert that new rows have been created in flow_binders_search_index and in flow_binders_submitters
        assertSearchKeysExist(flowBinder, submitter.getContent().getNumber());

        final Submitter submitterForUpdateA = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns + ".A")
                .setNumber(new Date().getTime())
                .build());
        final Submitter submitterForUpdateB = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns + ".B")
                .setNumber(new Date().getTime())
                .build());
        List<Long> submitterIds = new ArrayList<>(2);
        submitterIds.add(submitterForUpdateA.getId());
        submitterIds.add(submitterForUpdateB.getId());

        final FlowBinderContent updatedFlowBinderContent = new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns + ".updated")
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(submitterIds)
                .build();

        // When...
        FlowBinder updatedFlowBinder = flowStoreServiceConnector.updateFlowBinder(
                updatedFlowBinderContent, flowBinder.getId(), flowBinder.getVersion());

        // Then...
        assertThat(updatedFlowBinder.getContent(), is(updatedFlowBinderContent));
        assertFlowBinderEquals(updatedFlowBinder, flowBinder, 1);

        // And...
        // Assert that the rows created for the "old" flow binder has been removed from the database
        assertSearchKeysDoNotExist(flowBinder.getContent(), submitter.getContent().getNumber());

        // Assert that new rows have been created for the updated flow binder
        assertSearchKeysExist(updatedFlowBinder, submitterForUpdateA.getContent().getNumber());
        assertSearchKeysExist(updatedFlowBinder, submitterForUpdateB.getContent().getNumber());
    }

    /**
     * Given: a deployed flow-store service
     * When : JSON posted to the flow binders path with update causes JSONBException
     * Then : request returns with a BAD REQUEST http status code
     */
    @Test
    public void updateFlowBinder_invalidJson_BadRequest() throws FlowStoreServiceConnectorException{
        final String ns = "FlowBindersIT.updateFlowBinder_invalidJson_BadRequest";

        // Given ...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);

        final Map<String, String> headers = new HashMap<>(1);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(flowBinder.getVersion()));
        final Response response = HttpClient.doPostWithJson(flowStoreServiceConnector.getClient(),
                headers, "<invalid json />", flowStoreServiceBaseUrl,
                FlowStoreServiceConstants.FLOW_BINDERS, Long.toString(flowBinder.getId()), "content");

        // Then...
        assertThat(response.getStatusInfo().getStatusCode(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) and wrong id number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     * And  : assert that no flow binder exists in the underlying database
     */
    @Test
    public void updateFlowBinder_wrongIdNumber_NotFound() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.updateFlowBinder_wrongIdNumber_NotFound";

        // Given...
        try {
            final FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
            final Submitter submitter = flowStoreServiceConnector
                    .getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

            // When...
            final FlowBinderContent content = new FlowBinderContentBuilder()
                    .setSubmitterIds(Collections.singletonList(submitter.getId()))
                    .build();
            flowStoreServiceConnector.updateFlowBinder(content, new Date().getTime(), 1L);

            fail("Wrong flow binder Id was not detected as input to updateFlowBinder()");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : Two valid flow binders are already stored
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) but with a name that is already
     *        in use by one of the existing flow binders
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_ACCEPTABLE http status code
     */
    @Test
    public void updateFlowBinder_duplicateName_NotAcceptable() throws FlowStoreServiceConnectorException{
        final String ns = "FlowBindersIT.updateFlowBinder_duplicateName_NotAcceptable";

        FlowBinder flowBinder1 = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder1.getContent().getSubmitterIds().get(0));

        final FlowBinderContent flowBinderContent2 = new FlowBinderContentBuilder()
                .setName(ns + ".2")
                .setDestination(ns + ".2")
                .setFlowId(flowBinder1.getContent().getFlowId())
                .setSinkId(flowBinder1.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        FlowBinder flowBinder2 = flowStoreServiceConnector.createFlowBinder(flowBinderContent2);

        final FlowBinderContent invalidFlowBinderContent = new FlowBinderContentBuilder()
                .setName(flowBinder1.getContent().getName())
                .setFlowId(flowBinder1.getContent().getFlowId())
                .setSinkId(flowBinder1.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();
        try {
            // When... (Attempting to save the second flow binder created with the
            // same name as the first flow binder created)
            flowStoreServiceConnector.updateFlowBinder(
                    invalidFlowBinderContent, flowBinder2.getId(), flowBinder2.getVersion());

            fail("Primary key violation was not detected as input to updateFlowBinder()");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

    /**
     * Given: a deployed flow-store service
     * And  : a valid flow binder with given id is already stored and the flow binder is opened for edit by two different users
     * And  : the first user updates the flow binder, valid JSON is POSTed to the flow binders path with an identifier (update)
     *        and correct version number
     * When : the second user attempts to update the original version of the flow binder, valid JSON is POSTed to the flow binders
     *        path with an identifier (update) and wrong version number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     */
    @Test
    public void updateFlowBinder_wrongVersion_Conflict() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.updateFlowBinder_wrongVersion_Conflict";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        final FlowBinderContent flowBinderContentFirstUser = new FlowBinderContentBuilder()
                .setName(ns + ".1")
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        final FlowBinderContent flowBinderContentSecondUser = new FlowBinderContentBuilder()
                .setName(ns + ".2")
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        // And... First user updates the flow binder
        flowStoreServiceConnector.updateFlowBinder(flowBinderContentFirstUser,
                flowBinder.getId(), flowBinder.getVersion());

        try {
            // When... Second user attempts to update the same flow binder
            flowStoreServiceConnector.updateFlowBinder(
                    flowBinderContentSecondUser, flowBinder.getId(), flowBinder.getVersion());

            fail("Edit conflict, in the case of multiple updates, was not detected as input to updateFlowBinder()");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(409));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) but the referenced
     *        flow could not be located in the underlying database
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void updateFlowBinder_referencedFlowNotFound_PreconditionFailed() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.updateFlowBinder_referencedFlowNotFound_PreconditionFailed";

        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        FlowBinderContent flowBinderContentForUpdate = new FlowBinderContentBuilder()
                .setName(ns + ".updated")
                .setDestination(ns + ".updated")
                .setFlowId(new Date().getTime())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        assertPreconditionFailed(flowBinderContentForUpdate, flowBinder,
                submitter.getContent().getNumber());
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) but the referenced
     *        sink could not be located in the underlying database
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void updateFlowBinder_referencedSinkNotFound_PreconditionFailed() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.updateFlowBinder_referencedSinkNotFound_PreconditionFailed";

        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));
        FlowBinderContent flowBinderContentForUpdate = new FlowBinderContentBuilder()
                .setName(ns + ".updated")
                .setDestination(ns + ".updated")
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(new Date().getTime())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        assertPreconditionFailed(flowBinderContentForUpdate, flowBinder,
                submitter.getContent().getNumber());
    }

    /**
     * Given: a deployed flow-store service
     * When : valid JSON is POSTed to the flow binders path with an identifier (update) but the referenced
     *        submitter could not be located in the underlying database
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a PRECONDITION FAILED http status code
     */
    @Test
    public void updateFlowBinder_referencedSubmitterNotFound_PreconditionFailed() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.updateFlowBinder_referencedSubmitterNotFound_PreconditionFailed";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));
        FlowBinderContent flowBinderContentForUpdate = new FlowBinderContentBuilder()
                .setName(ns + ".updated")
                .setDestination(ns + ".updated")
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(new Date().getTime()))
                .build();

        assertPreconditionFailed(flowBinderContentForUpdate, flowBinder,
                submitter.getContent().getNumber());
    }

    /**
     * Given: a deployed flow-store service containing multiple flow binder resources
     * When : updating a flow binder with different name but matching search key
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT ACCEPTABLE http status code
     */
    @Test
    public void updateFlowBinder_searchKeyExists_NotAcceptable() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.updateFlowBinder_searchKeyExists_NotAcceptable";

        // Given...
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

        final FlowBinderContent flowBinder1Content = new FlowBinderContentBuilder()
                .setName(ns + "_1")
                .setDestination(ns + "_1")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        final FlowBinderContent flowBinder2Content = new FlowBinderContentBuilder()
                .setName(ns + "_2")
                .setDestination(ns + "_2")
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        final FlowBinder flowBinder1 = flowStoreServiceConnector.createFlowBinder(flowBinder1Content);
        final FlowBinder flowBinder2 = flowStoreServiceConnector.createFlowBinder(flowBinder2Content);

        try {
            // When...
            final FlowBinderContent flowBinder1UpdatedContent = new FlowBinderContentBuilder()
                    .setName(flowBinder1.getContent().getName())
                    .setDestination(flowBinder2.getContent().getDestination())
                    .setFlowId(flow.getId())
                    .setSinkId(sink.getId())
                    .setSubmitterIds(Collections.singletonList(submitter.getId()))
                    .build();

            flowStoreServiceConnector.updateFlowBinder(
                    flowBinder1UpdatedContent, flowBinder1.getId(), flowBinder1.getVersion());
            fail("Unique constraint violation was not detected for updateFlowBinder()");
            // Then...
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(406));
        }
    }

    /**
     * Given: a deployed flow-store service and a flow binder is stored and belonging search-index and entry in flow_binders_submitters exists
     * When : attempting to delete the flow binder
     * Then : the flow binder is deleted
     */
    @Test
    public void deleteFlowBinder_ok() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.deleteFlowBinder_ok";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        // When...
        flowStoreServiceConnector.deleteFlowBinder(flowBinder.getId(), flowBinder.getVersion());

        // Then... Verify that the flow binder is deleted
        try {
            flowStoreServiceConnector.getFlowBinder(flowBinder.getId());
            fail("FlowBinder was not deleted");
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // We expect this exception from getFlowBinder(...) method when no flow binder exists!
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to delete a flow binder that does not exist
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void deleteFlowBinder_nonExistingId_NotFound() {
        final long nonExistingFlowBinderId = new Date().getTime();

        try {
            // When...
            flowStoreServiceConnector.deleteFlowBinder(nonExistingFlowBinderId, 1L);
            fail("None existing flow binder was not detected");

            // Then ...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(NOT_FOUND));
        }
    }

    /**
     * Given: a deployed flow-store service and a flow binder is stored.
     * And  : the flow binder is updated and, valid JSON is POSTed to the flow binders path with an identifier (update)
     *        and correct version number
     * When : attempting to delete the flow binder with the previous version number, valid JSON is POSTed to the flow binders
     *        path with an identifier (delete) and wrong version number
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a CONFLICT http status code
     */
    @Test
    public void deleteFlowBinder_optimisticLocking_Conflict() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.deleteFlowBinder_optimisticLocking_Conflict";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        // And...
        final long versionFirst = flowBinder.getVersion();

        final FlowBinderContent flowBinderContentFirstUser = new FlowBinderContentBuilder()
                .setName(ns + ".updated")
                .setDestination(ns + ".updated")
                .setFlowId(flowBinder.getContent().getFlowId())
                .setSinkId(flowBinder.getContent().getSinkId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        final FlowBinder updatedFlowBinder = flowStoreServiceConnector.updateFlowBinder(
                flowBinderContentFirstUser, flowBinder.getId(), versionFirst);

        try {
            // When...
            flowStoreServiceConnector.deleteFlowBinder(flowBinder.getId(), versionFirst);
            fail("FlowBinder was deleted");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException fssce) {
            // And...
            assertThat(Response.Status.fromStatusCode(fssce.getStatusCode()), is(CONFLICT));

            // And...
            assertSearchKeysDoNotExist(flowBinder.getContent(), submitter.getContent().getNumber());
            assertSearchKeysExist(updatedFlowBinder, submitter.getContent().getNumber());
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : search keys resolves to a flow binder
     * Then : assert that the flow binder found has an id, a version and contains the same information as the flow binder created
     */
    @Test
    public void resolveFlowBinder_ok() throws FlowStoreServiceConnectorException {
        final String ns = "FlowBindersIT.resolveFlowBinder_ok";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        // When...
        FlowBinder flowBinderToGet = flowStoreServiceConnector.getFlowBinder(
                flowBinder.getContent().getPackaging(),
                flowBinder.getContent().getFormat(),
                flowBinder.getContent().getCharset(),
                submitter.getContent().getNumber(),
                flowBinder.getContent().getDestination());

        // Then...
        assertThat(flowBinderToGet, is(flowBinder));
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to resolve a flow binder but search key packaging does not match
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void resolveFlowBinder_nonExistingPackaging_NotFound() throws FlowStoreServiceConnectorException{
        final String ns = "FlowBindersIT.resolveFlowBinder_nonExistingPackaging_NotFound";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        try {
            // When...
            flowStoreServiceConnector.getFlowBinder(
                    "noSuchPackaging",
                    flowBinder.getContent().getFormat(),
                    flowBinder.getContent().getCharset(),
                    submitter.getContent().getNumber(),
                    flowBinder.getContent().getDestination());
            fail("Invalid request to getFlowBinder() was not detected");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
            assertThat(e.getFlowStoreError(), not(nullValue()));
            assertThat(e.getFlowStoreError().getCode(),
                    is(FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to resolve a flow binder but format does not match
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void resolveFlowBinder_nonExistingFormat_NotFound() throws FlowStoreServiceConnectorException{
        final String ns = "FlowBindersIT.resolveFlowBinder_nonExistingFormat_NotFound";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        try {
            // When...
            flowStoreServiceConnector.getFlowBinder(
                    flowBinder.getContent().getPackaging(),
                    "nonExistingFormat",
                    flowBinder.getContent().getCharset(),
                    submitter.getContent().getNumber(),
                    flowBinder.getContent().getDestination());
            fail("Invalid request to getFlowBinder() was not detected");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
            assertThat(e.getFlowStoreError(), not(nullValue()));
            assertThat(e.getFlowStoreError().getCode(),
                    is(FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to resolve a flow binder but charset does not match
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void resolveFlowBinder_nonExistingCharset_NotFound() throws FlowStoreServiceConnectorException{
        final String ns = "FlowBindersIT.resolveFlowBinder_nonExistingCharset_NotFound";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        try {
            // When...
            flowStoreServiceConnector.getFlowBinder(
                    flowBinder.getContent().getPackaging(),
                    flowBinder.getContent().getFormat(),
                    "nonExistingCharset",
                    submitter.getContent().getNumber(),
                    flowBinder.getContent().getDestination());
            fail("Invalid request to getFlowBinder() was not detected");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
            assertThat(e.getFlowStoreError(), not(nullValue()));
            assertThat(e.getFlowStoreError().getCode(),
                    is(FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to resolve a flow binder but submitter number is unknown
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void resolveFlowBinder_nonExistingSubmitterNumber_NotFound() throws FlowStoreServiceConnectorException{
        final String ns = "FlowBindersIT.resolveFlowBinder_nonExistingSubmitterNumber_NotFound";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);

        try {
            // When...
            flowStoreServiceConnector.getFlowBinder(
                    flowBinder.getContent().getPackaging(),
                    flowBinder.getContent().getFormat(),
                    flowBinder.getContent().getCharset(),
                    new Date().getTime(),
                    flowBinder.getContent().getDestination());
            fail("Invalid request to getFlowBinder() was not detected");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
            assertThat(e.getFlowStoreError(), not(nullValue()));
            assertThat(e.getFlowStoreError().getCode(),
                    is(FlowStoreError.Code.NONEXISTING_SUBMITTER));
        }
    }

    /**
     * Given: a deployed flow-store service
     * When : attempting to resolve a flow binder but destination does not match
     * Then : assume that the exception thrown is of the type: FlowStoreServiceConnectorUnexpectedStatusCodeException
     * And  : request returns with a NOT_FOUND http status code
     */
    @Test
    public void resolveFlowBinder_nonExistingDestination_NotFound() throws FlowStoreServiceConnectorException{
        final String ns = "FlowBindersIT.resolveFlowBinder_nonExistingDestination_NotFound";

        // Given...
        FlowBinder flowBinder = createFlowBinderWithReferencedObjects(ns);
        Submitter submitter = flowStoreServiceConnector.getSubmitter(flowBinder.getContent().getSubmitterIds().get(0));

        try {
            // When...
            flowStoreServiceConnector.getFlowBinder(
                    flowBinder.getContent().getPackaging(),
                    flowBinder.getContent().getFormat(),
                    flowBinder.getContent().getCharset(),
                    submitter.getContent().getNumber(),
                    "nonExistingDestination");
            fail("Invalid request to getFlowBinder() was not detected");
            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(404));
            assertThat(e.getFlowStoreError(), not(nullValue()));
            assertThat(e.getFlowStoreError().getCode(),
                    is(FlowStoreError.Code.EXISTING_SUBMITTER_NONEXISTING_DESTINATION));
        }
    }

    /**
     * This method attempts to locate a non-existing flow binder via search keys
     * @param flowBinderContent holding search keys values
     * @param submitterNumber of the submitter referenced by the flow binder
     */
    private void assertSearchKeysDoNotExist(FlowBinderContent flowBinderContent, long submitterNumber) throws FlowStoreServiceConnectorException {
        try {
            flowStoreServiceConnector.getFlowBinder(
                    flowBinderContent.getPackaging(),
                    flowBinderContent.getFormat(),
                    flowBinderContent.getCharset(),
                    submitterNumber,
                    flowBinderContent.getDestination());

            fail("Search keys matched flow binder");
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e1) {
            assertThat(e1.getStatusCode(), is(404));
        }
    }

    /**
     * Retrieves a flow binder through search keys.
     * Asserts that the flow binder returned is the same as expected
     * @param flowBinder the existing flow binder
     * @param submitterNumber of the submitter referenced by the flow binder
     */
    private void assertSearchKeysExist(FlowBinder flowBinder, long submitterNumber) throws FlowStoreServiceConnectorException {
        FlowBinder flowBinderThroughSearchIndex =
                flowStoreServiceConnector.getFlowBinder(
                        flowBinder.getContent().getPackaging(),
                        flowBinder.getContent().getFormat(),
                        flowBinder.getContent().getCharset(),
                        submitterNumber,
                        flowBinder.getContent().getDestination());

        assertFlowBinderNotNull(flowBinderThroughSearchIndex);
        assertFlowBinderEquals(flowBinder, flowBinderThroughSearchIndex, 0);
    }

    /**
     * This method attempts to update an existing flow binder with content that does not reference either
     * a flow, a sink or a submitter
     */
    private void assertPreconditionFailed(FlowBinderContent flowBinderContentForUpdate, FlowBinder flowBinder, long submitterNumber)
            throws FlowStoreServiceConnectorException{
        try {
            flowStoreServiceConnector.updateFlowBinder(flowBinderContentForUpdate, flowBinder.getId(), flowBinder.getVersion());
            fail("None existing reference was not detected as input to updateFlowBinder()");

            // Then...
        } catch(FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            // And...
            assertThat(e.getStatusCode(), is(412));

            // And... assert that the tables: flow_binder_search_index and flow_binders_submitters have not been updated
            assertSearchKeysDoNotExist(flowBinderContentForUpdate, submitterNumber);
            assertSearchKeysExist(flowBinder, submitterNumber);
        }
    }

    /**
     * Creates a new flow binder with pre-defined values. The referenced sink, flow and submitter is also created
     * @return the created flow binder
     */
    private FlowBinder createFlowBinderWithReferencedObjects(String ns) throws FlowStoreServiceConnectorException{
        final Flow flow = flowStoreServiceConnector.createFlow(new FlowContentBuilder()
                .setName(ns)
                .build());
        final Sink sink = flowStoreServiceConnector.createSink(new SinkContentBuilder()
                .setName(ns)
                .build());
        final Submitter submitter = flowStoreServiceConnector.createSubmitter(new SubmitterContentBuilder()
                .setName(ns)
                .setNumber(new Date().getTime())
                .build());

        FlowBinderContent flowBinderContent =  new FlowBinderContentBuilder()
                .setName(ns)
                .setDestination(ns)
                .setFlowId(flow.getId())
                .setSinkId(sink.getId())
                .setSubmitterIds(Collections.singletonList(submitter.getId()))
                .build();

        return flowStoreServiceConnector.createFlowBinder(flowBinderContent);
    }

    private void assertFlowBinderNotNull(FlowBinder flowBinder) {
        assertThat(flowBinder, not(nullValue()));
        assertThat(flowBinder.getContent(), not(nullValue()));
        assertThat(flowBinder.getId(), not(nullValue()));
        assertThat(flowBinder.getVersion(), not(nullValue()));
    }

    private void assertFlowBinderEquals(FlowBinder flowBinderA, FlowBinder flowBinderB, int versionIncrement) {
        assertThat(flowBinderA.getId(), is(flowBinderB.getId()));
        assertThat(flowBinderA.getVersion(), is(flowBinderB.getVersion() + versionIncrement));
    }
}
