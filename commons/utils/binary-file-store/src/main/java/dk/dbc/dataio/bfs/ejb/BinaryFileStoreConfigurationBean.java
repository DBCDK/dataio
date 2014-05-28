package dk.dbc.dataio.bfs.ejb;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.naming.NamingException;
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

    @Resource(lookup = "java:app/env/dataio/bfs/jndi/path")
    String basePathJndiName;

    private String basePath;

    @PostConstruct
    public void initialize() {
        LOGGER.info("basePathJndiName={}", basePathJndiName);
        try {
            basePath = ServiceUtil.getStringValueFromResource(basePathJndiName);
        } catch (NamingException e) {
            LOGGER.error("Exception caught during JNDI lookup of {}", basePathJndiName, e);
            throw new EJBException(e);
        }
        LOGGER.info("basePath={}", basePath);
    }

    public Path getBasePath() {
        return Paths.get(basePath);
    }
}
