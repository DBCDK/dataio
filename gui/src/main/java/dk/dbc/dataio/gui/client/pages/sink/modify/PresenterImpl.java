package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.PingResponseModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Abstract Presenter Implementation Class for Sink Create and Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    protected Texts texts;
    protected final ProxyErrorTexts proxyErrorTexts;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected SinkServiceProxyAsync sinkServiceProxy;
    protected View view;

    // Application Models
    protected SinkModel model = new SinkModel();

    public PresenterImpl(ClientFactory clientFactory) {
        texts = clientFactory.getSinkModifyTexts();
        proxyErrorTexts = clientFactory.getProxyErrorTexts();
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
        initializeViewFields();
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
        view.status.setText("");
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    public void saveButtonPressed() {
        if (model.isInputFieldsEmpty()) {
            view.setErrorText(texts.error_InputFieldValidationError());
        } else if (!model.getDataioPatternMatches().isEmpty()) {
            view.setErrorText(texts.error_NameFormatValidationError());
        } else {
            doPingAndSaveSink();
        }
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

    /*
     * Private methods
     */

    public void initializeViewFields() {
        view.name.clearText();
        view.name.setEnabled(false);
        view.resource.clearText();
        view.resource.setEnabled(false);
    }

    private void doPingAndSaveSink() {
        sinkServiceProxy.ping(model, new PingSinkServiceFilteredAsyncCallback());
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        view.name.setText(model.getSinkName());
        view.name.setEnabled(true);
        view.resource.setText(model.getResourceName());
        view.resource.setEnabled(true);
        view.status.setText("");
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
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, null));
        }

        @Override
        public void onSuccess(SinkModel model) {
            view.status.setText(texts.status_SinkSuccessfullySaved());
            setSinkModel(model);
        }
    }

    class PingSinkServiceFilteredAsyncCallback extends FilteredAsyncCallback<PingResponseModel> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_PingCommunicationError());
        }

        @Override
        public void onSuccess(PingResponseModel result) {
            if (result.getStatus() == PingResponseModel.Status.OK) {
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


