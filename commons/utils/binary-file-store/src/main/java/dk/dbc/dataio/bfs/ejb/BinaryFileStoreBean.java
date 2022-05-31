package dk.dbc.dataio.bfs.ejb;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import java.nio.file.Path;

/**
 * This Enterprise Java Bean (EJB) stateless bean is used to gain
 * access to binary file representations.
 * <p>
 * It is understood that the direct file system access used by the
 * current BinaryFileStore implementation is in violation of the
 * EJB specification.
 * </p>
 */
@Stateless
@LocalBean
public class BinaryFileStoreBean implements BinaryFileStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryFileStoreBean.class);
    private BinaryFileStore binaryFileStore;

    @EJB
    BinaryFileStoreConfigurationBean configuration;

    /**
     * Initializes BinaryFileStore implementation
     */
    @PostConstruct
    public void initializeBinaryFileStore() {
        LOGGER.debug("Initializing binary file store");
        binaryFileStore = new BinaryFileStoreFsImpl(configuration.getBasePath());
    }

    /**
     * Returns binary file representation associated with given path
     *
     * @param path binary file path
     * @return binary file representation
     */
    @Override
    public BinaryFile getBinaryFile(Path path) {
        return binaryFileStore.getBinaryFile(path);
    }
}
