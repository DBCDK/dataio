package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.flowstore.util.ServiceUtil;
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
    private static final Logger log = LoggerFactory.getLogger(JsonExceptionMapper.class);

    @Override
    public Response toResponse(JsonException e) {
        log.error("Mapping JSON exception", e);
        return ServiceUtil.buildResponse(Response.Status.NOT_ACCEPTABLE, ServiceUtil.asJsonError(e));
    }
}
