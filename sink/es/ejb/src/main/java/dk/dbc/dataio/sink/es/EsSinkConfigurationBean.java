package dk.dbc.dataio.sink.es;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@LocalBean
@Singleton
@Startup
public class EsSinkConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsSinkConfigurationBean.class);

    @Resource(name = "esResourceName")
    String esResourceName;

    @Resource(name = "esDatabaseName")
    String esDatabaseName;

    @Resource(name = "esRecordsCapacity")
    int esRecordsCapacity;

    @PostConstruct
    public void initialize() {
        LOGGER.info("esResourceName={}", esResourceName);
        LOGGER.info("esDatabaseName={}", esDatabaseName);
        LOGGER.info("esRecordsCapacity={}", esRecordsCapacity);
    }

    public String getEsResourceName() {
        return esResourceName;
    }

    public String getEsDatabaseName() {
        return esDatabaseName;
    }

    public int getRecordsCapacity() {
        return esRecordsCapacity;
    }
}
