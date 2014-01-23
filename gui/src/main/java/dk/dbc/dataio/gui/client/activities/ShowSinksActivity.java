package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.presenters.SinksShowPresenter;
import dk.dbc.dataio.gui.client.views.SinksShowView;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.ArrayList;
import java.util.List;


/**
 * This class represents the show sinks activity
 */
public class ShowSinksActivity extends AbstractActivity implements SinksShowPresenter {
    private ClientFactory clientFactory;
    private SinksShowView sinksShowView;
//    private SinkServiceProxyAsync sinkServiceProxy;

    public ShowSinksActivity(/*SinksShowPlace place,*/ ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
//        this.sinkServiceProxy = clientFactory.getSinkServiceProxyAsync();
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


    // Local methods
    private void fetchSinks() {
        List<SinkContent> sinks = new ArrayList<SinkContent>();
        sinks.add(new SinkContent("Sink Name 1", "Sink Resource et"));
        sinks.add(new SinkContent("Sink Name 2", "Sink Resource to"));
        sinks.add(new SinkContent("Sink Name III", "Sink Resource three"));
        sinksShowView.setSinks(sinks);
    }

}
