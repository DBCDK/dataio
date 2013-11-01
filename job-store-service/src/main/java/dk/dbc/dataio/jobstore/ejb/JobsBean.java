package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.types.restparameters.FlowBinderFlowQuery;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/JobStoreServiceEntryPoint.JOBS' entry point
 */
@Stateless
@Path(JobStoreServiceEntryPoint.JOBS)
public class JobsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsBean.class);

    public static final String REST_FLOW_QUERY_ENTRY_POINT = "/flow"; // todo: move this to a better place - this entrypoint is also hardcodet in FlowBindersBean.

    @EJB
    JobHandlerBean jobHandler;

    /**
     * Creates new job based on POSTed job specification and persists it in
     * the underlying data store
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
        final Flow flow = lookupFlowInFlowStore(jobSpec);
        final JobInfo jobInfo;
        try {
            final Job job = jobHandler.createJob(jobSpec, flow);
            final String sinkFile = jobHandler.sendToSink(job);
            jobInfo = new JobInfo(job.getId(), job.getJobInfo().getJobSpecification(), job.getJobInfo().getJobCreationTime(),
                    job.getJobInfo().getJobState(), job.getJobInfo().getJobErrorCode(), job.getJobInfo().getJobStatusMessage(), job.getJobInfo().getJobRecordCount(), sinkFile);
        } catch (JobStoreException e) {
            throw new EJBException(e);
        }

        return Response.created(uriInfo.getAbsolutePath()).entity(JsonUtil.toJson(jobInfo)).build();
    }

    private Flow lookupFlowInFlowStore(JobSpecification jobSpec) throws EJBException, ReferencedEntityNotFoundException, JsonException {
        final Client client = HttpClient.newClient();
        final Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_PACKAGING, jobSpec.getPackaging());
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_FORMAT, jobSpec.getFormat());
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_CHARSET, jobSpec.getCharset());
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_SUBMITTER, jobSpec.getSubmitterId());
        queryParameters.put(FlowBinderFlowQuery.REST_PARAMETER_DESTINATION, jobSpec.getDestination());
        String flowData = null;
        try {
            final Response response = HttpClient.doGet(client, queryParameters, getFlowStoreServiceEndpoint(),
                    FlowStoreServiceEntryPoint.FLOW_BINDERS, REST_FLOW_QUERY_ENTRY_POINT);
            try {
                final int status = response.getStatus();
                switch (Response.Status.fromStatusCode(status)) {
                    case OK:
                        flowData = extractFlowDataFromFlowStoreResponse(response);
                        break;
                    case NOT_FOUND:
                        throwOnFlowNotFoundInFlowStore(queryParameters);
                        break;
                    default:
                        throwOnUnexpectedResponseFromFlowStore(queryParameters, status, response);
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

    private String formatQueryParametersForLog(Map<String, Object> queryParameters) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry entry : queryParameters.entrySet()) {
            sb.append(" ").append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }

    private String extractFlowDataFromFlowStoreResponse(Response response) {
        final String flowData = response.readEntity(String.class);
        return flowData;
    }

    private void throwOnFlowNotFoundInFlowStore(Map<String, Object> queryParameters) throws ReferencedEntityNotFoundException {
        final String errorMessage = String.format("flow not found with paramters: %s", formatQueryParametersForLog(queryParameters));
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
