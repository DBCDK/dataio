package dk.dbc.dataio.gui.server;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletUtil {
    private static final Logger log = LoggerFactory.getLogger(ServletUtil.class);

    private static final String FLOW_STORE_SERVICE_ENDPOINT_RESOURCE = "dataioGuiFlowStoreServiceEndpoint";
    
    public static String getFlowStoreServiceEndpoint() throws ServletException {

        String flowStoreServiceEndpoint = System.getProperty("flowStoreURL");
        if (flowStoreServiceEndpoint == null || flowStoreServiceEndpoint.isEmpty()) {
            InitialContext initialContext = null;
            try {
                initialContext = new InitialContext();
                flowStoreServiceEndpoint = (String) initialContext.lookup(FLOW_STORE_SERVICE_ENDPOINT_RESOURCE);
            } catch (NamingException e) {
                throw new ServletException(e);
            } finally {
                closeInitialContext(initialContext);
            }
        }
        return flowStoreServiceEndpoint;
    }
    
    public static void closeInitialContext(InitialContext initialContext) {
        if (initialContext != null) {
            try {
                initialContext.close();
            } catch (NamingException e) {
                log.warn("Unable to close initial context", e);
            }
        }
    }
}
