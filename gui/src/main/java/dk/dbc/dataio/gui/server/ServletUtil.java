package dk.dbc.dataio.gui.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

public class ServletUtil {
    private static final Logger log = LoggerFactory.getLogger(ServletUtil.class);

    private static final String FLOW_STORE_SERVICE_ENDPOINT_RESOURCE = "dataioGuiFlowStoreServiceEndpoint";
    private static final String SUBVERSION_SCM_ENDPOINT_RESOURCE = "dataioGuiSubversionScmEndpoint";

    private ServletUtil() { }
    
    public static String getFlowStoreServiceEndpoint() throws ServletException {
        String flowStoreServiceEndpoint = System.getProperty("flowStoreURL");
        if (flowStoreServiceEndpoint == null || flowStoreServiceEndpoint.isEmpty()) {
            flowStoreServiceEndpoint = getStringValueFromResource(FLOW_STORE_SERVICE_ENDPOINT_RESOURCE);
        }
        return flowStoreServiceEndpoint;
    }

    public static String getSubversionScmEndpoint() throws ServletException {
        String subversionScmEndpoint = System.getProperty(SUBVERSION_SCM_ENDPOINT_RESOURCE);
        if (subversionScmEndpoint == null || subversionScmEndpoint.isEmpty()) {
            subversionScmEndpoint = getStringValueFromResource(SUBVERSION_SCM_ENDPOINT_RESOURCE);
        }
        return subversionScmEndpoint;
    }

    private static String getStringValueFromResource(String resourceName) throws ServletException {
        String resourceValue;
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            resourceValue = (String) initialContext.lookup(resourceName);
        } catch (NamingException e) {
            throw new ServletException(e);
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
