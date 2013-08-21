package dk.dbc.dataio.gui.client.tmpengine;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class EngineEntryPoint implements EntryPoint {

    EngineGUI engineGui = new EngineGUI();
    
    @Override
    public void onModuleLoad() {
        RootLayoutPanel.get().add(engineGui);
    }

}
