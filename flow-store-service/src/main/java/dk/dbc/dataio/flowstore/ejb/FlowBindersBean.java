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

package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.FlowStoreError;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.rest.FlowBinderFlowQuery;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import dk.dbc.dataio.flowstore.entity.FlowBinderSearchIndexEntry;
import dk.dbc.dataio.flowstore.entity.SinkEntity;
import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dk.dbc.dataio.flowstore.util.ServiceUtil.getResourceUriOfVersionedEntity;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource exposed
 * by the '/FlowStoreServiceConstants.FLOW_BINDERS' entry point
 */
@Stateless
@Path("/")
public class FlowBindersBean {

    private static final Logger log = LoggerFactory.getLogger(FlowBindersBean.class);
    private static final String FLOW_BINDER_CONTENT_DISPLAY_TEXT = "flowBinderContent";
    private static final String NULL_ENTITY = "";
    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Retrieves a flow binder from underlying data store through a named query retrieving data
     * from table: flow_binders_search_index
     *
     * @param packaging set for the flow binder
     * @param format set for the flow binder
     * @param charset set for the flow binder
     * @param submitterNumber to identify the referenced submitter
     * @param destination set for the flow binder
     *
     * @return
     * a HTTP 200 OK response flow binder content as JSON,
     * a HTTP 404 NOT_FOUND response if flow binder is not found
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JSONBException when given invalid (null-valued, empty-valued or
     * non-json) JSON string
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDER_RESOLVE)
    @Produces({MediaType.APPLICATION_JSON})
    @SuppressWarnings("unchecked")
    public Response getFlowBinder(@QueryParam(FlowBinderFlowQuery.REST_PARAMETER_PACKAGING) String packaging,
                                  @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_FORMAT) String format,
                                  @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_CHARSET) String charset,
                                  @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_SUBMITTER) Long submitterNumber,
                                  @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_DESTINATION) String destination) throws JSONBException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, FlowBinderFlowQuery.REST_PARAMETER_PACKAGING);
        InvariantUtil.checkNotNullNotEmptyOrThrow(format, FlowBinderFlowQuery.REST_PARAMETER_FORMAT);
        InvariantUtil.checkNotNullNotEmptyOrThrow(charset, FlowBinderFlowQuery.REST_PARAMETER_CHARSET);
        InvariantUtil.checkNotNullOrThrow(submitterNumber, FlowBinderFlowQuery.REST_PARAMETER_SUBMITTER);
        InvariantUtil.checkNotNullNotEmptyOrThrow(destination, FlowBinderFlowQuery.REST_PARAMETER_DESTINATION);

        final Query query = entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOWBINDER);

        query.setParameter(FlowBinder.DB_QUERY_PARAMETER_PACKAGING, packaging);
        query.setParameter(FlowBinder.DB_QUERY_PARAMETER_FORMAT, format);
        query.setParameter(FlowBinder.DB_QUERY_PARAMETER_CHARSET, charset);
        query.setParameter(FlowBinder.DB_QUERY_PARAMETER_SUBMITTER, submitterNumber);
        query.setParameter(FlowBinder.DB_QUERY_PARAMETER_DESTINATION, destination);

        List<FlowBinder> flowBinders = query.getResultList();

        if(flowBinders.size() > 1) {
            String msg = getMoreThanOneFlowFoundMessage(query);
            log.warn(msg);
        }

        if (flowBinders.isEmpty()) {

            // Search for flowBinders with the submitter number given as input
            final List<FlowBinderSearchIndexEntry> searchIndexesForSubmitter = entityManager
                    .createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_BY_SUBMITTER)
                    .setParameter(FlowBinder.DB_QUERY_PARAMETER_SUBMITTER, submitterNumber)
                    .getResultList();

            // Generate appropriate FlowStoreError depending on the flow binder search index entries returned
            FlowStoreError flowStoreError = getFlowStoreError(searchIndexesForSubmitter, packaging, format, charset, submitterNumber, destination);

            // Return NOT_FOUND response with the FlowStoreError as entity
            return Response.status(Response.Status.NOT_FOUND).entity(jsonbContext.marshall(flowStoreError)).build();
        }
        return Response.ok().entity(jsonbContext.marshall(flowBinders.get(0))).build();
    }

    /**
     * Creates new flow binder with data POST'ed as JSON and persists it in the
     * underlying data store.
     *
     * Note: this method updates multiple database tables assuming transactional
     * integrity
     *
     * @param uriInfo application and request URI information
     * @param flowBinderContent flow binder data as JSON string
     *
     * @return
     * a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     * a HTTP 400 BAD_REQUEST response on invalid json content,
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints,
     * a HTTP 412 PRECONDITION_FAILED if a referenced submitter or flow no longer exists,
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JSONBException when given invalid (null-valued, empty-valued or
     * non-json) JSON string, or if JSON object does not comply with model
     * schema
     * @throws ReferencedEntityNotFoundException when unable to resolve any
     * attached flow or submitters
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_BINDERS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createFlowBinder(@Context UriInfo uriInfo, String flowBinderContent) throws JSONBException, ReferencedEntityNotFoundException {
        log.trace("Called with: '{}'", flowBinderContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowBinderContent, FLOW_BINDER_CONTENT_DISPLAY_TEXT);
        /* ATTENTION:
         Below we rely on the transactional integrity provided by the underlying relational
         database system and Java EE, so that if the persisting of a search index entry fails
         the persisted flow binder will be automatically rolled back. This will have to be
         handled differently in case the underlying data store no longer supports transactions.
         */

        /* We set the JSON content for a new FlowBinder instance causing the IDs of referenced
         flow, sink and submitters to be made available. We then resolve these references into
         entities and attaches them to the flow binder causing foreign key relations to be
         created. Finally we generate the search index entries generated by this flow binder
         and persists them in the data store.
         */

        FlowBinder flowBinder = new FlowBinder();
        flowBinder.setContent(flowBinderContent);
        flowBinder.setFlow(resolveFlow(flowBinder.getFlowId()));
        flowBinder.setSinkEntity(resolveSink(flowBinder.getSinkId()));
        flowBinder.setSubmitters(resolveSubmitters(flowBinder.getSubmitterIds()));

        entityManager.persist(flowBinder);

        FlowBinder.generateSearchIndexEntries(flowBinder).forEach(entityManager::persist);

        entityManager.flush();

        final String flowBinderJson = jsonbContext.marshall(flowBinder);
        return Response
                .created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), flowBinder))
                .entity(flowBinderJson)
                .tag(flowBinder.getVersion().toString())
                .build();
    }

    /**
     * Updates an existing flow binder with data POST'ed as JSON and persists it in the
     * underlying data store.
     *
     * Note: this method updates multiple database tables assuming transactional
     * integrity
     *
     * @param flowBinderContent flow binder data as JSON string
     * @param id identifying the flow binder in the underlying data store
     * @param version the current version of the persisted flow binder
     *
     * @return
     * a HTTP 200 OK response flow binder content as JSON,
     * a HTTP 400 BAD_REQUEST response on invalid json content,
     * a HTTP 404 NOT_FOUND response if flow binder is not found
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints,
     * a HTTP 409 response in case of Concurrent Update error
     * a HTTP 412 PRECONDITION_FAILED on failure to locate one or more of the referenced objects
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JSONBException when given invalid (null-valued, empty-valued or
     * non-json) JSON string, or if JSON object does not comply with model
     * schema
     *
     * @throws ReferencedEntityNotFoundException if one or more of the referenced entities was not found
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_BINDER_CONTENT)
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateFlowBinder(String flowBinderContent,
                                     @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
                                     @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JSONBException, ReferencedEntityNotFoundException {

        log.trace("called with: '{}'", flowBinderContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowBinderContent, FLOW_BINDER_CONTENT_DISPLAY_TEXT);

        // Retrieve the existing flow binder
        final FlowBinder flowBinderEntity = entityManager.find(FlowBinder.class, id);
        if (flowBinderEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        // Delete the existing search indexes
        Response response = findAndDeleteSearchIndexesForFlowBinder(flowBinderEntity.getId());
        if (response != null) {
            return response;
        }
        // Update the flow binder
        updateFlowBinderEntity(flowBinderEntity, flowBinderContent, version);

        // Create new search indexes for the flow binder
        createSearchIndexesForFlowBinder(flowBinderEntity);

        // Retrieve the updated flow binder
        final FlowBinder updatedFlowBinderEntity = entityManager.find(FlowBinder.class, id);

        // Return the updated flow binder
        String updatedFlowBinderEntityJson = jsonbContext.marshall((updatedFlowBinderEntity));
        return Response
                .ok()
                .entity(updatedFlowBinderEntityJson)
                .tag(updatedFlowBinderEntity.getVersion().toString())
                .build();
    }

    /**
     * Deletes an existing flow binder
     *
     * @param flowBinderId The flow binder ID
     * @param version The version of the flow binder
     *
     * @return a HTTP 204 response with no content,
     *         a HTTP 404 response in case of flow binder ID not found,
     *         a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     *         a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.FLOW_BINDER)
    public Response deleteFlowBinder(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long flowBinderId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final FlowBinder flowBinderEntity = entityManager.find(FlowBinder.class, flowBinderId);

        if(flowBinderEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }

        // First we need to update the version no to see if any Optimistic Locking occurs!
        entityManager.detach(flowBinderEntity);
        flowBinderEntity.setVersion(version);
        FlowBinder versionUpdatedAndNoOptimisticLocking = entityManager.merge(flowBinderEntity);

        // If no Optimistic Locking - delete it!

        // Delete the existing search indexes
        Response response = findAndDeleteSearchIndexesForFlowBinder(flowBinderEntity.getId());
        if (response != null) {
            return response;
        }

        // Delete the flow binder entity
        entityManager.remove(versionUpdatedAndNoOptimisticLocking);
        entityManager.flush();

        return Response.noContent().build();
    }

    /**
     * Returns list of stored flow binders sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JSONBException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDERS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllFlowBinders() throws JSONBException {
        final TypedQuery<dk.dbc.dataio.commons.types.FlowBinder> query = entityManager.createNamedQuery(FlowBinder.QUERY_FIND_ALL, dk.dbc.dataio.commons.types.FlowBinder.class);
        return Response.ok().entity(jsonbContext.marshall(query.getResultList())).build();
    }

    /**
     * Retrieves flow binder from underlying data store
     *
     * @param id flow binder identifier
     *
     * @return a HTTP 200 response with flow binder as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDER)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFlowBinderById(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) throws JSONBException {
        final FlowBinder flowBinder = entityManager.find(FlowBinder.class, id);

        if (flowBinder == null) {
            return Response.status(NOT_FOUND).entity(NULL_ENTITY).build();
        }
        return Response.ok().entity(jsonbContext.marshall(flowBinder)).build();
    }


     // Private methods

    /**
     * This method locates and deletes all search indexes for a given flow binder
     * @param flowBinderId identifying which flowBinderSearchIndexEntries should be deleted
     * @return response, null if all went well
     */
    @SuppressWarnings("unchecked")
    private Response findAndDeleteSearchIndexesForFlowBinder(Long flowBinderId) {
        Response response = null;

        // Create named query
        Query query = entityManager.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER);
        try {
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_FLOWBINDER, flowBinderId);
        } catch (IllegalArgumentException e) {
            String errMsg = String.format("Error while setting parameters for database query: %s", e.getMessage());
            log.warn(errMsg, e);
            response = Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }

        if(query.getResultList().isEmpty()) {
            log.warn(getNoFlowBinderSearchIndexEntryFoundMessage(query));
        }
        // Extract the results
        List<FlowBinderSearchIndexEntry> existingSearchIndexEntries = query.getResultList();

        // Delete the existing search indexes
        existingSearchIndexEntries.forEach(entityManager::remove);
        return response;
    }

    /**
     * This method creates new search index entries for the flow binder given as input
     * @param flowBinderEntity, the flow binder to create new search indexes for
     *
     * @throws PersistenceException if a JsonException is thrown while generating the new SearchIndexEntries.
     * The conversion to an unchecked exception is to ensure that a transaction rollback is performed.
     */
    private void createSearchIndexesForFlowBinder (FlowBinder flowBinderEntity) throws PersistenceException {
        try {
            FlowBinder.generateSearchIndexEntries(flowBinderEntity).forEach(entityManager::persist);
        } catch (JSONBException e) {
            throw new PersistenceException("flow binder contains invalid JSON content", e.getCause());
        }
    }

    /**
     * Looks through the FlowBinderSearchIndexEntry and deciphers which FlowStoreError should be returned
     *
     * @param searchIndexesForSubmitter the result of the query
     * @param packaging packaging
     * @param format format
     * @param charset charset
     * @param submitterNumber submitter number
     * @param destination destination
     * @return flowStoreError containing the appropriate error message
     */
    private FlowStoreError getFlowStoreError(List<FlowBinderSearchIndexEntry> searchIndexesForSubmitter,
                                             String packaging,
                                             String format,
                                             String charset,
                                             Long submitterNumber,
                                             String destination) {

        if(searchIndexesForSubmitter.isEmpty()) {
            return getFlowStoreErrorForSubmitterNotFound(submitterNumber);
        } else {
            return searchIndexesForSubmitter.stream()
                    .map(FlowBinderSearchIndexEntry::getDestination)
                    .filter(destination::equals)
                    .findAny()
                    .isPresent() ? getFlowStoreErrorForExistingSubmitterWithExistingDestination(packaging, format, charset, submitterNumber, destination)
                    : getFlowStoreErrorForDestinationNotFound(destination, submitterNumber);
        }
    }

    /**
     * FlowStoreError in the case of: FlowBinder not found for submitter
     * @param submitterNumber the submitter number input parameter
     * @return flowStoreError containing the appropriate error message
     */
    private FlowStoreError getFlowStoreErrorForSubmitterNotFound(Long submitterNumber) {
        return new FlowStoreError(
                FlowStoreError.Code.NONEXISTING_SUBMITTER,
                String.format("Biblioteksnummer %s kan ikke findes", submitterNumber),
                "");
    }

    /**
     * FlowStoreError in the case of: FlowBinder found for submitter, but the flow binder does not contain the given destination
     * @param destination the destination input parameter
     * @return flowStoreError containing the appropriate error message
     */
    private FlowStoreError getFlowStoreErrorForDestinationNotFound(String destination, Long submitterNumber) {
        return new FlowStoreError(
                FlowStoreError.Code.EXISTING_SUBMITTER_NONEXISTING_DESTINATION,
                String.format("Baseparameteren %s kan ikke findes i kombination med biblioteksnummer %s", destination, submitterNumber),
                "");
    }

    /**
     * FlowStoreError in the case of: FlowBinder found for given submitter with destination, but one or more of the remaining values are incorrect
     * @param packaging the packaging input parameter
     * @param format the format input parameter
     * @param charset the charset input parameter
     * @return flowStoreError containing the appropriate error message
     */
    private FlowStoreError getFlowStoreErrorForExistingSubmitterWithExistingDestination(String packaging,
                                                                                        String format,
                                                                                        String charset,
                                                                                        Long submitterNumber,
                                                                                        String destination) {
        return new FlowStoreError(
                FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC,
                String.format("Én eller flere af de angivne værdier protokol(t): %s, format(o): %s, tegnsæt(c): %s," +
                              "kan ikke findes i kombination med biblioteksnummer %s og baseparameter %s",
                        packaging,
                        format,
                        charset,
                        submitterNumber,
                        destination),
                "");
    }

    /**
     * Updates the flow binder entity
     * @param flowBinderEntity the currently persisted flow binder entity
     * @param flowBinderContentString the new flow binder content as String
     * @param version the current version of the flow binder
     * @throws PersistenceException if the objects referenced by the flow binder, could not be resolved
     */
    private void updateFlowBinderEntity(FlowBinder flowBinderEntity, String flowBinderContentString, long version) throws JSONBException, ReferencedEntityNotFoundException {
            entityManager.detach(flowBinderEntity);
            flowBinderEntity.setContent(flowBinderContentString);
            flowBinderEntity.setVersion(version);
            flowBinderEntity.setFlow(resolveFlow(flowBinderEntity.getFlowId()));
            flowBinderEntity.setSinkEntity(resolveSink(flowBinderEntity.getSinkId()));
            flowBinderEntity.setSubmitters(resolveSubmitters(flowBinderEntity.getSubmitterIds()));
            entityManager.merge(flowBinderEntity);
            entityManager.flush();
    }

    /**
     * Resolves each submitter referenced in given set by looking up the
     * corresponding submitter entity in the data store
     *
     * @param submitterIds set of submitter identifiers
     * @return set of submitter entities
     * @throws ReferencedEntityNotFoundException if unable to find a referenced
     * submitter entity in the data store
     */
    private Set<Submitter> resolveSubmitters(Set<Long> submitterIds) throws ReferencedEntityNotFoundException {
        final Set<Submitter> submitters = new HashSet<>(submitterIds.size());
        for (Long submitterId : submitterIds) {
            log.trace("Looking up Submitter entity for ID {}", submitterId);
            final Submitter submitter = entityManager.find(Submitter.class, submitterId);
            if (submitter == null) {
                throw new ReferencedEntityNotFoundException(String.format("Submitter(%d)", submitterId));
            }
            submitters.add(submitter);
        }
        log.debug("Resolved {} submitters from '{}' field", submitters.size(), FlowBinder.SUBMITTER_IDS_FIELD);
        return submitters;
    }

    /**
     * Resolves flow referenced by given id by looking up the corresponding flow
     * entity in the data store
     *
     * @param flowId flow identifier
     * @return flow entity
     * @throws ReferencedEntityNotFoundException if unable to find the
     * referenced flow entity in the data store
     */
    private Flow resolveFlow(Long flowId) throws ReferencedEntityNotFoundException {
        log.trace("Looking up Flow entity for ID {}", flowId);
        final Flow flow = entityManager.find(Flow.class, flowId);
        if (flow == null) {
            throw new ReferencedEntityNotFoundException(String.format("Flow(%d)", flowId));
        }
        return flow;
    }

    /**
     * Resolves sink referenced by given id by looking up the corresponding sink
     * entity in the data store
     *
     * @param sinkId sink identifier
     * @return sink entity
     * @throws ReferencedEntityNotFoundException if unable to find the
     * referenced sink entity in the data store
     */
    private SinkEntity resolveSink(Long sinkId) throws ReferencedEntityNotFoundException {
        log.trace("Looking up Sink entity for ID {}", sinkId);
        final SinkEntity sinkEntity = entityManager.find(SinkEntity.class, sinkId);
        if (sinkEntity == null) {
            throw new ReferencedEntityNotFoundException(String.format("Sink(%d)", sinkId));
        }
        return sinkEntity;
    }

    private String getMoreThanOneFlowFoundMessage(Query query) {
        return getQueryParametersStringify("More than one result was found for the query with parameters", query);
    }

    private String getNoFlowBinderSearchIndexEntryFoundMessage(Query query) {
        return getQueryParametersStringify("No flowBinderSearchIndexEntry found for query with parameters", query);
    }

    private String getQueryParametersStringify(String message, Query query) {
        return String.format("%s: '%s'='%s' '%s'='%s' '%s'='%s' '%s'='%s' '%s'='%s'",
                message,
                FlowBinder.DB_QUERY_PARAMETER_PACKAGING, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_PACKAGING),
                FlowBinder.DB_QUERY_PARAMETER_FORMAT, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_FORMAT),
                FlowBinder.DB_QUERY_PARAMETER_CHARSET, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_CHARSET),
                FlowBinder.DB_QUERY_PARAMETER_SUBMITTER, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_SUBMITTER),
                FlowBinder.DB_QUERY_PARAMETER_DESTINATION, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_DESTINATION));
    }

}
