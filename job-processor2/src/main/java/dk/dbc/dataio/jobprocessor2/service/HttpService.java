package dk.dbc.dataio.jobprocessor2.service;

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

import javax.ws.rs.core.MediaType;
import java.io.IOException;

public class HttpService implements AutoCloseable {
    private final Server server = new Server();
    private final ServerConnector sc;
    private final ServletContextHandler context;

    public HttpService(int port) {
        try {
            HttpConfiguration conf = new HttpConfiguration();
            conf.setHttpCompliance(HttpCompliance.LEGACY);
            conf.setUriCompliance(UriCompliance.LEGACY);
            HttpConnectionFactory cf = new HttpConnectionFactory(conf);
            sc = new ServerConnector(server, cf);
            sc.setPort(port);
            server.setConnectors(new Connector[] {sc});
            context = new ServletContextHandler();
            server.setHandler(context);
            server.start();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void addServlet(AServlet servlet, String path) {
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType(MediaType.TEXT_PLAIN);
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
