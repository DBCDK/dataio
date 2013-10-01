package dk.dbc.dataio.jobstore.ejb;

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

@Stateless
@Path(JobStoreServiceEntryPoint.JOBS)
public class JobsBean {
    private static final Logger log = LoggerFactory.getLogger(JobsBean.class);

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createJob(@Context UriInfo uriInfo, String jobSpecData)
            throws EJBException, JsonException, ReferencedEntityNotFoundException {
        log.trace("JobSpec: {}", jobSpecData);

        final JobSpecification jobSpec = JsonUtil.fromJson(jobSpecData, JobSpecification.class, MixIns.getMixIns());
        final String flowAsJson = lookupFlowInFlowStore(jobSpec.getFlowId());

        return Response.created(uriInfo.getAbsolutePath()).build();
    }

    private String lookupFlowInFlowStore(long flowId) throws EJBException, ReferencedEntityNotFoundException {
        final Response response = HttpClient.doGet(HttpClient.newClient(), getFlowStoreServiceEndpoint(),
                FlowStoreServiceEntryPoint.FLOWS, Long.toString(flowId));

        String flowData = null;
        try {
            final int status = response.getStatus();
            switch (Response.Status.fromStatusCode(status)) {
                case OK:
                    flowData = extractFlowFromFlowStoreResponse(flowId, response);
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

        return flowData;
    }

    private String extractFlowFromFlowStoreResponse(long flowId, Response response) {
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
