/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.FlowData;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.views.FlowEditView;
import dk.dbc.dataio.gui.views.FlowEditViewImpl;
import dk.dbc.dataio.gui.places.FlowEditPlace;

/**
 *
 * @author slf
 */
public class FlowEditActivity extends AbstractActivity implements FlowEditView.Presenter {
    FlowEditPlace place;
    ClientFactory clientFactory;
    private FlowStoreProxyAsync flowStoreProxy = FlowStoreProxy.Factory.getAsyncInstance();

    public FlowEditActivity(FlowEditPlace place, ClientFactory clientFactory) {
        this.place = place;
        this.clientFactory = clientFactory;
        this.clientFactory.getFlowEditView().setPresenter(this);
    }

    @Override
    public void reload() {
		this.clientFactory.getFlowEditView().refresh();
    }

    @Override
    public void saveFlow(String name, String description) {
        final FlowEditView view = this.clientFactory.getFlowEditView();

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

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        FlowEditView flowEditView = clientFactory.getFlowEditView();
        flowEditView.setPresenter(this);
        containerWidget.setWidget(flowEditView.asWidget());
    }
    
}
