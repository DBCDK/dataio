package dk.dbc.dataio.commons.utils.newjobstore.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;

@Singleton
@LocalBean
public class JobStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreServiceConnectorBean.class);

    JobStoreServiceConnector jobStoreServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        Client client = HttpClient.newClient();
        try {
            final String endpoint = ServiceUtil.getStringValueFromResource(JndiConstants.URL_RESOURCE_JOBSTORE_RS);
            jobStoreServiceConnector = new JobStoreServiceConnector(client, endpoint);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public JobStoreServiceConnector getConnector() {
        return jobStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(jobStoreServiceConnector.getHttpClient());
    }
}
