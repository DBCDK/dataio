package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import dk.dbc.dataio.gui.client.exceptions.texts.LogMessageTexts;

/**
 * Created by ThomasBerg on 09/11/15.
 */
@GinModules(ViewModule.class)
public interface ViewGinjector extends Ginjector {
    View getView();

    Texts getTexts();

    LogMessageTexts getLogMessageTexts();
}
