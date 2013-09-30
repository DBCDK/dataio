package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.servlet.ServletException;

public class ServletUtil {
    private static final Logger log = LoggerFactory.getLogger(ServletUtil.class);

    private ServletUtil() { }
    
    public static String getFlowStoreServiceEndpoint() throws ServletException {
        try {
            return ServiceUtil.getFlowStoreServiceEndpoint();
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    public static String getSubversionScmEndpoint() throws ServletException {
        try {
            return ServiceUtil.getSubversionScmEndpoint();
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }
}
