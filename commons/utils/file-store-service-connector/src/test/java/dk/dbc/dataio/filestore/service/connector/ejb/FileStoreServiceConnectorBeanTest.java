package dk.dbc.dataio.filestore.service.connector.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceUtil.class
})
public class FileStoreServiceConnectorBeanTest {

    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowNamingException_throws() {
        final FileStoreServiceConnectorBean jobStoreServiceConnectorBean = new FileStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void setConnector_connectorIsSet_connectorIsReturned() {
        FileStoreServiceConnector fileStoreServiceConnector = Mockito.mock(FileStoreServiceConnector.class);
        FileStoreServiceConnectorBean fileStoreServiceConnectorBean = getInitializedBean();
        fileStoreServiceConnectorBean.fileStoreServiceConnector = fileStoreServiceConnector;

        MatcherAssert.assertThat(fileStoreServiceConnectorBean.getConnector(), is(fileStoreServiceConnector));
    }

    @Test
    public void initializeConnector_connectorIsInitialized_connectorIsNotNull() throws Exception{
        mockStatic(ServiceUtil.class);
        Mockito.when(ServiceUtil.getStringValueFromResource(JndiConstants.URL_RESOURCE_FILESTORE_RS)).thenReturn("fileStoreEndpoint");
        FileStoreServiceConnectorBean fileStoreServiceConnectorBean = getInitializedBean();
        fileStoreServiceConnectorBean.initializeConnector();

        MatcherAssert.assertThat(fileStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    /*
     * Private methods
     */
    private FileStoreServiceConnectorBean getInitializedBean() {
        return new FileStoreServiceConnectorBean();
    }
}