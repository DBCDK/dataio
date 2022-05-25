package dk.dbc.dataio.sink.ticklerepo.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
    @PersistenceContext(unitName = "tickleRepoPU")
    private EntityManager entityManager;

    @Override
    public Response getStatus() {
        healthCheckDatabase();
        return Response.ok().build();
    }

    private void healthCheckDatabase() {
        final Query query = entityManager.createNativeQuery("SELECT 1");
        query.getSingleResult();
    }
}
