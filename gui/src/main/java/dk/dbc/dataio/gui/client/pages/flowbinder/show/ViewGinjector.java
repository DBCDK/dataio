package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * Ginjector for Flowbinder Show
 */
@GinModules(ViewModule.class)
public interface ViewGinjector extends Ginjector {
    View getView();

    Texts getTexts();
}
