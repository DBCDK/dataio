package dk.dbc.dataio.gui.client.pages.sink.sinkmodify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Abstract Presenter Implementation Class for Sink Create and Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    protected SinkModifyConstants constants;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected SinkServiceProxyAsync sinkServiceProxy;
    protected View view;
    protected SinkModel model;

    private final static String EMPTY = "";

    public PresenterImpl(ClientFactory clientFactory, SinkModifyConstants constants) {
        this.constants = constants;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        sinkServiceProxy = clientFactory.getSinkServiceProxyAsync();
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     * @param name, the new name value
     */
    public void nameChanged(String name) {
        model.setSinkName(name);
    }

    /**
     * A signal to the presenter, saying that the resource field has been changed
     * @param resource, the new resource value
     */
    @Override
    public void resourceChanged(String resource) {
        model.setResourceName(resource);
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    public void keyPressed() {
        view.setStatusText(EMPTY);
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    public void saveButtonPressed() {
        doPingAndSaveSink();
    }

    /*
     * Protected methods
     */

    /**
     * A local method to be used to translate an exception to a readable text
     * @param e Exception
     * @return errorMessage, the error text
     */
    protected String getErrorText(Throwable e) {
        ProxyError errorCode = null;
        if (e instanceof ProxyException) {
            errorCode = ((ProxyException) e).getErrorCode();
        }
        final String errorMessage;
        if (errorCode == null) {
            errorMessage = e.getMessage();
        } else {
            switch (errorCode) {
                case NOT_ACCEPTABLE: errorMessage = constants.error_ProxyKeyViolationError();
                    break;
                case BAD_REQUEST: errorMessage = constants.error_ProxyDataValidationError();
                    break;
                default: errorMessage = e.getMessage();
                    break;
            }
        }
        return errorMessage;
    }

    /*
     * Private methods
     */

    private void doPingAndSaveSink() {
        sinkServiceProxy.ping(model, new PingSinkServiceFilteredAsyncCallback());
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        view.setName(model.getSinkName());
        view.setResource(model.getResourceName());
        view.setStatusText(EMPTY);
    }

    /*
     * Local classes
     */

    /**
     * Local call back class to be instantiated in the call to createSubmitter or updateSubmitter in flowstore proxy
     */
    class SaveSinkModelFilteredAsyncCallback extends FilteredAsyncCallback<SinkModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(getErrorText(e));
        }

        @Override
        public void onSuccess(SinkModel model) {
            view.setStatusText(constants.status_SinkSuccessfullySaved());
        }
    }

    class PingSinkServiceFilteredAsyncCallback extends FilteredAsyncCallback<PingResponse> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(constants.error_PingCommunicationError());
        }

        @Override
        public void onSuccess(PingResponse result) {
            PingResponse.Status status = result.getStatus();
            if (status == PingResponse.Status.OK) {
                saveModel();
            } else {
                view.setErrorText(constants.error_ResourceNameNotValid());
            }
        }
    }

     /*
     * Abstract methods
     */

    /**
     * getModel
     */
    abstract void initializeModel();

    /**
     * saveModel
     */
    abstract void saveModel();

}


