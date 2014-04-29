package dk.dbc.dataio.bfs.ejb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This Enterprise Java Bean (EJB) singleton bean is used to gain
 * access to binary file store configuration.
 */
@Singleton
@LocalBean
public class BinaryFileStoreConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryFileStoreConfigurationBean.class);

    @Resource(name = "basePath")
    String basePath;

    @PostConstruct
    public void initialize() {
        LOGGER.info("basePath={}", basePath);
    }

    public Path getBasePath() {
        return Paths.get(basePath);
    }
}
