package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.types.rest.FlowBinderFlowQuery;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobState;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/JobStoreServiceConstants.JOBS' entry point
 */
@Stateless
@Path(JobStoreServiceConstants.JOBS)
public class JobsBean {
    public static final String REST_FLOWBINDER_QUERY_ENTRY_POINT = "/resolve"; // todo: move this to a better place - this entrypoint is also hardcodet in FlowBindersBean.

    private static final Logger LOGGER = LoggerFactory.getLogger(JobsBean.class);

    @EJB
    JobStoreBean jobStore;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

    /**
     * Creates new job based on POSTed job specification, persists it in
     * the underlying data store and notifies job processor of its existence
     *
     * @param uriInfo application and request URI information
     * @param jobSpecData job specification as JSON string
     *
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 412 PRECONDITION_FAILED if unable to resolve attached flow,
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws NullPointerException when given null-valued jobSpecData argument
     * @throws IllegalArgumentException when given empty-valued jobSpecData argument
     * @throws EJBException on internal server error
     * @throws JsonException when given non-json jobSpecData argument,
     * or if JSON object does not comply with model schema
     * @throws ReferencedEntityNotFoundException when unable to resolve attached flow
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response createJob(@Context UriInfo uriInfo, String jobSpecData)
            throws NullPointerException, IllegalArgumentException, EJBException, JsonException, ReferencedEntityNotFoundException {
        LOGGER.trace("JobSpec: {}", jobSpecData);
        final JobSpecification jobSpec = JsonUtil.fromJson(jobSpecData, JobSpecification.class, MixIns.getMixIns());
        final FlowBinder flowBinder = lookupFlowBinderInFlowStore(jobSpec);
        LOGGER.trace("flowBinder: {}", flowBinder.toString());
        final Flow flow = lookupFlowInFlowStore(flowBinder.getContent().getFlowId());
        final Sink sink = lookupSinkInFlowStore(flowBinder.getContent().getSinkId());
        final Job job;
        final String jobInfoJson;
        try {
            job = jobStore.createJob(jobSpec, flowBinder, flow, sink);
            jobInfoJson = JsonUtil.toJson(job.getJobInfo());
            final NewJob newJob = new NewJob(job.getId(), jobStore.getNumberOfChunksInJob(job.getId()), sink);
            jobProcessorMessageProducer.send(newJob);
        } catch (JobStoreException | JsonException e) {
            throw new EJBException(e);
        }

        return Response.created(uriInfo.getAbsolutePath()).entity(jobInfoJson).build();
    }

    /**
     * Retrieves chunk from the underlying data store
     *
     * @param jobId Id of job containing chunk
     * @param chunkId Id of chunk
     *
     * @return a HTTP 200 OK response with Chunk entity as JSON string,
     *         a HTTP 404 NOT_FOUND if unable to locate chunk,
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JobStoreException on error reading chunk from store, or if unable
     * to marshall retrieved chunk to JSON.
     */
    @GET
    @Path("{jobId}/chunks/{chunkId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getChunk(@PathParam("jobId") long jobId, @PathParam("chunkId") long chunkId) throws JobStoreException {
        LOGGER.info("Getting chunk {} for job {}", chunkId, jobId);
        final Chunk chunk = jobStore.getChunk(jobId, chunkId);
        if (chunk == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final String entity;
        try {
            entity = JsonUtil.toJson(chunk);
        } catch (JsonException e) {
            throw new JobStoreException(String.format("Error marshalling chunk %d for job %d", chunkId, jobId), e);
        }
        return Response.ok().entity(entity).build();
    }

    /**
     * Retrieves job state from the underlying data store
     *
     * @param jobId Id of job
     *
     * @return a HTTP 200 OK response with JobState entity as JSON string,
     *         a HTTP 404 NOT_FOUND if unable to locate job,
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JobStoreException on error reading JobState from store, or if unable
     * to marshall retrieved JobState to JSON.
     */
    @GET
    @Path("{jobId}/state")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getState(@PathParam("jobId") long jobId) throws JobStoreException {
        LOGGER.info("Getting state for job {}", jobId);
        final JobState jobState = jobStore.getJobState(jobId);
        if (jobState == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final String entity;
        try {
            entity = JsonUtil.toJson(jobState);
        } catch (JsonException e) {
            throw new JobStoreException(String.format("Error marshalling state for job %d", jobId), e);
        }
        return Response.ok().entity(entity).build();
    }

    /**
     * Retrieves processor result from the underlying data store
     *
     * @param jobId Id of job containing processor result
     * @param chunkId Id of chunk for which to retrieve processor result
     *
     * @return a HTTP 200 OK response with ChunkResult entity as JSON string,
     *         a HTTP 404 NOT_FOUND if unable to locate processor result,
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JobStoreException on error reading processor result from store, or if unable
     * to marshall retrieved processor result to JSON.
     */
    @GET
    @Path("{jobId}/processed/{chunkId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getProcessorResult(@PathParam("jobId") long jobId, @PathParam("chunkId") long chunkId) throws JobStoreException {
        LOGGER.info("Getting processor result for chunk {} in job {}", chunkId, jobId);
        final ChunkResult processorResult = jobStore.getProcessorResult(jobId, chunkId);
        if (processorResult == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final String entity;
        try {
            entity = JsonUtil.toJson(processorResult);
        } catch (JsonException e) {
            throw new JobStoreException(String.format("Error marshalling processor result %d for job %d", chunkId, jobId), e);
        }
        return Response.ok().entity(entity).build();
    }

    /**
     * Retrieves sink result from the underlying data store
     *
     * @param jobId Id of job containing sink result
     * @param chunkId Id of chunk for which to retrieve sink result
     *
     * @return a HTTP 200 OK response with SinkChunkResult entity as JSON string,
     *         a HTTP 404 NOT_FOUND if unable to locate sink result,
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JobStoreException on error reading sink result from store, or if unable
     * to marshall retrieved sink result to JSON.
     */
    @GET
    @Path("{jobId}/delivered/{chunkId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSinkResult(@PathParam("jobId") long jobId, @PathParam("chunkId") long chunkId) throws JobStoreException {
        LOGGER.info("Getting sink result for chunk {} in job {}", chunkId, jobId);
        final SinkChunkResult sinkResult = jobStore.getSinkResult(jobId, chunkId);
        if (sinkResult == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final String entity;
        try {
            entity = JsonUtil.toJson(sinkResult);
        } catch (JsonException e) {
            throw new JobStoreException(String.format("Error marshalling sink result %d for job %d", chunkId, jobId), e);
        }
        return Response.ok().entity(entity).build();
    }

    /**
     * Retrieves list of jobs from the underlying data store
     *
     * @return a HTTP 200 OK response with list of jobs as JSON,
     *         a HTTP 500 INTERNAL_SERVER_ERROR if unable to locate list of jobs,
     *
     * @throws JobStoreException on error reading list from store, or if unable
     * to marshall retrieved data to JSON.
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getJobs() throws JobStoreException {
        LOGGER.info("Getting Jobs list");
        final List<JobInfo> jobInfo = jobStore.getAllJobInfos();
        if (jobInfo == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        final String entity;
        try {
            entity = JsonUtil.toJson(jobInfo);
        } catch (JsonException e) {
            throw new JobStoreException(String.format("Error marshalling job list"), e);
        }
        return Response.ok().entity(entity).build();
    }


    // hardening: lookupFlowInFlowStore and lookupSinkInFlowStore is identical - replace them with a single generic function!

    private Flow lookupFlowInFlowStore(long flowId) throws EJBException, ReferencedEntityNotFoundException, JsonException {
        final Client client = HttpClient.newClient();
        String flowData = null;
        try {
            final Response response = HttpClient.doGet(client, getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.FLOWS, Long.toString(flowId));
            try {
                final int status = response.getStatus();
                switch (Response.Status.fromStatusCode(status)) {
                    case OK:
                        flowData = extractDataFromFlowStoreResponse(response);
                        break;
                    case NOT_FOUND:
                        throwOnDataNotFoundInFlowStore(flowId, "flow");
                        break;
                    default:
                        throwOnUnexpectedResponseFromFlowStore(flowId, status, response, "flow");
                        break;
                }
            } finally {
                response.close();
            }
            LOGGER.trace("Found flow: {}", flowData);
        } finally {
            HttpClient.closeClient(client);
        }
        return JsonUtil.fromJson(flowData, Flow.class, MixIns.getMixIns());
    }

    private Sink lookupSinkInFlowStore(long sinkId) throws EJBException, ReferencedEntityNotFoundException, JsonException {
        final Client client = HttpClient.newClient();
        String sinkData = null;
        try {
            final Response response = HttpClient.doGet(client, getFlowStoreServiceEndpoint(), FlowStoreServiceEntryPoint.SINKS, Long.toString(sinkId));
            try {
                final int status = response.getStatus();
                switch (Response.Status.fromStatusCode(status)) {
                    case OK:
                        sinkData = extractDataFromFlowStoreResponse(response);
                        break;
                    case NOT_FOUND:
                        throwOnDataNotFoundInFlowStore(sinkId, "sink");
                        break;
                    default:
                        throwOnUnexpectedResponseFromFlowStore(sinkId, status, response, "sink");
                        break;
                }
            } finally {
                response.close();
            }
            LOGGER.trace("Found sink: {}", sinkData);
        } finally {
            HttpClient.closeClient(client);
        }
        return JsonUtil.fromJson(sinkData, Sink.class, MixIns.getMixIns());
    }

    private FlowBinder lookupFlowBinderInFlowStore(JobSpecification jobSpec) throws EJBException, ReferencedEntityNotFoundException, JsonException {
        final Client client = HttpClient.newClient();
        final Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_PACKAGING, jobSpec.getPackaging());
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_FORMAT, jobSpec.getFormat());
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_CHARSET, jobSpec.getCharset());
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_SUBMITTER, jobSpec.getSubmitterId());
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_DESTINATION, jobSpec.getDestination());
        String flowBinderData = null;
        try {
            final Response response = HttpClient.doGet(client, queryParameters, getFlowStoreServiceEndpoint(),
                    FlowStoreServiceEntryPoint.FLOW_BINDERS, REST_FLOWBINDER_QUERY_ENTRY_POINT);
            try {
                final int status = response.getStatus();
                switch (Response.Status.fromStatusCode(status)) {
                    case OK:
                        flowBinderData = extractDataFromFlowStoreResponse(response);
                        break;
                    case NOT_FOUND:
                        throwOnDataNotFoundInFlowStore(queryParameters);
                        break;
                    default:
                        throwOnUnexpectedResponseFromFlowStore(queryParameters, status, response);
                        break;
                }
            } finally {
                response.close();
            }
            LOGGER.trace("Found flowbinder: {}", flowBinderData);
        } finally {
            HttpClient.closeClient(client);
        }
        return JsonUtil.fromJson(flowBinderData, FlowBinder.class, MixIns.getMixIns());
    }

    private String formatQueryParametersForLog(Map<String, Object> queryParameters) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry entry : queryParameters.entrySet()) {
            sb.append(" ").append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }

    private String extractDataFromFlowStoreResponse(Response response) {
        final String flowData = response.readEntity(String.class);
        return flowData;
    }

    private void throwOnDataNotFoundInFlowStore(Map<String, Object> queryParameters) throws ReferencedEntityNotFoundException {
        final String errorMessage = String.format("flow not found with parameters: %s", formatQueryParametersForLog(queryParameters));
        LOGGER.error(errorMessage);
        throw new ReferencedEntityNotFoundException(errorMessage);
    }

    private void throwOnDataNotFoundInFlowStore(long id, String entityType) throws ReferencedEntityNotFoundException {
        final String errorMessage = String.format("%s not found with id: %d", entityType, id);
        LOGGER.error(errorMessage);
        throw new ReferencedEntityNotFoundException(errorMessage);
    }

    private void throwOnUnexpectedResponseFromFlowStore(Map<String, Object> queryParameters, int status, Response response) throws EJBException {
        final String errorDetails = response.readEntity(String.class);
        final String errorMessage = String.format("Attempt to resolve flow with parameters [ %s ] returned with status code %d: %s",
                queryParameters, status, errorDetails);
        LOGGER.error(errorMessage);
        throw new EJBException(errorMessage);
    }

    private void throwOnUnexpectedResponseFromFlowStore(long id, int status, Response response, String entityType) throws EJBException {
        final String errorDetails = response.readEntity(String.class);
        final String errorMessage = String.format("Attempt to resolve %s with id: %d returned with status code %d: %s",
                entityType, id, status, errorDetails);
        LOGGER.error(errorMessage);
        throw new EJBException(errorMessage);
    }

    private String getFlowStoreServiceEndpoint() throws EJBException {
        final String flowStoreServiceEndpoint;
        try {
            flowStoreServiceEndpoint = ServiceUtil.getFlowStoreServiceEndpoint();
            LOGGER.debug("flow-store service endpoint {}", flowStoreServiceEndpoint);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
        return flowStoreServiceEndpoint;
    }

}
