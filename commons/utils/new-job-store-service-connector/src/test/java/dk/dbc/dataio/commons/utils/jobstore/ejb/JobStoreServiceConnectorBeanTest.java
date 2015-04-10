package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceUtil.class
})
public class JobStoreServiceConnectorBeanTest {

    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowNamingException_throws() {
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = new JobStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void setConnector_connectorIsSet_connectorIsReturned() {
        JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        JobStoreServiceConnectorBean jobStoreServiceConnectorBean = getInitializedBean();
        jobStoreServiceConnectorBean.jobStoreServiceConnector = jobStoreServiceConnector;

        assertThat(jobStoreServiceConnectorBean.getConnector(), is(jobStoreServiceConnector));
    }

    @Test
    public void initializeConnector_connectorIsInitialized_connectorIsNotNull() throws Exception{
        mockStatic(ServiceUtil.class);
        when(ServiceUtil.getStringValueFromResource(JndiConstants.URL_RESOURCE_JOBSTORE_RS)).thenReturn("jobStoreEndpoint");
        JobStoreServiceConnectorBean jobStoreServiceConnectorBean = getInitializedBean();
        jobStoreServiceConnectorBean.initializeConnector();

        assertThat(jobStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

   /*
    * Private methods
    */
    private JobStoreServiceConnectorBean getInitializedBean() {
        return new JobStoreServiceConnectorBean();
    }
}
