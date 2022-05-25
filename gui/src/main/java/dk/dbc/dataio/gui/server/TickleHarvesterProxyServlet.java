package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.TickleHarvesterProxy;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

import javax.servlet.ServletException;

public class TickleHarvesterProxyServlet extends RemoteServiceServlet implements TickleHarvesterProxy {

    private transient TickleHarvesterProxy tickleHarvesterProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        tickleHarvesterProxy = new TickleHarvesterProxyImpl();
    }

    @Override
    public void createHarvestTask(TickleRepoHarvesterConfig config) throws ProxyException {
        tickleHarvesterProxy.createHarvestTask(config);
    }

    @Override
    public int getDataSetSizeEstimate(String dataSetName) throws ProxyException {
        return tickleHarvesterProxy.getDataSetSizeEstimate(dataSetName);
    }

    @Override
    public void deleteOutdatedRecords(String dataSetName, long fromDateEpochMillis) throws ProxyException {
        tickleHarvesterProxy.deleteOutdatedRecords(dataSetName, fromDateEpochMillis);
    }

    @Override
    public void close() {
        if (tickleHarvesterProxy != null) {
            tickleHarvesterProxy.close();
            tickleHarvesterProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }
}
