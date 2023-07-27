package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

import java.util.List;

public interface JobStoreProxyAsync {
    void listJobs(JobListCriteria model, AsyncCallback<List<JobModel>> async);

    void fetchEarliestActiveJob(int sinkId, AsyncCallback<JobModel> async);

    void countJobs(JobListCriteria model, AsyncCallback<Long> async);

    void listItems(ItemListCriteria.Field searchType, ItemListCriteria criteria, AsyncCallback<List<ItemModel>> async);

    void countItems(ItemListCriteria criteria, AsyncCallback<Long> async);

    void getItemData(ItemModel itemModel, ItemModel.LifeCycle lifeCycle, AsyncCallback<String> async);

    void getProcessedNextResult(int jobId, int chunkId, short itemId, AsyncCallback<String> async);

    void listJobNotificationsForJob(int jobId, AsyncCallback<List<Notification>> async);

    void reSubmitJob(JobModel jobModel, AsyncCallback<JobModel> async);
    void resendJob(JobModel jobModel, AsyncCallback<JobModel> async);

    void abortJob(JobModel jobModel, AsyncCallback<JobModel> async);

    void reSubmitJobs(List<JobModel> jobModels, AsyncCallback<List<JobModel>> async);

    void listInvalidTransfileNotifications(AsyncCallback<List<Notification>> async);

    void setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId, AsyncCallback<JobModel> async);

    void setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId, int chunkId, short itemId, AsyncCallback<ItemModel> async);

    void getSinkStatusModels(AsyncCallback<List<SinkStatusTable.SinkStatusModel>> async);

    void createJobRerun(int jobId, boolean failedItemsOnly, AsyncCallback<Void> async);

    void close(AsyncCallback<Void> async);

}
