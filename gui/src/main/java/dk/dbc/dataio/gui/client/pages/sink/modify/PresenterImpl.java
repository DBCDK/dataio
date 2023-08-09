package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract Presenter Implementation Class for Sink Create and Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;

    // Application Models
    protected SinkModel model = new SinkModel();

    public PresenterImpl(String header) {
        this.header = header;
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        initializeView();
        initializeViewFields();
        containerWidget.setWidget(getView().asWidget());
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the selection of Sink Type has changed
     *
     * @param sinkType Sink Type selection
     */
    public void sinkTypeChanged(SinkContent.SinkType sinkType) {
        View view = getView();
        view.sequenceAnalysisSection.setVisible(true);
        view.updateSinkSection.setVisible(false);
        view.esSinkSection.setVisible(false);
        view.imsSinkSection.setVisible(false);
        view.worldCatSinkSection.setVisible(false);
        view.vipSinkSection.setVisible(false);
        view.dpfSinkSection.setVisible(false);
        handleSinkConfig(sinkType);
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new name value
     */
    public void nameChanged(String name) {
        model.setSinkName(name);
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new description value
     */
    @Override
    public void descriptionChanged(String description) {
        model.setDescription(description);
    }

    @Override
    public void timeoutChanged(String timeout) {
        model.setTimeout(Integer.parseInt(timeout));
    }

    @Override
    public void queueChanged(String name) {
        if(name.contains("::")) {
            model.setQueue(name);
        } else {
            getView().setErrorText(getTexts().error_QueueNameValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the User Id field has been changed
     *
     * @param userId, the new User Id value
     */
    @Override
    public void openUpdateUserIdChanged(String userId) {
        if (isValid(userId)) {
            model.setOpenUpdateUserId(userId);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the openupdatepassword field has been changed
     *
     * @param password, the new openupdatepassword value
     */
    @Override
    public void passwordChanged(String password) {
        if (isValid(password)) {
            model.setOpenUpdatePassword(password);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the list of Available Queue Providers has been changed
     *
     * @param availableQueueProviders The list of Available Queue Providers
     */
    @Override
    public void queueProvidersChanged(List<String> availableQueueProviders) {
        if (isValid(availableQueueProviders)) {
            model.setOpenUpdateAvailableQueueProviders(availableQueueProviders);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    @Override
    public void updateServiceIgnoredValidationErrorsChanged(Set<String> errors) {
        model.setUpdateServiceIgnoredValidationErrors(errors);
    }

    @Override
    public void updateServiceIgnoredValidationErrorsAddButtonPressed() {
        getView().updateServiceIgnoredValidationErrorsPopupTextBox.show();
    }

    public void updateServiceIgnoredValidationErrorsRemoveButtonPressed(String error) {
        final Map<String, String> errors = getView().updateServiceIgnoredValidationErrors.getValue();
        errors.remove(error);
        getView().updateServiceIgnoredValidationErrors.setValue(errors);
        updateAllFieldsAccordingToCurrentState();
    }

    @Override
    public void dpfUpdateServiceUserIdChanged(String userId) {
        if (isValid(userId)) {
            model.setDpfUpdateServiceUserId(userId);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    @Override
    public void dpfUpdateServicePasswordChanged(String password) {
        if (isValid(password)) {
            model.setDpfUpdateServicePassword(password);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    @Override
    public void dpfUpdateServiceQueueProvidersChanged(List<String> availableQueueProviders) {
        if (isValid(availableQueueProviders)) {
            model.setDpfUpdateServiceAvailableQueueProviders(availableQueueProviders);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the sequence analysis selection has changed
     *
     * @param value Holds the value attribute of the selected Radio Button
     */
    @Override
    public void sequenceAnalysisSelectionChanged(String value) {
        if (isValid(value)) {
            model.setSequenceAnalysisOption(SinkContent.SequenceAnalysisOption.valueOf(value));
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the endpoint field has been changed
     *
     * @param endpoint, the new endpoint value
     */
    @Override
    public void endpointChanged(String endpoint) {
        if (isValid(endpoint)) {
            model.setOpenUpdateEndpoint(endpoint);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the es user id field has been changed
     *
     * @param userId, the new es user id value
     */
    @Override
    public void esUserIdChanged(String userId) {
        if (isValid(userId)) {
            try {
                model.setEsUserId(Integer.valueOf(userId));
            } catch (NumberFormatException e) {
                getView().setErrorText(getTexts().error_NumericEsUserValidationError());
            }
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the es database field has been changed
     *
     * @param esDatabase, the new es database name value
     */
    @Override
    public void esDatabaseChanged(String esDatabase) {
        if (isValid(esDatabase)) {
            model.setEsDatabase(esDatabase);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the ims endpoint field has been changed
     *
     * @param imsEndpoint, the new ims endpoint value
     */
    @Override
    public void imsEndpointChanged(String imsEndpoint) {
        if (isValid(imsEndpoint)) {
            model.setImsEndpoint(imsEndpoint);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the worldcat user id field has been changed
     *
     * @param userId, the new worldcat user id value
     */
    @Override
    public void worldCatUserIdChanged(String userId) {
        if (isValid(userId)) {
            model.setWordCatUserId(userId);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the worldcat password field has been changed
     *
     * @param password, the new worldcat password value
     */
    @Override
    public void worldCatPasswordChanged(String password) {
        if (isValid(password)) {
            model.setWordCatPassword(password);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the worldCat project id field has been changed
     *
     * @param projectId, the new worldCat project id value
     */
    @Override
    public void worldCatProjectIdChanged(String projectId) {
        if (isValid(projectId)) {
            model.setWordCatProjectId(projectId);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the worldCat endpoint field has been changed
     *
     * @param endpoint, the new worldCat endpoint value
     */
    @Override
    public void worldCatEndpointChanged(String endpoint) {
        if (isValid(endpoint)) {
            model.setWorldCatEndpoint(endpoint);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the worldCat list of retry diagnostics has been changed
     *
     * @param diagnostics, the new worldCat list of retry diagnostics
     */
    @Override
    public void worldCatRetryDiagnosticsChanged(List<String> diagnostics) {
        if (isValid(diagnostics)) {
            model.setWorldCatRetryDiagnostics(diagnostics);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    @Override
    public void vipEndpointChanged(String vipEndpoint) {
        if (isValid(vipEndpoint)) {
            model.setVipEndpoint(vipEndpoint);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    public void keyPressed() {
        getView().status.setText("");
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    public void saveButtonPressed() {
        if (model.isInputFieldsEmpty()) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        } else if (!model.getDataioPatternMatches().isEmpty()) {
            getView().setErrorText(getTexts().error_NameFormatValidationError());
        } else {
            saveModel();
        }
    }

    /**
     * A signal to the presenter, saying that the add button on the Available Queue Providers list has been pressed
     */
    @Override
    public void queueProvidersAddButtonPressed() {
        getView().queueProvidersPopupTextBox.show();
    }

    @Override
    public void dpfUpdateServiceQueueProvidersAddButtonPressed() {
        getView().queueProvidersPopupTextBox.show();
    }


    @Override
    public void worldCatRetryDiagnosticsAddButtonPressed() {
        getView().worldCatPopupTextBox.show();
    }


    /**
     * Removes a diagnostic from the list of retry diagnostics
     *
     * @param retryDiagnostic The diagnostic to remove from the list of retry diagnostics
     */
    @Override
    public void worldCatRetryDiagnosticRemoveButtonPressed(String retryDiagnostic) {
        final Map<String, String> retryDiagnostics = getView().worldCatRetryDiagnostics.getValue();
        retryDiagnostics.remove(retryDiagnostic);
        getView().worldCatRetryDiagnostics.setValue(retryDiagnostics);
        updateAllFieldsAccordingToCurrentState();
    }

    /*
     * Protected methods
     */

    /**
     * Method used to set the model after a successful update or a save
     *
     * @param model The model to save
     */
    protected void setSinkModel(SinkModel model) {
        this.model = model;
    }

    /*
     * Private methods
     */

    private void initializeView() {
        getView().setHeader(this.header);
        getView().setPresenter(this);
    }

    private void initializeViewFields() {
        View view = getView();
        view.name.clearText();
        view.name.setEnabled(false);
        view.queue.clearText();
        view.queue.setEnabled(false);
        view.description.clearText();
        view.description.setEnabled(false);
        view.timeout.clearText();
        view.timeout.setEnabled(false);
        view.url.clearText();
        view.url.setEnabled(false);
        view.openupdateuserid.clearText();
        view.openupdateuserid.setEnabled(false);
        view.openupdatepassword.clearText();
        view.openupdatepassword.setEnabled(false);
        view.queueProviders.clear();
        view.queueProviders.setEnabled(false);
        view.updateServiceIgnoredValidationErrors.clear();
        view.updateServiceIgnoredValidationErrors.setEnabled(false);
        view.dpfUpdateServiceUserId.clearText();
        view.dpfUpdateServiceUserId.setEnabled(false);
        view.dpfUpdateServicePassword.clearText();
        view.dpfUpdateServicePassword.setEnabled(false);
        view.dpfUpdateServiceQueueProviders.clear();
        view.dpfUpdateServiceQueueProviders.setEnabled(false);
        view.esUserId.clearText();
        view.esUserId.setEnabled(false);
        view.esDatabase.clearText();
        view.esDatabase.setEnabled(false);
        view.imsEndpoint.clearText();
        view.imsEndpoint.setEnabled(false);
        view.worldCatUserId.clearText();
        view.worldCatUserId.setEnabled(false);
        view.worldCatPassword.clearText();
        view.worldCatPassword.setEnabled(false);
        view.worldCatProjectId.clearText();
        view.worldCatProjectId.setEnabled(false);
        view.worldCatEndpoint.clearText();
        view.worldCatEndpoint.setEnabled(false);
        view.worldCatRetryDiagnostics.clear();
        view.worldCatRetryDiagnostics.setEnabled(false);
        view.vipEndpoint.clearText();
        view.vipEndpoint.setEnabled(false);
    }

    private boolean isValid(String input) {
        return input != null && !input.trim().isEmpty();
    }

    private boolean isValid(List<String> input) {
        return input != null && !input.isEmpty();
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        View view = getView();
        final SinkContent.SinkType sinkType = model.getSinkType();
        view.sinkTypeSelection.setSelectedValue(sinkType.name());
        view.name.setText(model.getSinkName());
        view.name.setEnabled(true);
        view.name.setFocus(true);
        view.queue.setEnabled(true);
        view.queue.setText(model.getQueue());
        view.description.setText(model.getDescription());
        view.description.setEnabled(true);
        view.timeout.setText(Integer.toString(model.getTimeout()));
        view.timeout.setEnabled(true);
        view.url.setEnabled(true);
        view.openupdateuserid.setEnabled(true);
        view.openupdatepassword.setEnabled(true);
        view.queueProviders.setEnabled(true);
        view.updateServiceIgnoredValidationErrors.setEnabled(true);
        view.dpfUpdateServiceUserId.setEnabled(true);
        view.dpfUpdateServicePassword.setEnabled(true);
        view.dpfUpdateServiceQueueProviders.setEnabled(true);
        view.esUserId.setEnabled(true);
        view.esDatabase.setEnabled(true);
        view.imsEndpoint.setEnabled(true);
        view.status.setText("");
        view.sequenceAnalysisSelection.setValue(model.getSequenceAnalysisOption().toString());
        view.worldCatUserId.setEnabled(true);
        view.worldCatPassword.setEnabled(true);
        view.worldCatProjectId.setEnabled(true);
        view.worldCatEndpoint.setEnabled(true);
        view.worldCatRetryDiagnostics.setEnabled(true);
        view.vipEndpoint.setEnabled(true);
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
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(SinkModel model) {
            getView().status.setText(getTexts().status_SinkSuccessfullySaved());
            setSinkModel(model);
            History.back();
        }
    }

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
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

    /**
     * handleSinkConfig
     *
     * @param sinkType defining the type of sinkConfig
     */
    abstract void handleSinkConfig(SinkContent.SinkType sinkType);

}


