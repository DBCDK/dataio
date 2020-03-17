/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.events;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public interface HasSubmitterFilterPanelHandlers extends HasHandlers {
    HandlerRegistration addSubmitterFilterPanelHandler(SubmitterFilterPanelHandler handler);
}
