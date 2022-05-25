package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.PeriodicJobsHarvesterProxy;

import javax.servlet.ServletException;

public class PeriodicJobsHarvesterProxyServlet extends RemoteServiceServlet implements PeriodicJobsHarvesterProxy {
    private transient PeriodicJobsHarvesterProxy periodicJobsHarvesterProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        periodicJobsHarvesterProxy = new PeriodicJobsHarvesterProxyImpl();
    }

    @Override
    public void executePeriodicJob(Long harvesterId) throws ProxyException {
        periodicJobsHarvesterProxy.executePeriodicJob(harvesterId);
    }

    public String executeSolrValidation(Long harvesterId) throws ProxyException {
        return periodicJobsHarvesterProxy.executeSolrValidation(harvesterId);
    }
}
