package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
