package dk.dbc.dataio.jobstore.service.rs;

import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

//@WebServlet(value = "/pre-shutdown", loadOnStartup = 1)
//@Provider
public class ResponseNodeDecorator extends HttpServlet implements ContainerResponseFilter, ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseNodeDecorator.class);
    private static final AtomicBoolean STOPPING = new AtomicBoolean(false);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().add("Cluster-Node", InetAddress.getLocalHost().getHostName());
        if(isStopping()) responseContext.getHeaders().add("Connection", "close");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.info("{} initiated shutdown", req.getRemoteHost());
        resp.setContentType(MediaType.TEXT_PLAIN);
        try(PrintWriter writer = resp.getWriter()) {
            writer.println("preparing shutdown");
        }
        stop();
    }

    public static boolean isStopping() {
        return STOPPING.get();
    }

    public static void stop() {
        STOPPING.set(true);
    }
}
