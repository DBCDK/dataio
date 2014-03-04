package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
        ServiceUtil.class
})
public class JobStoreServiceConnectorBeanTest {
    private final String jobStoreUrl = "http://dataio/job-store";
    private final Client client = mock(Client.class);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getJobStoreServiceEndpoint())
                .thenReturn(jobStoreUrl);
        when(HttpClient.newClient()).thenReturn(client);
    }

    @Test
    public void createJob_endpointLookupThrowsNamingException_throws() throws NamingException, JobStoreServiceConnectorException {
        final NamingException namingException = new NamingException();
        when(ServiceUtil.getJobStoreServiceEndpoint()).thenThrow(namingException);
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = getInitializedBean();
        try {
            jobStoreServiceConnectorBean.createJob(new JobSpecificationBuilder().build());
            fail("No exception thrown by createJob()");
        } catch (EJBException e) {
            assertThat((NamingException) e.getCause(), is(namingException));
        }
    }

    private JobStoreServiceConnectorBean getInitializedBean() {
        return new JobStoreServiceConnectorBean();
    }
}
