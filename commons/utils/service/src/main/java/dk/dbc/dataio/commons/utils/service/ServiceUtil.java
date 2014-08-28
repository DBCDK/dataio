package dk.dbc.dataio.commons.utils.service;

import dk.dbc.dataio.commons.types.ServiceError;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This utility class provides convenience methods for working with Servlet/JAX-RS based web services
 */
public class ServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);

    private static final String FLOW_STORE_SERVICE_ENDPOINT_RESOURCE = "dataioGuiFlowStoreServiceEndpoint";
    private static final String JOB_STORE_SERVICE_ENDPOINT_RESOURCE = "dataioJobStoreServiceEndpoint";
    private static final String SINK_SERVICE_ENDPOINT_RESOURCE = "dataioSinkServiceEndpoint";
    private static final String SUBVERSION_SCM_ENDPOINT_RESOURCE = "dataioGuiSubversionScmEndpoint";

    private ServiceUtil() { }

    /**
     * Looks up file-store filesystem url through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_JOBSTORE_FILESYSTEM}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_JOBSTORE_FILESYSTEM}'
     * system property.
     *
     * @return job-store filesystem URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getJobStoreFilesystemUrl() throws NamingException {
        return System.getProperty(JndiConstants.URL_RESOURCE_JOBSTORE_FILESYSTEM);
    }

    /**
     * Looks up file-store service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_FILESTORE_RS}'.
     * For testing purposes the JNDI lookup can be bypassed by defining a '{@value
     * dk.dbc.dataio.commons.types.jndi.JndiConstants#URL_RESOURCE_FILESTORE_RS}'
     * system property.
     *
     * @return file-store service URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getFileStoreServiceEndpoint() throws NamingException {
        String fileStoreServiceEndpoint = System.getProperty(JndiConstants.URL_RESOURCE_FILESTORE_RS);
        if (fileStoreServiceEndpoint == null || fileStoreServiceEndpoint.isEmpty()) {
            fileStoreServiceEndpoint = getStringValueFromResource(JndiConstants.URL_RESOURCE_FILESTORE_RS);
        }
        return fileStoreServiceEndpoint;
    }

    /**
     * Looks up flow-store service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value #FLOW_STORE_SERVICE_ENDPOINT_RESOURCE}'. For testing purposes
     * the JNDI lookup can be bypassed by defining a 'flowStoreURL' system property.
     *
     * @return flow-store service URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getFlowStoreServiceEndpoint() throws NamingException {
        String flowStoreServiceEndpoint = System.getProperty("flowStoreURL");
        if (flowStoreServiceEndpoint == null || flowStoreServiceEndpoint.isEmpty()) {
            flowStoreServiceEndpoint = getStringValueFromResource(FLOW_STORE_SERVICE_ENDPOINT_RESOURCE);
        }
        return flowStoreServiceEndpoint;
    }

    /**
     * Looks up job-store service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value #JOB_STORE_SERVICE_ENDPOINT_RESOURCE}'. For testing purposes
     * the JNDI lookup can be bypassed by defining a '{@value #JOB_STORE_SERVICE_ENDPOINT_RESOURCE}'
     * system property.
     *
     * @return job-store service URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getJobStoreServiceEndpoint() throws NamingException {
        String jobStoreServiceEndpoint = System.getProperty(JOB_STORE_SERVICE_ENDPOINT_RESOURCE);
        if (jobStoreServiceEndpoint == null || jobStoreServiceEndpoint.isEmpty()) {
            jobStoreServiceEndpoint = getStringValueFromResource(JOB_STORE_SERVICE_ENDPOINT_RESOURCE);
        }
        return jobStoreServiceEndpoint;
    }

    /**
     * Looks up sink service endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value #SINK_SERVICE_ENDPOINT_RESOURCE}'. For testing purposes
     * the JNDI lookup can be bypassed by defining a '{@value #SINK_SERVICE_ENDPOINT_RESOURCE}'
     * system property.
     *
     * @return sink service URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getSinkServiceEndpoint() throws NamingException {
        String sinkServiceEndpoint = System.getProperty(SINK_SERVICE_ENDPOINT_RESOURCE);
        if (sinkServiceEndpoint == null || sinkServiceEndpoint.isEmpty()) {
            sinkServiceEndpoint = getStringValueFromResource(SINK_SERVICE_ENDPOINT_RESOURCE);
        }
        return sinkServiceEndpoint;
    }

    /**
     * Looks up subversion SCM endpoint through Java Naming and Directory Interface (JNDI)
     * using the name '{@value #SUBVERSION_SCM_ENDPOINT_RESOURCE}'. For testing purposes
     * the JNDI lookup can be bypassed by defining a '{@value #SUBVERSION_SCM_ENDPOINT_RESOURCE}'
     * system property.
     *
     * @return subversion repository URL as String
     *
     * @throws NamingException if unable to lookup name
     */
    public static String getSubversionScmEndpoint() throws NamingException {
        String subversionScmEndpoint = System.getProperty(SUBVERSION_SCM_ENDPOINT_RESOURCE);
        if (subversionScmEndpoint == null || subversionScmEndpoint.isEmpty()) {
            subversionScmEndpoint = getStringValueFromResource(SUBVERSION_SCM_ENDPOINT_RESOURCE);
        }
        return subversionScmEndpoint;
    }

    /**
     * Builds service method response
     *
     * @param status HTTP status code of response
     * @param entity entity to include in response
     * @param <T> type parameter for entity type
     *
     * @return response object
     */
    public static <T> Response buildResponse(Response.Status status, T entity) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Returns JSON string representation of dk.dbc.dataio.commons.types.ServiceError object
     * constructed from given error message
     *
     * @param errorMessage error message
     *
     * @return JSON string representation of ServiceError object
     */
    public static String asJsonError(String errorMessage) {
        String error = null;
        try {
            error = JsonUtil.toJson(new ServiceError(errorMessage));
        } catch (JsonException e) {
            log.error("Caught exception trying to create JSON representation of error", e);
        }
        return error;
    }

    /**
     * Returns JSON string representation of dk.dbc.dataio.commons.types.ServiceError object
     * constructed from given exception
     *
     * @param ex exception to wrap
     *
     * @return JSON string representation of ServiceError object
     */
    public static String asJsonError(Exception ex) {
        String error = null;
        try {
            log.error("Generating error based on exception", ex);
            error = JsonUtil.toJson(new ServiceError(ex.getMessage(), stackTraceToString(ex)));
        } catch (JsonException e) {
            log.error("Caught exception trying to create JSON representation of error", e);
        }
        return error;
    }

    public static String stackTraceToString(Throwable t) {
        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String getStringValueFromResource(String resourceName) throws NamingException {
        String resourceValue;
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            resourceValue = (String) initialContext.lookup(resourceName);
        } finally {
            closeInitialContext(initialContext);
        }
        return resourceValue;
    }

    private static void closeInitialContext(InitialContext initialContext) {
        if (initialContext != null) {
            try {
                initialContext.close();
            } catch (NamingException e) {
                log.warn("Unable to close initial context", e);
            }
        }
    }
}
