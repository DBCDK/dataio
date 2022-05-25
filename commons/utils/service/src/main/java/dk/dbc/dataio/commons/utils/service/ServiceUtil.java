package dk.dbc.dataio.commons.utils.service;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ServiceError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This utility class provides convenience methods for working with Servlet/JAX-RS based web services
 */
public class ServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);
    private static final JSONBContext jsonbContext = new JSONBContext();

    private ServiceUtil() {
    }

    /**
     * Builds service method response
     *
     * @param status HTTP status code of response
     * @param entity entity to include in response
     * @param <T>    type parameter for entity type
     * @return response object
     */
    public static <T> Response buildResponse(Response.Status status, T entity) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Returns JSON string representation of dk.dbc.dataio.commons.types.ServiceError object
     * constructed from given exception
     *
     * @param ex exception to wrap
     * @return JSON string representation of ServiceError object
     */
    public static String asJsonError(Exception ex) {
        return asJsonError(ex, null);
    }

    /**
     * Returns JSON string representation of dk.dbc.dataio.commons.types.ServiceError object
     * constructed from given exception and message
     *
     * @param ex      exception to wrap
     * @param message describing the error
     * @return JSON string representation of ServiceError object
     */
    public static String asJsonError(Exception ex, String message) {
        String error = null;
        try {
            log.error("Generating error based on exception", ex);
            error = jsonbContext.marshall(new ServiceError().withMessage(message).withDetails(ex.getMessage()).withStacktrace(stackTraceToString(ex)));
        } catch (JSONBException e) {
            log.error("Caught exception trying to create JSON representation of error", e);
        }
        return error;
    }

    public static String stackTraceToString(Throwable t) {
        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Looks up a resource through System Environment or System Property
     * using the name passed as a parameter in the call to this method.
     *
     * @param resourceName The name of the resource
     * @return System Environment or System Property as String
     */
    public static String getStringValueFromSystemEnvironmentOrProperty(String resourceName) {
        String value = getStringValueFromSystemEnvironment(resourceName);
        if (value == null || value.isEmpty()) {
            value = getStringValueFromSystemProperty(resourceName);
        }
        return value;
    }

    /**
     * Looks up a resource through named system property.
     *
     * @param resourceName The name of the resource
     * @return System Property name as String
     */
    public static String getStringValueFromSystemProperty(String resourceName) {
        return System.getProperty(resourceName);
    }

    /**
     * Looks up a resource through named environment variable.
     *
     * @param resourceName The name of the resource
     * @return System Property name as String
     */
    public static String getStringValueFromSystemEnvironment(String resourceName) {
        return System.getenv(resourceName);
    }
}
