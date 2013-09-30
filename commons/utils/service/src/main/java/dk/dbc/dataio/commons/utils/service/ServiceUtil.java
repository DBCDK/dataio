package dk.dbc.dataio.commons.utils.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This utility class provides convenience methods for working with Servlet/JAX-RS based web services
 */
public class ServiceUtil {
    private static final Logger log = LoggerFactory.getLogger(ServiceUtil.class);

    private static final String FLOW_STORE_SERVICE_ENDPOINT_RESOURCE = "dataioGuiFlowStoreServiceEndpoint";
    private static final String SUBVERSION_SCM_ENDPOINT_RESOURCE = "dataioGuiSubversionScmEndpoint";

    private ServiceUtil() { }

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

    private static String getStringValueFromResource(String resourceName) throws NamingException {
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
