package dk.dbc.dataio.filestore.service.connector.ejb;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileStoreServiceConnectorBeanTest {
    @Test
    public void initializeConnector_endpointNotSet_throws() {
        assertThrows(NullPointerException.class, FileStoreServiceConnectorBean::new);
    }

    @Test
    public void initializeConnector() {
        final FileStoreServiceConnectorBean fileStoreServiceConnectorBean = newFileStoreServiceConnectorBean();
        assertThat(fileStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    private FileStoreServiceConnectorBean newFileStoreServiceConnectorBean() {
        return new FileStoreServiceConnectorBean("http://test");
    }
}
