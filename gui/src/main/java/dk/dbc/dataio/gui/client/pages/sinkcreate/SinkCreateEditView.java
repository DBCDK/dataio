/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.pages.sinkcreate;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.views.View;

/**
 *
 * @author slf
 */
public interface SinkCreateEditView extends IsWidget, View<SinkCreateEditPresenter> {
    void initializeFields(String header, Sink sink);
    void onFlowStoreProxyFailure(ProxyError errorCode, String detail);
    void onSaveSinkSuccess();
}
