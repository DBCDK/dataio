package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;

@RemoteServiceRelativePath("PeriodicJobsHarvesterProxy")
public interface PeriodicJobsHarvesterProxy extends RemoteService {
    void executePeriodicJob(Long id) throws ProxyException;

    String executeSolrValidation(Long id) throws ProxyException;
}
