package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.TestFileStoreServiceConnector;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.enterprise.inject.Specializes;

/**
 * Created by ja7 on 1/5/17.
 * Test FilestoreConenctorBean for ArquillianTests
 */
@Specializes
@Singleton
@LocalBean
public class TestFileStoreServiceConnectorBean extends FileStoreServiceConnectorBean {
    @Override
    @PostConstruct
    public void initializeConnector() {}
    
    @Override
    public FileStoreServiceConnector getConnector() {
        return new TestFileStoreServiceConnector();
    }
}
