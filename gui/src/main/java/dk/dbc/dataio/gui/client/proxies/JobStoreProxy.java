package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

import java.util.List;

@RemoteServiceRelativePath("JobStoreProxy")
public interface JobStoreProxy extends RemoteService {
    List<JobModel> listJobs(JobListCriteria model) throws ProxyException;

    JobModel fetchEarliestActiveJob(int sinkId) throws ProxyException;

    long countJobs(JobListCriteria model) throws ProxyException;

    List<ItemModel> listItems(ItemListCriteria.Field searchType, ItemListCriteria criteria) throws ProxyException;

    long countItems(ItemListCriteria criteria) throws ProxyException;

    String getItemData(ItemModel itemModel, ItemModel.LifeCycle lifeCycle) throws ProxyException;

    String getProcessedNextResult(int jobId, int chunkId, short itemId) throws ProxyException;

    List<Notification> listJobNotificationsForJob(int jobId) throws ProxyException;

    JobModel reSubmitJob(JobModel jobModel) throws ProxyException;
    JobModel resendJob(JobModel jobModel) throws ProxyException;

    List<JobModel> reSubmitJobs(List<JobModel> jobModels) throws ProxyException;

    JobModel abortJob(JobModel jobModel) throws ProxyException;

    List<Notification> listInvalidTransfileNotifications() throws ProxyException;

    JobModel setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId) throws ProxyException;

    ItemModel setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId, int chunkId, short itemId) throws ProxyException;

    List<SinkStatusTable.SinkStatusModel> getSinkStatusModels() throws ProxyException;

    void createJobRerun(int jobId, boolean failedItemsOnly) throws ProxyException;

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
