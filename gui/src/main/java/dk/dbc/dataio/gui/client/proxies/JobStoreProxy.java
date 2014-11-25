package dk.dbc.dataio.gui.client.proxies;


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.JobModel;

import java.util.List;

@RemoteServiceRelativePath("JobStoreProxy")
public interface JobStoreProxy extends RemoteService {

    String getJobStoreFilesystemUrl() throws ProxyException;
    List<JobInfo> findAllJobs() throws ProxyException;
    List<JobModel> findAllJobsNew() throws ProxyException;
    JobCompletionState getJobCompletionState(long jobId) throws ProxyException;
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
