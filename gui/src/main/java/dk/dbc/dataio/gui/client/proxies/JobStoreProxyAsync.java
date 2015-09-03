package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

import java.util.List;

public interface JobStoreProxyAsync {
    void listJobs(JobListCriteria model, AsyncCallback<List<JobModel>> async);
    void countJobs(JobListCriteria model, AsyncCallback<Long> async);
    void listItems(ItemListCriteria.Field searchType, ItemListCriteria criteria, AsyncCallback<List<ItemModel>> async);
    void countItems(ItemListCriteria criteria, AsyncCallback<Long> async);
    void getItemData(int jobId, int chunkId, short itemId, ItemModel.LifeCycle lifeCycle, AsyncCallback<String> async);
    void getProcessedNextResult(int jobId, int chunkId, short itemId, AsyncCallback<String> async);
    void close(AsyncCallback<Void> async);
}
