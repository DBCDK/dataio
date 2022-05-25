package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * Created by ThomasBerg on 09/11/15.
 */
@GinModules(ViewModule.class)
public interface ViewTestJobsGinjector extends Ginjector {
    View getView();

    Texts getTexts();
}
