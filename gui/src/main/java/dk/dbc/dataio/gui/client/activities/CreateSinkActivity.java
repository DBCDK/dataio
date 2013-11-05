package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyError;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyException;
import dk.dbc.dataio.gui.client.places.SinkCreatePlace;
import dk.dbc.dataio.gui.client.presenters.SinkCreatePresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.SinkCreateView;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create sink activity encompassing saving
 * of sink data in the flow store via RPC proxy
 */
public class CreateSinkActivity extends AbstractActivity implements SinkCreatePresenter {
    public static final String SINK_PING_COMMUNICATION_FAILURE = "Det kunne ikke undersøges, om det pågældende resource navn er en gyldig sink resource";
    public static final String SINK_RESOURCE_NAME_NOT_VALID_ERROR = "Det pågældende resource navn er ikke en gyldig sink resource";
    
    private ClientFactory clientFactory;
    private SinkCreateView sinkCreateView;
    private SinkServiceProxyAsync sinkServiceProxy;
    private FlowStoreProxyAsync flowStoreProxy;
    
    public CreateSinkActivity(SinkCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        sinkServiceProxy = clientFactory.getSinkServiceProxyAsync();
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

    @Override
    public void saveSink(String sinkName, String resourceName) {
        final SinkContent sinkContent = new SinkContent(sinkName, resourceName);
        doPingAndSaveSink(sinkContent);
    }

    public void doPingAndSaveSink(final SinkContent sinkContent) {
        sinkServiceProxy.ping(sinkContent, new AsyncCallback<PingResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                sinkCreateView.onFailure(SINK_PING_COMMUNICATION_FAILURE);
            }
            @Override
            public void onSuccess(PingResponse result) {
                PingResponse.Status status = result.getStatus();
                if (status == PingResponse.Status.OK) {
                    doSaveSink(sinkContent);
                } else {
                    sinkCreateView.onFailure(SINK_RESOURCE_NAME_NOT_VALID_ERROR);
                }
            }
        });
    }

    public void doSaveSink(SinkContent sinkContent) {
        flowStoreProxy.createSink(sinkContent, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
                sinkCreateView.onFlowStoreProxyFailure(getErrorCode(e), e.getMessage());
            }
            @Override
            public void onSuccess(Void aVoid) {
                sinkCreateView.onSaveSinkSuccess();
            }
        });
    }

    private FlowStoreProxyError getErrorCode(Throwable e) {
        FlowStoreProxyError errorCode = null;
        if (e instanceof FlowStoreProxyException) {
            errorCode = ((FlowStoreProxyException) e).getErrorCode();
        }
        return errorCode;
    }
    
}
