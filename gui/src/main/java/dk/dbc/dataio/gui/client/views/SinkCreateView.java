/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.presenters.SinkCreatePresenter;

/**
 *
 * @author slf
 */
public interface SinkCreateView extends IsWidget, View<SinkCreatePresenter> {
    void onFlowStoreProxyFailure(ProxyError errorCode, String detail);
    void onSaveSinkSuccess();
}
