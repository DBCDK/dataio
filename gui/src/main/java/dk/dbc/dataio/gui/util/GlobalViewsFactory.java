package dk.dbc.dataio.gui.util;

import com.google.gwt.core.client.GWT;
import dk.dbc.dataio.gui.client.pages.item.show.ViewGinjector;
import dk.dbc.dataio.gui.client.pages.job.show.ViewAcctestJobsGinjector;
import dk.dbc.dataio.gui.client.pages.job.show.ViewJobsGinjector;
import dk.dbc.dataio.gui.client.pages.job.show.ViewPeriodicJobsGinjector;
import dk.dbc.dataio.gui.client.pages.job.show.ViewTestJobsGinjector;

public class GlobalViewsFactory {

    private ViewJobsGinjector viewJobsInjector = GWT.create(ViewJobsGinjector.class);
    private ViewPeriodicJobsGinjector viewPeriodicJobsGinjector = GWT.create(ViewPeriodicJobsGinjector.class);
    private ViewTestJobsGinjector viewTestJobsGinjector = GWT.create(ViewTestJobsGinjector.class);
    private ViewAcctestJobsGinjector viewAcctestJobsGinjector = GWT.create(ViewAcctestJobsGinjector.class);
    private ViewGinjector viewItemGinjector = GWT.create(ViewGinjector.class);

    public dk.dbc.dataio.gui.client.pages.job.show.View getJobsView() {
        return viewJobsInjector.getView();
    }

    public dk.dbc.dataio.gui.client.pages.job.show.View getPeriodicJobsView() {
        return viewPeriodicJobsGinjector.getView();
    }

    public dk.dbc.dataio.gui.client.pages.job.show.View getTestJobsView() {
        return viewTestJobsGinjector.getView();
    }

    public dk.dbc.dataio.gui.client.pages.job.show.View getAcctestJobsView() {
        return viewAcctestJobsGinjector.getView();
    }

    public dk.dbc.dataio.gui.client.pages.item.show.View getItemsView() {
        return viewItemGinjector.getView();
    }
}
