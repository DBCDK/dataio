package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.model.JobModel;

import java.util.List;

public interface JobStoreProxyAsync {
    void getJobStoreFilesystemUrl(AsyncCallback<String> async);
    void findAllJobs(AsyncCallback<List<JobInfo>> async);
    void findAllJobsNew(AsyncCallback<List<JobModel>> async);
    void getJobCompletionState(long jobId, AsyncCallback<JobCompletionState> async);
    void close(AsyncCallback<Void> async);

}
