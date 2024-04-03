package dk.dbc.dataio.jobstore.service.rs;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

@Provider
public class ResponseNodeDecorator implements ContainerResponseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseNodeDecorator.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        LOGGER.info("Adding header");
        containerResponseContext.getHeaders().add("Cluster-Node", InetAddress.getLocalHost().getHostName());
    }
}
