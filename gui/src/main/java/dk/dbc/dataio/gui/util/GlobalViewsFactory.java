package dk.dbc.dataio.gui.util;

import com.google.gwt.core.client.GWT;
import dk.dbc.dataio.gui.client.pages.item.show.ViewGinjector;
import dk.dbc.dataio.gui.client.pages.job.show.ViewJobsGinjector;
import dk.dbc.dataio.gui.client.pages.job.show.ViewTestJobsGinjector;

/**
 * Created by ThomasBerg on 18/11/15.
 */
public class GlobalViewsFactory {

    private ViewJobsGinjector viewJobsInjector = GWT.create(ViewJobsGinjector.class);
    private ViewTestJobsGinjector viewTestJobsGinjector = GWT.create(ViewTestJobsGinjector.class);
    private ViewGinjector viewItemGinjector= GWT.create(ViewGinjector.class);

    public dk.dbc.dataio.gui.client.pages.job.show.View getJobsView() {
       return viewJobsInjector.getView();
    }
    public dk.dbc.dataio.gui.client.pages.job.show.View getTestJobsView() {
       return viewTestJobsGinjector.getView();
    }
    public dk.dbc.dataio.gui.client.pages.item.show.View getItemsView() {
        return viewItemGinjector.getView();
    }
}
