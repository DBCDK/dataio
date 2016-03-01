package dk.dbc.dataio.gui.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface DialogHandler extends EventHandler {
    void onDialogButtonClick(DialogEvent event);
}
