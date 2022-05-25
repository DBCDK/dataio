package dk.dbc.dataio.gui.server;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UrlResolverServlet extends HttpServlet {
    private static final long serialVersionUID = -6885510844881237998L;
    private static final Logger log = LoggerFactory.getLogger(UrlResolverServlet.class);

    private final JSONBContext jsonbContext = new JSONBContext();
    private String urlsJson;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            urlsJson = jsonbContext.marshall(Urls.getInstance());
        } catch (JSONBException e) {
            throw new ServletException(e);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.getWriter().print(urlsJson);
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            log.info("getUrls() failed with exception" + e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
