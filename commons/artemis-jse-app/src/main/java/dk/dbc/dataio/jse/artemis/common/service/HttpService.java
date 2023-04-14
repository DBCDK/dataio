package dk.dbc.dataio.jse.artemis.common.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HttpService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpService.class);
    private final Server server = new Server();
    private final ServerConnector sc;
    private final ServletContextHandler context;

    public HttpService(int port) {
        HttpConfiguration conf = new HttpConfiguration();
        conf.setHttpCompliance(HttpCompliance.LEGACY);
        conf.setUriCompliance(UriCompliance.LEGACY);
        HttpConnectionFactory cf = new HttpConnectionFactory(conf);
        sc = new ServerConnector(server, cf);
        sc.setPort(port);
        server.setConnectors(new Connector[] {sc});
        context = new ServletContextHandler();
        server.setHandler(context);
    }

    public void start() {
        try {
            server.start();
            LOGGER.info("Webserver started on port {}", sc.getLocalPort());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addServlet(AServlet servlet, String path) {
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("text/plain");
                servlet.doGet(req, resp);
            }
        }), path);
    }

    @Override
    public void close() throws Exception {
        sc.close();
        server.stop();
    }

    public interface AServlet {
        void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
    }
}
