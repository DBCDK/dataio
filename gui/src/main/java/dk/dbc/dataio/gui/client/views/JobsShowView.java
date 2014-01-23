package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.presenters.JobsShowPresenter;
import java.util.List;

public interface JobsShowView extends IsWidget, View<JobsShowPresenter> {
    void setJobs(List<JobInfo> job);
}
