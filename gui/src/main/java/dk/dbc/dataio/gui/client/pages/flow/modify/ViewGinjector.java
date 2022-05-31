package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * Created by ThomasBerg on 09/11/15.
 */
@GinModules(ViewModule.class)
public interface ViewGinjector extends Ginjector {
    ViewWidget getView();

    Texts getTexts();
}
