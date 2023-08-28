package dk.dbc.dataio.harvester.corepo;

import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.rrharvester.service.connector.ejb.RRHarvesterServiceConnectorBean;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Local
@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, CoRepoHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @Inject
    VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector;

    @EJB
    RRHarvesterServiceConnectorBean rrHarvesterServiceConnectorBean;

    @Override
    public int executeFor(CoRepoHarvesterConfig config) throws HarvesterException {
        return new HarvestOperation(config,
                flowStoreServiceConnectorBean.getConnector(),
                vipCoreLibraryRulesConnector,
                rrHarvesterServiceConnectorBean.getConnector()).execute();
    }

    @Override
    public HarvesterBean self() {
        return sessionContext.getBusinessObject(HarvesterBean.class);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
