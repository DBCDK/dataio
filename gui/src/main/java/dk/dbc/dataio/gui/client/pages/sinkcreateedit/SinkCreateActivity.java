package dk.dbc.dataio.gui.client.pages.sinkcreateedit;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create sink activity encompassing saving
 * of sink data in the flow store via RPC proxy
 */
public class SinkCreateActivity extends AbstractActivity implements SinkCreateEditPresenter {
    private final SinkCreateEditConstants constants = GWT.create(SinkCreateEditConstants.class);

    private ClientFactory clientFactory;
    private SinkCreateEditView sinkCreateView;
    private SinkServiceProxyAsync sinkServiceProxy;
    private FlowStoreProxyAsync flowStoreProxy;

    public SinkCreateActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        sinkServiceProxy = clientFactory.getSinkServiceProxyAsync();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    private void bind() {
        sinkCreateView = clientFactory.getSinkCreateEditView();
        sinkCreateView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(sinkCreateView.asWidget());
        sinkCreateView.initializeFields(constants.menu_SinkCreation(), null);  // A null sink is used to clear the view
        sinkCreateView.setStatusText("");  // Clear status message
    }

    @Override
    public void saveSink(String sinkName, String resourceName) {
        final SinkContent sinkContent = new SinkContent(sinkName, resourceName);
        doPingAndSaveSink(sinkContent);
    }

    public void doPingAndSaveSink(final SinkContent sinkContent) {
        sinkServiceProxy.ping(sinkContent, new FilteredAsyncCallback<PingResponse>() {
            @Override
            public void onFilteredFailure(Throwable caught) {
                sinkCreateView.onFailure(constants.error_PingCommunicationError());
            }
            @Override
            public void onSuccess(PingResponse result) {
                PingResponse.Status status = result.getStatus();
                if (status == PingResponse.Status.OK) {
                    doSaveSink(sinkContent);
                } else {
                    sinkCreateView.onFailure(constants.error_ResourceNameNotValid());
                }
            }
        });
    }

    public void doSaveSink(SinkContent sinkContent) {
        flowStoreProxy.createSink(sinkContent, new FilteredAsyncCallback<Sink>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                sinkCreateView.onFlowStoreProxyFailure(getErrorCode(e), e.getMessage());
            }
            @Override
            public void onSuccess(Sink sink) {
                sinkCreateView.onSaveSinkSuccess();
            }
        });
    }

    private ProxyError getErrorCode(Throwable e) {
        ProxyError errorCode = null;
        if (e instanceof ProxyException) {
            errorCode = ((ProxyException) e).getErrorCode();
        }
        return errorCode;
    }

}
