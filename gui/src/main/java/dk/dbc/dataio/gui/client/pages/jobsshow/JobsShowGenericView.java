package dk.dbc.dataio.gui.client.pages.jobsshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.views.GenericView;

import java.util.List;

public interface JobsShowGenericView extends IsWidget, GenericView<JobsShowGenericPresenter> {
    void setJobs(List<JobInfo> job);
}
