/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
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
     * @param sinkType Sink Type selection
     */
    public void sinkTypeChanged(SinkContent.SinkType sinkType) {
        View view = getView();
        view.updateSinkSection.setVisible(false);
        view.esSinkSection.setVisible(false);
        view.imsSinkSection.setVisible(false);
        handleSinkConfig(sinkType);
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     * @param name, the new name value
     */
    public void nameChanged(String name) {
        model.setSinkName(name);
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     * @param description, the new description value
     */
    @Override
    public void descriptionChanged(String description) {
        model.setDescription(description);
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
     * A signal to the presenter, saying that the User Id field has been changed
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
     * A signal to the presenter, saying that the password field has been changed
     * @param password, the new password value
     */
    @Override
    public void passwordChanged(String password) {
        if(isValid(password)) {
            model.setOpenUpdatePassword(password);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the list of Available Queue Providers has been changed
     * @param availableQueueProviders The list of Available Queue Providers
     */
    @Override
    public void queueProvidersChanged(List<String> availableQueueProviders) {
        if(isValid(availableQueueProviders)) {
            model.setOpenUpdateAvailableQueueProviders(availableQueueProviders);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the sequence analysis selection has changed
     * @param value Holds the value attribute of the selected Radio Button
     */
    @Override
    public void sequenceAnalysisSelectionChanged(String value) {
        if(isValid(value)) {
            model.setSequenceAnalysisOption(SinkContent.SequenceAnalysisOption.valueOf(value));
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the endpoint field has been changed
     * @param endpoint, the new endpoint value
     */
    @Override
    public void endpointChanged(String endpoint) {
        if(isValid(endpoint)) {
            model.setOpenUpdateEndpoint(endpoint);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the es user id field has been changed
     * @param userId, the new es user id value
     */
    @Override
    public void esUserIdChanged(String userId) {
        if(isValid(userId)) {
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
     * @param esDatabase, the new es database name value
     */
    @Override
    public void esDatabaseChanged(String esDatabase) {
        if(isValid(esDatabase)) {
            model.setEsDatabase(esDatabase);
        } else {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        }
    }

    /**
     * A signal to the presenter, saying that the ims endpoint field has been changed
     * @param imsEndpoint, the new ims endpoint value
     */
    @Override
    public void imsEndpointChanged(String imsEndpoint) {
        if(isValid(imsEndpoint)) {
            model.setImsEndpoint(imsEndpoint);
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
        getView().popupTextBox.show();
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

    private void initializeView() {
        getView().setHeader(this.header);
        getView().setPresenter(this);
    }

    public void initializeViewFields() {
        View view = getView();
        view.name.clearText();
        view.name.setEnabled(false);
        view.resource.clearText();
        view.resource.setEnabled(false);
        view.description.clearText();
        view.description.setEnabled(false);
        view.url.clearText();
        view.url.setEnabled(false);
        view.openupdateuserid.clearText();
        view.openupdateuserid.setEnabled(false);
        view.password.clearText();
        view.password.setEnabled(false);
        view.queueProviders.clear();
        view.queueProviders.setEnabled(false);
        view.esUserId.clearText();
        view.esUserId.setEnabled(false);
        view.esDatabase.clearText();
        view.esDatabase.setEnabled(false);
        view.imsEndpoint.clearText();
        view.imsEndpoint.setEnabled(false);
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
        view.resource.setText(model.getResourceName());
        view.resource.setEnabled(true);
        view.description.setText(model.getDescription());
        view.description.setEnabled(true);
        view.url.setEnabled(true);
        view.openupdateuserid.setEnabled(true);
        view.password.setEnabled(true);
        view.queueProviders.setEnabled(true);
        view.esUserId.setEnabled(true);
        view.esDatabase.setEnabled(true);
        view.imsEndpoint.setEnabled(true);
        view.status.setText("");
        view.sequenceAnalysisSelection.setValue(model.getSequenceAnalysisOption().toString());
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
     * @param sinkType defining the type of sinkConfig
     */
    abstract void handleSinkConfig(SinkContent.SinkType sinkType);

}


