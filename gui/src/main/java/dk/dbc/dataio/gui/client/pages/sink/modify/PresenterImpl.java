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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.PingResponseModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.List;

/**
 * Abstract Presenter Implementation Class for Sink Create and Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    private final String DEFAULT_SINK_TYPE = "ES";

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
    public void sinkTypeChanged(String sinkType) {
        model.setSinkType(SinkContent.SinkType.valueOf(sinkType));
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
    public void userIdChanged(String userId) {
        model.setOpenUpdateUserId(userId);
    }

    /**
     * A signal to the presenter, saying that the password field has been changed
     * @param password, the new password value
     */
    @Override
    public void passwordChanged(String password) {
        model.setOpenUpdatePassword(password);
    }

    /**
     * A signal to the presenter, saying that the list of Available Queue Providers has been changed
     * @param availableQueueProviders The list of Available Queue Providers
     */
    @Override
    public void queueProvidersChanged(List<String> availableQueueProviders) {
        model.setOpenUpdateAvailableQueueProviders(availableQueueProviders);
    }

    /**
     * A signal to the presenter, saying that the endpoint field has been changed
     * @param endpoint, the new endpoint value
     */
    @Override
    public void endpointChanged(String endpoint) {
        model.setOpenUpdateEndpoint(endpoint);
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
            doPingAndSaveSink();
        }
    }

    /**
     * A signal to the presenter, saying that the add button on the Available Queue Providers list has been pressed
     */
    @Override
    public void queueProvidersAddButtonPressed() {
        Window.alert("Add Available Queue Provider");
        View vie = getView();
        setQueueProvidersMultiList(vie.queueProviders, model.getOpenUpdateAvailableQueueProviders());
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
        view.sinkTypeSelection.setSelectedValue(DEFAULT_SINK_TYPE);  // Default selection is ES Sink
        view.sinkTypeSelection.setEnabled(false);
        view.name.clearText();
        view.name.setEnabled(false);
        view.resource.clearText();
        view.resource.setEnabled(false);
        view.description.clearText();
        view.description.setEnabled(false);
        view.url.clearText();
        view.url.setEnabled(false);
        view.userid.clearText();
        view.userid.setEnabled(false);
        view.password.clearText();
        view.password.setEnabled(false);
        view.queueProviders.clear();
        view.queueProviders.setEnabled(false);
        view.sinkTypeSelection.fireChangeEvent(); // Assure, that Config fields are shown correctly
    }

    private void doPingAndSaveSink() {
        viewInjector.getSinkServiceProxyAsync().ping(model, new PingSinkServiceFilteredAsyncCallback());
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        View view = getView();
        view.sinkTypeSelection.setSelectedValue(model.getSinkType() != null ? model.getSinkType().name() : DEFAULT_SINK_TYPE);
        view.sinkTypeSelection.setEnabled(true);
        view.name.setText(model.getSinkName());
        view.name.setEnabled(true);
        view.name.setFocus(true);
        view.resource.setText(model.getResourceName());
        view.resource.setEnabled(true);
        view.description.setText(model.getDescription());
        view.description.setEnabled(true);
        view.url.setText(model.getOpenUpdateEndpoint());
        view.url.setEnabled(true);
        view.userid.setText(model.getOpenUpdateUserId());
        view.userid.setEnabled(true);
        view.password.setText(model.getOpenUpdatePassword());
        view.password.setEnabled(true);
        setQueueProvidersMultiList(view.queueProviders, model.getOpenUpdateAvailableQueueProviders());
        view.queueProviders.setEnabled(true);
        view.status.setText("");
        view.sinkTypeSelection.fireChangeEvent(); // Assure, that Config fields are shown correctly
    }


    /*
     * Private methods
     */

    private void setQueueProvidersMultiList(PromptedMultiList queueProviders, List<String> modelProviders) {
        queueProviders.clear();
        if (modelProviders != null) {
            for (String value: modelProviders) {
                queueProviders.addValue(value, value);
            }
        }
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

    class PingSinkServiceFilteredAsyncCallback extends FilteredAsyncCallback<PingResponseModel> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            getView().setErrorText(getTexts().error_PingCommunicationError());
        }

        @Override
        public void onSuccess(PingResponseModel result) {
            if (result.getStatus() == PingResponseModel.Status.OK) {
                saveModel();
            } else {
                getView().setErrorText(getTexts().error_ResourceNameNotValid());
            }
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

}


