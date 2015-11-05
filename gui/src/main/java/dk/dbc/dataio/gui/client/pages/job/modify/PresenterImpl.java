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

package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Abstract Presenter Implementation Class for Submitter Create and Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    protected final Texts texts;
    protected final FlowStoreProxyAsync flowStoreProxy;
    protected final ProxyErrorTexts proxyErrorTexts;
    protected View view;

    // Application Models
    protected JobModel model = new JobModel();


    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method
     *
     * @param clientFactory, clientFactory
     */
    public PresenterImpl(ClientFactory clientFactory) {
        texts = clientFactory.getJobModifyTexts();
        proxyErrorTexts = clientFactory.getProxyErrorTexts();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
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
     * A signal to the presenter, saying that the number field has been changed
     * @param number, the new number value
     */
    public void numberChanged(String number) {
//        model.setNumber(number);
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     * @param name, the new name value
     */
    public void nameChanged(String name) {
//        model.setName(name);
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     * @param description, the new description value
     */
    public void descriptionChanged(String description) {
//        model.setDescription(description);
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    public void keyPressed() {
//        view.status.setText("");
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    public void saveButtonPressed() {
        /*
        if (model != null) {
            if (model.isInputFieldsEmpty()) {
                view.setErrorText(texts.error_InputFieldValidationError());
            } else if (!model.isNumberValid()) {
               view.setErrorText(texts.error_NumberInputFieldValidationError());
            } else if (!model.getDataioPatternMatches().isEmpty()) {
                view.setErrorText(texts.error_NameFormatValidationError());
            } else {
                saveModel();
            }
        }*/
    }

    /*
     * Private methods
     */

    public void initializeViewFields() {
//        view.number.clearText();
//        view.number.setEnabled(false);
//        view.name.clearText();
//        view.name.setEnabled(false);
//        view.description.clearText();
//        view.description.setEnabled(false);
    }
    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
//        if(model.getId() == 0) {
//            view.number.setEnabled(true);
//        }
//        view.number.setText(model.getNumber());
//        view.name.setText(model.getName());
//        view.name.setEnabled(true);
//        view.description.setText(model.getDescription());
//        view.description.setEnabled(true);
//        view.status.setText("");
//        if (view.number.isEnabled()) {
//            view.number.setFocus(true);
//        } else if (view.name.isEnabled()) {
//            view.name.setFocus(true);
//        }
    }


    /*
     * Protected methods
     */

    /**
     * Method used to set the model after a successful update or a save
     * @param model The model to save
     */
    protected void setSubmitterModel(JobModel model) {
        this.model = model;
    }

    /*
     * Local class
     */

    /**
     * Local call back class to be instantiated in the call to createSubmitter or updateSubmitter in flowstore proxy
     */
    /*
    class SaveSubmitterModelFilteredAsyncCallback extends FilteredAsyncCallback<SubmitterModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, null));
        }

        @Override
        public void onSuccess(SubmitterModel model) {
            view.status.setText(texts.status_SubmitterSuccessfullySaved());
            setSubmitterModel(model);
            History.back();
        }
    }*/


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
