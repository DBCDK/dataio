package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

public interface TickleHarvesterProxyAsync {

    void createHarvestTask(TickleRepoHarvesterConfig config, AsyncCallback<Void> async);

    void getDataSetSizeEstimate(String dataSetName, AsyncCallback<Integer> async);

    void deleteOutdatedRecords(String dataSetName, long fromDateEpochMillis, AsyncCallback<Void> async);

    void close(AsyncCallback<Void> async);
}
