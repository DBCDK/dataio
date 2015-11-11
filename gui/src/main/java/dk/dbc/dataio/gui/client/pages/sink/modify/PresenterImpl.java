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
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.PingResponseModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

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
        getView().setHeader(this.header);
        initializeViewFields();
        getView().setPresenter(this);
        containerWidget.setWidget(getView().asWidget());
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
        View view = getView();
        view.sinkTypeSelection.setSelectedValue("ES_SINK_TYPE");  // Default selection is ES Sink
        view.sinkTypeSelection.setEnabled(false);
        view.name.clearText();
        view.name.setEnabled(false);
        view.resource.clearText();
        view.resource.setEnabled(false);
        view.description.clearText();
        view.description.setEnabled(false);
        view.updateSinkSection.setVisible(false);
        view.url.clearText();
        view.url.setEnabled(false);
        view.userid.clearText();
        view.userid.setEnabled(false);
        view.password.clearText();
        view.password.setEnabled(false);
    }

    private void doPingAndSaveSink() {
        viewInjector.getSinkServiceProxyAsync().ping(model, new PingSinkServiceFilteredAsyncCallback());
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        View view = getView();
        view.sinkTypeSelection.setSelectedValue("ES_SINK_TYPE");
        view.sinkTypeSelection.setEnabled(true);
        view.name.setText(model.getSinkName());
        view.name.setEnabled(true);
        view.name.setFocus(true);
        view.resource.setText(model.getResourceName());
        view.resource.setEnabled(true);
        view.description.setText(model.getDescription());
        view.description.setEnabled(true);
        view.updateSinkSection.setVisible(false);
        view.url.clearText();  // To be replaced by actual values
        view.url.setEnabled(true);
        view.userid.clearText();  // To be replaced by actual values
        view.userid.setEnabled(true);
        view.password.clearText();  // To be replaced by actual values
        view.password.setEnabled(true);
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


