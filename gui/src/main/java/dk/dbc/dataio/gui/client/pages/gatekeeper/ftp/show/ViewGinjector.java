package dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

@GinModules(ViewModule.class)
public interface ViewGinjector extends Ginjector {
    View getView();
    Texts getTexts();
}
