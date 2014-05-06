package dk.dbc.dataio.harvester.rawrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class ScheduledHarvestBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledHarvestBean.class);
}
