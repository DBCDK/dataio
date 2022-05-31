package dk.dbc.dataio.gui.client.pages.harvester.corepo.modify;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * Ginjector for the Harvester Modify module
 */
@GinModules(ViewModule.class)
public interface ViewGinjector extends Ginjector {
    View getView();

    Texts getTexts();
}
