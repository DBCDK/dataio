package dk.dbc.dataio.gui.client.pages.sink.modify;

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
    protected Texts texts;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected SinkServiceProxyAsync sinkServiceProxy;
    protected View view;

    // Application Models
    protected SinkModel model = new SinkModel();

    private final static String EMPTY = "";

    public PresenterImpl(ClientFactory clientFactory, Texts texts) {
        this.texts = texts;
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
        view.initializeFields();
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
     * Method used to set the model after a successful update or a save
     * @param model The model to save
     */
    protected void setSinkModel(SinkModel model) {
        this.model = model;
    }

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
                case NOT_ACCEPTABLE: errorMessage = texts.error_ProxyKeyViolationError();
                    break;
                case BAD_REQUEST: errorMessage = texts.error_ProxyDataValidationError();
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
            view.setStatusText(texts.status_SinkSuccessfullySaved());
            setSinkModel(model);
        }
    }

    class PingSinkServiceFilteredAsyncCallback extends FilteredAsyncCallback<PingResponse> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_PingCommunicationError());
        }

        @Override
        public void onSuccess(PingResponse result) {
            PingResponse.Status status = result.getStatus();
            if (status == PingResponse.Status.OK) {
                saveModel();
            } else {
                view.setErrorText(texts.error_ResourceNameNotValid());
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


