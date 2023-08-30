package dk.dbc.dataio.flowstore.rest;

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
public class ClassNotFoundExceptionMapper implements ExceptionMapper<ClassNotFoundException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(ClassNotFoundException e) {
        LOGGER.error("Mapping ClassNotFound exception", e);
        return ServiceUtil.buildResponse(Response.Status.BAD_REQUEST, ServiceUtil.asJsonError(e));
    }
}
