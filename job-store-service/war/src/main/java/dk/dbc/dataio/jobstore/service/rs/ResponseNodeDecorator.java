package dk.dbc.dataio.jobstore.service.rs;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Provider
public class ResponseNodeDecorator implements ContainerResponseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseNodeDecorator.class);
    private static final AtomicBoolean STOPPING = new AtomicBoolean(false);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().add("Cluster-Node", InetAddress.getLocalHost().getHostName());
        responseContext.getHeaders().add("Connection", "close");
    }

    public static boolean isStopping() {
        return STOPPING.get();
    }

    public static void stop() {
        STOPPING.set(true);
    }
}
