package dk.dbc.dataio.harvester.rawrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

public class HarvesterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void harvest() {
        LOGGER.info("Harvesting...");
    }
}
