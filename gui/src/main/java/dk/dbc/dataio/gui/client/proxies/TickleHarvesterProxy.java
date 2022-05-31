package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

@RemoteServiceRelativePath("TickleHarvesterProxy")
public interface TickleHarvesterProxy extends RemoteService {

    void createHarvestTask(TickleRepoHarvesterConfig config) throws ProxyException;

    int getDataSetSizeEstimate(String dataSetName) throws ProxyException;

    void deleteOutdatedRecords(String dataSetName, long fromDateEpochMillis) throws ProxyException;

    void close();

    class Factory {

        private static TickleHarvesterProxyAsync asyncInstance = null;

        public static TickleHarvesterProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(TickleHarvesterProxy.class);
            }
            return asyncInstance;
        }
    }


}
