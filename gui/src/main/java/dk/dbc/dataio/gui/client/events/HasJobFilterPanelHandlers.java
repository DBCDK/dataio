package dk.dbc.dataio.gui.client.events;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public interface HasJobFilterPanelHandlers extends HasHandlers {
    HandlerRegistration addJobFilterPanelHandler(JobFilterPanelHandler handler);
}
