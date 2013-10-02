package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
    private static final Logger log = LoggerFactory.getLogger(JobsBean.class);

    @EJB
    JobStoreBean jobStore;

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
     * @throws JsonException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not comply with model schema
     * @throws ReferencedEntityNotFoundException when unable to resolve attached flow
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createJob(@Context UriInfo uriInfo, String jobSpecData)
            throws NullPointerException, IllegalArgumentException, EJBException, JsonException, ReferencedEntityNotFoundException {
        log.trace("JobSpec: {}", jobSpecData);

        final JobSpecification jobSpec = JsonUtil.fromJson(jobSpecData, JobSpecification.class, MixIns.getMixIns());
        final Flow flow = lookupFlowInFlowStore(jobSpec.getFlowId());
        //jobStore.createJob()

        return Response.created(uriInfo.getAbsolutePath()).build();
    }

    private Flow lookupFlowInFlowStore(long flowId) throws EJBException, ReferencedEntityNotFoundException, JsonException {
        final Response response = HttpClient.doGet(HttpClient.newClient(), getFlowStoreServiceEndpoint(),
                FlowStoreServiceEntryPoint.FLOWS, Long.toString(flowId));

        String flowData = null;
        try {
            final int status = response.getStatus();
            switch (Response.Status.fromStatusCode(status)) {
                case OK:
                    flowData = extractFlowDataFromFlowStoreResponse(flowId, response);
                    break;
                case NOT_FOUND:
                    throwOnFlowNotFoundInFlowStore(flowId);
                    break;
                default:
                    throwOnUnexpectedResponseFromFlowStore(flowId, status, response);
                    break;
            }
        } finally {
            response.close();
        }
        log.trace("Found flow: {}", flowData);

        return JsonUtil.fromJson(flowData, Flow.class, MixIns.getMixIns());
    }

    private String extractFlowDataFromFlowStoreResponse(long flowId, Response response) {
        final String flowData = response.readEntity(String.class);
        log.trace("Resolved flow({}) to {}", flowId, flowData);
        return flowData;
    }

    private void throwOnFlowNotFoundInFlowStore(long flowId) throws ReferencedEntityNotFoundException {
        final String errorMessage = String.format("flow(%d) not found", flowId);
        log.error(errorMessage);
        throw new ReferencedEntityNotFoundException(errorMessage);
    }

    private void throwOnUnexpectedResponseFromFlowStore(long flowId, int status, Response response) throws EJBException {
        final String errorDetails = response.readEntity(String.class);
        final String errorMessage = String.format("Attempt to resolve flow(%d) returned with status code %d: %s",
                flowId, status, errorDetails);
        log.error(errorMessage);
        throw new EJBException(errorMessage);
    }

    private String getFlowStoreServiceEndpoint() throws EJBException {
        final String flowStoreServiceEndpoint;
        try {
            flowStoreServiceEndpoint = ServiceUtil.getFlowStoreServiceEndpoint();
            log.debug("flow-store service endpoint {}", flowStoreServiceEndpoint);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
        return flowStoreServiceEndpoint;
    }
}
