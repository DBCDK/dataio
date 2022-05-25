package dk.dbc.dataio.harvester.ticklerepo.rest;

import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;

@Stateless
@Path("/")
public class TickleBean {
    @EJB
    TickleRepo tickleRepo;

    @GET
    @Path("dataset/{id:\\d+}/size-estimate")
    @Produces({MediaType.TEXT_PLAIN})
    public Response dataSetSizeEstimateById(@PathParam("id") String dataSetId) {
        final DataSet dataSet = tickleRepo.lookupDataSet(new DataSet()
                        .withId(Integer.parseInt(dataSetId)))
                .orElse(null);
        return Response.ok().entity(tickleRepo.estimateSizeOf(dataSet)).build();
    }

    @GET
    @Path("dataset/{id:.+}/size-estimate")
    @Produces({MediaType.TEXT_PLAIN})
    public Response dataSetSizeEstimateByName(@PathParam("id") String dataSetName) {
        final DataSet dataSet = tickleRepo.lookupDataSet(new DataSet()
                        .withName(dataSetName))
                .orElse(null);
        return Response.ok().entity(tickleRepo.estimateSizeOf(dataSet)).build();
    }

    /**
     * Change the status of records in the dataset to DELETED if their time
     * of last modification is before cut-off time POSTed as milliseconds
     * since epoch.
     *
     * @param dataSetId         ID of dataset for which to delete outdated records
     * @param cutOffEpochMillis threshold for outdated records as milliseconds
     *                          since epoch
     * @return a HTTP 200 OK response on success,
     * a HTTP 204 NO_CONTENT response on unknown dataset ID
     */
    @POST
    @Path("dataset/{id:\\d+}/time-of-last-modification-cut-off")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response deleteOutdatedRecords(@PathParam("id") Integer dataSetId, Long cutOffEpochMillis) {
        final DataSet dataSet = tickleRepo.lookupDataSet(new DataSet()
                        .withId(dataSetId))
                .orElse(null);
        return deleteOutdatedRecords(dataSet, cutOffEpochMillis);
    }

    /**
     * Change the status of records in the dataset to DELETED if their time
     * of last modification is before cut-off time POSTed as milliseconds
     * since epoch.
     *
     * @param dataSetName       name of dataset for which to delete outdated records
     * @param cutOffEpochMillis threshold for outdated records as milliseconds
     *                          since epoch
     * @return a HTTP 200 OK response on success,
     * a HTTP 204 NO_CONTENT response on unknown dataset name
     */
    @POST
    @Path("dataset/{id:.+}/time-of-last-modification-cut-off")
    @Consumes({MediaType.TEXT_PLAIN})
    public Response deleteOutdatedRecordsByDataSetName(
            @PathParam("id") String dataSetName, Long cutOffEpochMillis) {
        final DataSet dataSet = tickleRepo.lookupDataSet(new DataSet()
                        .withName(dataSetName))
                .orElse(null);
        return deleteOutdatedRecords(dataSet, cutOffEpochMillis);
    }

    private Response deleteOutdatedRecords(DataSet dataSet, Long cutOffEpochMillis) {
        if (dataSet == null) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        final Instant cutOff = Instant.ofEpochMilli(cutOffEpochMillis);
        final Batch batch = tickleRepo.createBatch(new Batch()
                .withDataset(dataSet.getId())
                .withType(Batch.Type.INCREMENTAL)
                .withBatchKey(0));
        tickleRepo.deleteOutdatedRecordsInBatch(batch, cutOff);
        tickleRepo.closeBatch(batch);
        return Response.ok().build();
    }
}
