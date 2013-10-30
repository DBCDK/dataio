package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.places.SinkCreatePlace;
import dk.dbc.dataio.gui.client.presenters.SinkCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.SinkCreateView;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create sink activity encompassing saving
 * of sink data in the flow store via RPC proxy
 */
public class CreateSinkActivity extends AbstractActivity implements SinkCreatePresenter {
    private ClientFactory clientFactory;
    private SinkCreateView sinkCreateView;
    private FlowStoreProxyAsync flowStoreProxy;

    
    public CreateSinkActivity(SinkCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        sinkCreateView = clientFactory.getSinkCreateView();
        sinkCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
		sinkCreateView.refresh();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(sinkCreateView.asWidget());
    }

    public void saveSink(String sinkName, String resourceName) {
        sinkCreateView.onSaveSinkSuccess();
    }

}
