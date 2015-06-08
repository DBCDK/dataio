package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;

import java.util.List;

public interface JobStoreProxyAsync {
    void listJobs(JobListCriteriaModel model, AsyncCallback<List<JobModel>> async);
    void listItems(ItemListCriteriaModel model, AsyncCallback<List<ItemModel>> async);
//    void getChunkItem(ItemModel model, ItemModel.LifeCycle lifeCycle, AsyncCallback<ChunkItemModel> async);
    void getItemData(int jobId, int chunkId, short itemId, ItemModel.LifeCycle lifeCycle, AsyncCallback<String> async);
    void close(AsyncCallback<Void> async);
}
