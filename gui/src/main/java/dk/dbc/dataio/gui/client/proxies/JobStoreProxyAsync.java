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
    void close(AsyncCallback<Void> async);

}
