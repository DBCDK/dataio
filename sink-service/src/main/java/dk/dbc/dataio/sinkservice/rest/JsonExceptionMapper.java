package dk.dbc.dataio.sinkservice.rest;

import dk.dbc.dataio.commons.utils.json.JsonException;
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
public class JsonExceptionMapper implements ExceptionMapper<JsonException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonExceptionMapper.class);

    @Override
    public Response toResponse(JsonException e) {
        LOGGER.error("Mapping JsonException", e);
        return ServiceUtil.buildResponse(Response.Status.BAD_REQUEST, ServiceUtil.asJsonError(e));
    }
}
