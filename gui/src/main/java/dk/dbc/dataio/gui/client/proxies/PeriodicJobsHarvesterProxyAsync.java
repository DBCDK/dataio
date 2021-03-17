package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface PeriodicJobsHarvesterProxyAsync {
    void executePeriodicJob(Long id, AsyncCallback<Void> async);

    void executeSolrValidation(Long id, AsyncCallback<String> async);
}
