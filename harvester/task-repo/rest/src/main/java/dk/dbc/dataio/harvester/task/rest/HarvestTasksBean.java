package dk.dbc.dataio.harvester.task.rest;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.rest.HarvesterServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvestRequest;
import dk.dbc.dataio.harvester.types.HarvestSelectorRequest;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

@Stateless
@Path("/")
public class HarvestTasksBean {
    private final JSONBContext jsonbContext = new JSONBContext();
    @EJB
    TaskRepo taskRepo;

    /**
     * Creates a new harvest task
     *
     * @param uriInfo   URI information
     * @param harvestId ID of harvest for which to create task
     * @param request   harvest request
     * @return a HTTP 201 CREATED response,
     * a HTTP 400 BAD REQUEST response on unknown request type or if request is invalid JSON,
     * a HTTP 500 INTERNAL SERVER ERROR response in case of general error.
     */
    @POST
    @Path(HarvesterServiceConstants.HARVEST_TASKS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createHarvestTask(@Context UriInfo uriInfo,
                                      @PathParam(HarvesterServiceConstants.HARVEST_ID_VARIABLE) long harvestId,
                                      String request) {
        try {
            HarvestTask task = toHarvestTask(parseRequest(request));
            task.setConfigId(harvestId);
            taskRepo.getEntityManager().persist(task);
            taskRepo.getEntityManager().flush();
            return Response.created(getResourceUri(uriInfo, task)).build();
        } catch (IllegalStateException | JSONBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ServiceUtil.asJsonError(e)).build();
        }
    }

    private HarvestRequest<?> parseRequest(String request) throws JSONBException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(request, "request");
        return jsonbContext.unmarshall(request, HarvestRequest.class);
    }

    private HarvestTask toHarvestTask(HarvestRequest<?> harvestRequest) throws IllegalStateException {
        HarvestTask task = new HarvestTask();
        if (harvestRequest instanceof HarvestRecordsRequest) {
            HarvestRecordsRequest request = (HarvestRecordsRequest) harvestRequest;
            task.setRecords(request.getRecords());
            task.setNumberOfRecords(request.getRecords().size());
            task.setBasedOnJob(request.getBasedOnJob());
        } else if (harvestRequest instanceof HarvestSelectorRequest) {
            task.setSelector(((HarvestSelectorRequest) harvestRequest).getSelector());
        } else {
            throw new IllegalStateException("Unknown type of harvest request: " + harvestRequest.getClass().getName());
        }
        return task;
    }

    private URI getResourceUri(UriInfo uriInfo, HarvestTask task) {
        return uriInfo.getAbsolutePathBuilder().path(String.valueOf(task.getId())).build();
    }
}
