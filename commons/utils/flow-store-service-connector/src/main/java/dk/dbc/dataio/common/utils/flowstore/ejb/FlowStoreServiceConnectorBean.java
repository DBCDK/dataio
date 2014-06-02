package dk.dbc.dataio.common.utils.flowstore.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.util.List;

/**
 * Created by sma on 29/04/14.
 *
 * This Enterprise Java Bean (EJB) singleton is used as a connector
 * to the flow-store REST interface.
 */
@Singleton
@LocalBean
public class FlowStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStoreServiceConnectorBean.class);

    Client client;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        client = HttpClient.newClient(new ClientConfig().register(new Jackson2xFeature()));
    }

    @Lock(LockType.READ)
    public Sink createSink(SinkContent sinkContent) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Creating new sink");
        try {
            return getFlowStoreServiceConnector().createSink(sinkContent);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public Sink getSink(long sinkId) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Retrieving sink with id: " + sinkId);
        try {
            return getFlowStoreServiceConnector().getSink(sinkId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public List<Sink> findAllSinks() throws FlowStoreServiceConnectorException {
        LOGGER.debug("Retrieving all sinks");
        try{
            return getFlowStoreServiceConnector().findAllSinks();
        }catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public Sink updateSink(SinkContent sinkContent, long id, long version) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Updating existing sink");
        try{
            return getFlowStoreServiceConnector().updateSink(sinkContent, id, version);
        }catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public Submitter createSubmitter(SubmitterContent submitterContent) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Creating new submitter");
        try {
            return getFlowStoreServiceConnector().createSubmitter(submitterContent);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public List<Submitter> findAllSubmitters() throws FlowStoreServiceConnectorException {
        LOGGER.debug("Retrieving all submitters");
        try{
            return getFlowStoreServiceConnector().findAllSubmitters();
        }catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public Flow createFlow(FlowContent flowContent) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Creating new flow");
        try {
            return getFlowStoreServiceConnector().createFlow(flowContent);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public Flow getFlow(long flowId) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Retrieving flow with id: " + flowId);
        try {
            return getFlowStoreServiceConnector().getFlow(flowId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public List<Flow> findAllFlows() throws FlowStoreServiceConnectorException {
        LOGGER.debug("Retrieving all flows");
        try{
            return getFlowStoreServiceConnector().findAllFlows();
        }catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public FlowComponent createFlowComponent(FlowComponentContent flowComponentContent) throws FlowStoreServiceConnectorException {
        LOGGER.debug("Creating new flowComponent");
        try {
            return getFlowStoreServiceConnector().createFlowComponent(flowComponentContent);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Lock(LockType.READ)
    public List<FlowComponent> findAllFlowComponents() throws FlowStoreServiceConnectorException {
        LOGGER.debug("Retrieving all flow components");
        try{
            return getFlowStoreServiceConnector().findAllFlowComponents();
        }catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    private FlowStoreServiceConnector getFlowStoreServiceConnector() throws NamingException{
        final String baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
        return new FlowStoreServiceConnector(client, baseUrl);
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(client);
    }
}
