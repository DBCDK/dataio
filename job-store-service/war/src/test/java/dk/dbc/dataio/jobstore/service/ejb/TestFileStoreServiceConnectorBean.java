package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.TestFileStoreServiceConnector;

import javax.enterprise.inject.Alternative;

/**
 * Created by ja7 on 1/5/17.
 * Test FilestoreConenctorBean for ArquillianTests
 */
@Alternative
public class TestFileStoreServiceConnectorBean extends dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean {
    @Override
    public FileStoreServiceConnector getConnector() {
        return new TestFileStoreServiceConnector();
    }
}
