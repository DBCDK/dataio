package dk.dbc.dataio.filestore.service.connector.ejb;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;

public class FileStoreServiceConnectorBeanTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void initializeConnector_environmentNotSet_throws() {
        final FileStoreServiceConnectorBean jobStoreServiceConnectorBean = newFileStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void initializeConnector() {
        environmentVariables.set("FILESTORE_URL", "http://test");
        final FileStoreServiceConnectorBean fileStoreServiceConnectorBean =
                newFileStoreServiceConnectorBean();
        fileStoreServiceConnectorBean.initializeConnector();

        assertThat(fileStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    @Test
    public void getConnector() {
        final FileStoreServiceConnector fileStoreServiceConnector =
                mock(FileStoreServiceConnector.class);
        final FileStoreServiceConnectorBean fileStoreServiceConnectorBean =
                newFileStoreServiceConnectorBean();
        fileStoreServiceConnectorBean.fileStoreServiceConnector = fileStoreServiceConnector;

        assertThat(fileStoreServiceConnectorBean.getConnector(),
                is(fileStoreServiceConnector));
    }

    private FileStoreServiceConnectorBean newFileStoreServiceConnectorBean() {
        return new FileStoreServiceConnectorBean();
    }
}
