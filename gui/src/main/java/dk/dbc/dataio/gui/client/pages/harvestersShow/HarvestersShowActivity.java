package dk.dbc.dataio.gui.client.pages.harvestersShow;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sma on 25/04/14.
 */
public class HarvestersShowActivity extends AbstractActivity implements HarvestersShowPresenter {

    private final ClientFactory clientFactory;
    private HarvestersShowView harvestersShowView;
    private final FlowStoreProxyAsync flowStoreProxy;
    private final PlaceController placeController;

    public HarvestersShowActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
    }

    //TODO Denne skal ikke være udkommenteret når tilføjet til ClientFactory
    @Override
    public void bind() {
//        harvestersShowView = clientFactory.getHarvestersShowView();
//        harvestersShowView.setPresenter(this);
    }

    @Override
    public void reload() {
        harvestersShowView.refresh();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(harvestersShowView.asWidget());
        fetchHarvesters();
    }

    // Local methods

    // TODO the "dummy harvester" needs to be replaced with a real object.
    private void fetchHarvesters() {

        List<String> dummyListForHarvesters = new ArrayList<String>();
        dummyListForHarvesters.add("DummyHarvester");
        harvestersShowView.setHarvesters(dummyListForHarvesters);
    }
}
