package dk.dbc.dataio.gui.client.pages.sinkcreateedit;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
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
 * This class represents the edit sinkId activity encompassing saving
 * of sinkId data in the flow store via RPC proxy
 */
public class SinkEditActivity extends AbstractActivity implements SinkCreateEditPresenter {
    private final SinkCreateEditConstants constants = GWT.create(SinkCreateEditConstants.class);

    private ClientFactory clientFactory;
    private SinkCreateEditView sinkEditView;
    private SinkServiceProxyAsync sinkServiceProxy;
    private FlowStoreProxyAsync flowStoreProxy;

    private Long sinkId;
    private Sink sink;


    public SinkEditActivity(Place place, ClientFactory clientFactory) {
        SinkEditPlace sinkEditPlace = (SinkEditPlace) place;
        sinkId = sinkEditPlace.getSinkId();
        this.clientFactory = clientFactory;
        sinkServiceProxy = clientFactory.getSinkServiceProxyAsync();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    private void bind() {
        sinkEditView = clientFactory.getSinkCreateEditView();
        sinkEditView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(sinkEditView.asWidget());
        getSink(sinkId);
        sinkEditView.setStatusText("");  // Clear status message
    }

    @Override
    public void saveSink(String sinkName, String resourceName) {
        final Sink updatedSink = new Sink(sink.getId(), sink.getVersion(), new SinkContent(sinkName, resourceName));
        doPingAndUpdateSink(updatedSink);
    }

    public void getSink(final Long sinkId) {
        flowStoreProxy.getSink(sinkId, new FilteredAsyncCallback<Sink>() {
            @Override
            public void onFilteredFailure(Throwable caught) {
                sinkEditView.setErrorText(constants.error_CannotFetchSink());
            }
            @Override
            public void onSuccess(Sink sink) {
                setSink(sink);
                sinkEditView.initializeFields(constants.menu_SinkEdit(), sink);
            }
        });
    }

    public void doPingAndUpdateSink(final Sink sink) {
        sinkServiceProxy.ping(sink.getContent(), new FilteredAsyncCallback<PingResponse>() {
            @Override
            public void onFilteredFailure(Throwable caught) {
                sinkEditView.setErrorText(constants.error_PingCommunicationError());
            }

            @Override
            public void onSuccess(PingResponse result) {
                PingResponse.Status status = result.getStatus();
                if (status == PingResponse.Status.OK) {
                    doUpdateSink(sink.getContent());
                } else {
                    sinkEditView.setErrorText(constants.error_ResourceNameNotValid());
                }
            }
        });
    }

    public void doUpdateSink(SinkContent sinkContent) {
        flowStoreProxy.updateSink(sinkContent, sink.getId(), sink.getVersion(), new FilteredAsyncCallback<Sink>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                sinkEditView.onFlowStoreProxyFailure(getErrorCode(e), e.getMessage());
            }
            @Override
            public void onSuccess(Sink sink) {
                sinkEditView.onSaveSinkSuccess();
                setSink(sink);
                sinkEditView.initializeFields(constants.menu_SinkEdit(), sink);
            }
        });
    }

    private void setSink(Sink sink) {
        this.sink = sink;
    }

    private ProxyError getErrorCode(Throwable e) {
        ProxyError errorCode = null;
        if (e instanceof ProxyException) {
            errorCode = ((ProxyException) e).getErrorCode();
        }
        return errorCode;
    }

}
