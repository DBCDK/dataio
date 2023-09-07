package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ReferencedEntityNotFoundExceptionMapper implements ExceptionMapper<ReferencedEntityNotFoundException> {
    private static final Logger log = LoggerFactory.getLogger(ReferencedEntityNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(ReferencedEntityNotFoundException e) {
        log.error("Mapping referenced entity not found exception", e);
        return ServiceUtil.buildResponse(Response.Status.PRECONDITION_FAILED, ServiceUtil.asJsonError(e));
    }
}
