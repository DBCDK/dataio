/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.presenters;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.FlowData;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.views.FlowEditView;
import dk.dbc.dataio.gui.views.FlowEditViewImpl;

/**
 *
 * @author slf
 */
public class FlowEditActivity implements FlowEditView.Presenter {
    final FlowEditView view;
    private FlowStoreProxyAsync flowStoreProxy = FlowStoreProxy.Factory.getAsyncInstance();

    public FlowEditActivity(FlowEditView flowEditView) {
        view = flowEditView;
        view.setPresenter(this);
    }

    @Override
    public void reload() {
		view.refresh();
    }

    @Override
    public void saveFlow(String name, String description) {
                FlowData flowData = new FlowData();
                flowData.setFlowname(name);
                flowData.setDescription(description);
                flowStoreProxy.createFlow(flowData, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable e) {
                        String errorClassName = e.getClass().getName();
                        view.displayError(errorClassName + " - " + e.getMessage());
                    }

                    @Override
                    public void onSuccess(Void aVoid) {
                        view.displaySuccess(FlowEditViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
                    }
                });
    }
    
}
