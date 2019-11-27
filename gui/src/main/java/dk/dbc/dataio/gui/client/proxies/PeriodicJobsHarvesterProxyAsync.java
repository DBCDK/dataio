package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

public interface PeriodicJobsHarvesterProxyAsync {
    void createPeriodicJob(Long id, AsyncCallback<Void> async);
}
