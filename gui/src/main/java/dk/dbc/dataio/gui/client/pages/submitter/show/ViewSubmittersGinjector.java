package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * Ginjector for submitter show
 */
@GinModules(ViewModule.class)
public interface ViewSubmittersGinjector extends Ginjector {
    View getView();

    Texts getTexts();
}
