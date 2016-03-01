package dk.dbc.dataio.gui.client.events;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public interface HasDialogHandlers extends HasHandlers {
    HandlerRegistration addDialogHandler(DialogHandler handler);
}
