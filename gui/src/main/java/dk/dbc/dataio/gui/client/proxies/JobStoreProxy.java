package dk.dbc.dataio.gui.client.proxies;


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;

import java.util.List;

@RemoteServiceRelativePath("JobStoreProxy")
public interface JobStoreProxy extends RemoteService {

    List<JobModel> listJobs(JobListCriteriaModel model) throws ProxyException;
    int countJobs(JobListCriteriaModel model) throws ProxyException;
    List<ItemModel> listItems(ItemListCriteriaModel model) throws ProxyException;
    String getItemData(int jobId, int chunkId, short itemId, ItemModel.LifeCycle lifeCycle) throws ProxyException;
    String getProcessedNextResult(int jobId, int chunkId, short itemId) throws ProxyException;

    void close();

    class Factory {

        private static JobStoreProxyAsync asyncInstance = null;

        public static JobStoreProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(JobStoreProxy.class);
            }
            return asyncInstance;
        }
    }
}
