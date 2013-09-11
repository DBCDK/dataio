package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.flowstore.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {
    private static final Logger log = LoggerFactory.getLogger(PersistenceExceptionMapper.class);

    @Override
    public Response toResponse(PersistenceException e) {
        log.error("Mapping persistence exception", e);
        if (e.getMessage() != null) {
            final String message = e.getMessage().toLowerCase();
            if (message.contains("duplicate key value violates unique constraint")      // postgresql
                    || message.contains("unique index or primary key violation")) {     // h2
                return ServiceUtil.buildResponse(Response.Status.CONFLICT, ServiceUtil.asJsonError(e));
            }
        }
        return ServiceUtil.buildResponse(Response.Status.INTERNAL_SERVER_ERROR, ServiceUtil.asJsonError(e));
    }

}
