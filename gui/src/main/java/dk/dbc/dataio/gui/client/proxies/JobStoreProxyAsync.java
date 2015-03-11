package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.JobModelOld;

import java.util.List;

public interface JobStoreProxyAsync {
    void getJobStoreFilesystemUrl(AsyncCallback<String> async);
    void findAllJobs(AsyncCallback<List<JobInfo>> async);
    void findAllJobsNew(AsyncCallback<List<JobModelOld>> async);
    void getJobCompletionState(long jobId, AsyncCallback<JobCompletionState> async);
    void listJobs(JobListCriteriaModel model, AsyncCallback<List<JobModel>> async);
    void listItems(ItemListCriteriaModel model, AsyncCallback<List<ItemModel>> async);
    void close(AsyncCallback<Void> async);

}
