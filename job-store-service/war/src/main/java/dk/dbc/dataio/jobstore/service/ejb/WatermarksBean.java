package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.WatermarkEntity;
import dk.dbc.dataio.jobstore.types.Watermark;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
@LocalBean
@Path("/")
public class WatermarksBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(WatermarksBean.class);
    JSONBContext jsonbContext = new JSONBContext();

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @GET
    @Path(JobStoreServiceConstants.SINK_WATERMARK)
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response getWatermark(
            @PathParam(JobStoreServiceConstants.SINK_ID_VARIABLE) long sinkId,
            @QueryParam(JobStoreServiceConstants.RECORD_KEY_QUERY_PARAM) String recordKey) throws JSONBException {
        LOGGER.trace("getWatermark called with sinkId {} recordKey {}", sinkId, recordKey);
        final WatermarkEntity entity = entityManager.find(
                WatermarkEntity.class, new WatermarkEntity.Key(Math.toIntExact(sinkId), recordKey));
        final WatermarkResponse response = new WatermarkResponse(entity == null ? null : toWatermark(entity));
        return Response.status(Response.Status.OK)
                .entity(jsonbContext.marshall(response))
                .build();
    }

    private Watermark toWatermark(WatermarkEntity entity) {
        return new Watermark(entity.getJobId(), entity.getChunkId(), entity.getItemId());
    }

    private static class WatermarkResponse {
        private final Watermark watermark;

        private WatermarkResponse(Watermark watermark) {
            this.watermark = watermark;
        }

        public Watermark getWatermark() {
            return watermark;
        }
    }
}
