package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

@GinModules(ViewModule.class)
public interface ViewAcctestJobsGinjector extends Ginjector {
    View getView();

    Texts getTexts();
}
