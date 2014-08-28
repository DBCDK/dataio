package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;

import javax.naming.NamingException;
import javax.servlet.ServletException;

public class ServletUtil {

    private ServletUtil() { }

    public static String getJobStoreFilesystemUrl() throws ServletException {
        try {
            return ServiceUtil.getJobStoreFilesystemUrl();
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    public static String getFlowStoreServiceEndpoint() throws ServletException {
        try {
            return ServiceUtil.getFlowStoreServiceEndpoint();
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    public static String getSinkServiceEndpoint() throws ServletException {
        try {
            return ServiceUtil.getSinkServiceEndpoint();
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    public static String getJobStoreServiceEndpoint() throws ServletException {
        try {
            return ServiceUtil.getJobStoreServiceEndpoint();
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
