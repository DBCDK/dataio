package dk.dbc.dataio.gui.client.pages.sinksshow;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkEditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.List;


/**
 * This class represents the show sinks activity
 */
public class SinksShowActivity extends AbstractActivity implements SinksShowPresenter {
    private final ClientFactory clientFactory;
    private SinksShowView sinksShowView;
    private final FlowStoreProxyAsync flowStoreProxy;
    private final PlaceController placeController;

    public SinksShowActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
    }

    @Override
    public void bind() {
        sinksShowView = clientFactory.getSinksShowView();
        sinksShowView.setPresenter(this);
    }

    @Override
    public void reload() {
		sinksShowView.refresh();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(sinksShowView.asWidget());
        fetchSinks();
    }

    /**
     * Creates a new place
     * @param sink The sink to edit
     */
    @Override
    public void editSink(Sink sink) {
        placeController.goTo(new SinkEditPlace(sink));
    }


    // Local methods

    private void fetchSinks() {
        flowStoreProxy.findAllSinks(new FilteredAsyncCallback<List<Sink>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                sinksShowView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<Sink> result) {
                sinksShowView.setSinks(result);
            }
        });
    }
}
