package dk.dbc.dataio.gui.client.pages.job.show;

import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.List;
import java.util.Map;

public interface Presenter extends GenericPresenter {
    void itemSelected(JobModel jobModel);

    void updateSelectedJobs();

    void refresh();

    void showJob();

    void showLog();

    void clearLog();

    void showHistory();

    void changeColorSchemeListBoxShow();

    void changeColorScheme(Map<String, String> colorScheme);

    void setWorkflowNote(WorkflowNoteModel workflowNoteModel, String jobId);

    WorkflowNoteModel preProcessAssignee(WorkflowNoteModel workflowNoteModel, String assignee);

    void setIsMultipleRerun(boolean isMultipleRerun);

    void getJobRerunScheme(JobModel jobModel);
    void resendJob(JobModel jobModel);

    void abortJob(JobModel jobModel);

    void rerun();

    List<JobModel> getShownJobModels();

    List<JobModel> validRerunJobsFilter(List<JobModel> jobModels);

    void setPlace(AbstractBasePlace place);
}
