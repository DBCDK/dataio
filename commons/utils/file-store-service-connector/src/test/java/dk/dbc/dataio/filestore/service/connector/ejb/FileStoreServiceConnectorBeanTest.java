package dk.dbc.dataio.filestore.service.connector.ejb;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;
import javax.naming.NamingException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceUtil.class,
})
public class FileStoreServiceConnectorBeanTest {
    private static final InputStream INPUT_STREAM = mock(InputStream.class);
    private static final String FILE_ID = "42";

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
    }

    @Test
    public void addFile_endpointLookupThrowsNamingException_throws() throws NamingException, FileStoreServiceConnectorException {
        final NamingException namingException = new NamingException();
        when(ServiceUtil.getFileStoreServiceEndpoint()).thenThrow(namingException);
        final FileStoreServiceConnectorBean fileStoreServiceConnectorBean = getInitializedBean();
        try {
            fileStoreServiceConnectorBean.addFile(INPUT_STREAM);
            fail("No exception thrown");
        } catch (EJBException e) {
            assertThat(e.getCause() instanceof NamingException, is(true));
            assertThat((NamingException) e.getCause(), is(namingException));
        }
    }

    @Test
    public void getFile_endpointLookupThrowsNamingException_throws() throws NamingException, FileStoreServiceConnectorException {
        final NamingException namingException = new NamingException();
        when(ServiceUtil.getFileStoreServiceEndpoint()).thenThrow(namingException);
        final FileStoreServiceConnectorBean fileStoreServiceConnectorBean = getInitializedBean();
        try {
            fileStoreServiceConnectorBean.getFile(FILE_ID);
            fail("No exception thrown");
        } catch (EJBException e) {
            assertThat(e.getCause() instanceof NamingException, is(true));
            assertThat((NamingException) e.getCause(), is(namingException));
        }
    }

    @Test
    public void getByteSize_endpointLookupThrowsNamingException_throws() throws NamingException, FileStoreServiceConnectorException {
        final NamingException namingException = new NamingException();
        when(ServiceUtil.getFileStoreServiceEndpoint()).thenThrow(namingException);
        final FileStoreServiceConnectorBean fileStoreServiceConnectorBean = getInitializedBean();
        try {
            fileStoreServiceConnectorBean.getByteSize(FILE_ID);
            fail("No exception thrown");
        } catch (EJBException e) {
            assertThat(e.getCause() instanceof NamingException, is(true));
            assertThat((NamingException) e.getCause(), is(namingException));
        }
    }

    private FileStoreServiceConnectorBean getInitializedBean() {
        return new FileStoreServiceConnectorBean();
    }
}