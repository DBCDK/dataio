package dk.dbc.dataio.sink.batchexchange.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import dk.dbc.dataio.sink.batchexchange.ScheduledBatchFinalizerBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
    @PersistenceContext(unitName = "batchExchangePU")
    private EntityManager entityManager;
    @Inject
    ScheduledBatchFinalizerBean scheduledBatchFinalizerBean;

    @Override
    public Response getStatus() {
        healthCheckDatabase();
        if(scheduledBatchFinalizerBean.isDown()) {
            return Response.status(Response.Status.PAYMENT_REQUIRED).entity("Batch finalizer is down").build();
        }
        return Response.ok().build();
    }

    private void healthCheckDatabase() {
        final Query query = entityManager.createNativeQuery("SELECT 1");
        query.getSingleResult();
    }
}
