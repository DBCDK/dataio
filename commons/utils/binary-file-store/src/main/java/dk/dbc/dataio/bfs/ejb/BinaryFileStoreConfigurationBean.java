package dk.dbc.dataio.bfs.ejb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class BinaryFileStoreConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryFileStoreConfigurationBean.class);

    String basePath;

    @PostConstruct
    public void initialize() {
        basePath = System.getenv("BFS_ROOT");
        if (basePath == null || basePath.trim().isEmpty()) {
            throw new EJBException("BFS_ROOT must be set");
        }
        LOGGER.info("basePath={}", basePath);
    }

    public Path getBasePath() {
        return Paths.get(basePath);
    }
}
