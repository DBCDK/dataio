/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.FlowData;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.client.views.FlowCreateView;
import dk.dbc.dataio.gui.client.views.FlowCreateViewImpl;

/**
 *
 * @author slf
 */
public class CreateFlowActivity extends AbstractActivity implements FlowCreateView.Presenter {
    ClientFactory clientFactory;
    private FlowStoreProxyAsync flowStoreProxy = FlowStoreProxy.Factory.getAsyncInstance();

    public CreateFlowActivity(FlowCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public void reload() {
		this.clientFactory.getFlowCreateView().refresh();
    }

    @Override
    public void saveFlow(String name, String description) {
        final FlowCreateView view = this.clientFactory.getFlowCreateView();

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
                view.displaySuccess(FlowCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
            }
        });
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        FlowCreateView flowCreateView = clientFactory.getFlowCreateView();
        flowCreateView.setPresenter(this);
        containerWidget.setWidget(flowCreateView.asWidget());
    }
    
//    public void goTo(Place place) {
//        clientFactory.getPlaceController().goTo(place);
//    }
}
