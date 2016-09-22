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

package dk.dbc.dataio.gui.client.pages.harvester.ush.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;


/**
 * Abstract Presenter Implementation Class for Harvester Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;

    // Application Models
    protected UshSolrHarvesterConfig config = null;

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
     * A signal to the presenter, saying that the name field has been changed
     * @param name, the new name value
     */
    @Override
    public void nameChanged(String name) {
        if (config != null) {
            config.getContent().withName(name);
        }
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     * @param description, the new description value
     */
    @Override
    public void descriptionChanged(String description) {
        if (config != null) {
            config.getContent().withDescription(description);
        }
    }

    /**
     * A signal to the presenter, saying that the submitter field has been changed
     * @param submitter, the new submitter value
     */
    @Override
    public void submitterChanged(String submitter) {
        if (config != null) {
            try {
                config.getContent().withSubmitterNumber(Integer.valueOf(submitter));
            } catch (NumberFormatException e) {
                config.getContent().withSubmitterNumber(0);
                getView().setErrorText(getTexts().error_SubmitterNumberValidationError());
            }
        }
    }

    /**
     * A signal to the presenter, saying that the format field has been changed
     * @param format, the new format value
     */
    @Override
    public void formatChanged(String format) {
        if (config != null) {
            config.getContent().withFormat(format);
        }
    }

    /**
     * A signal to the presenter, saying that the destination field has been changed
     * @param destination, the new destination value
     */
    @Override
    public void destinationChanged(String destination) {
        if (config != null) {
            config.getContent().withDestination(destination);
        }
    }

    /**
     * A signal to the presenter, saying that the enabled field has been changed
     * @param enabled, the new enabled value
     */
    @Override
    public void enabledChanged(Boolean enabled) {
        if (config != null) {
            config.getContent().withEnabled(enabled);
        }
    }


    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        if (config != null) {
            getView().status.setText("");
        }
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (isInputFieldsEmpty(config)) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        } else {
            saveModel();
        }
    }


    /*
     * Protected methods
     */

    /**
     * Method used to set the model after a successful update or a save
     * @param config The config to save
     */
    protected void setUshSolrHarvesterConfig(UshSolrHarvesterConfig config) {
        this.config = config;
    }

    /*
     * Private methods
     */

    private boolean isInputFieldsEmpty(UshSolrHarvesterConfig config) {
        return  config == null ||
                config.getContent() == null ||
                config.getContent().getName() == null ||
                config.getContent().getName().isEmpty() ||
                config.getContent().getDescription() == null ||
                config.getContent().getDescription().isEmpty() ||
                config.getContent().getSubmitterNumber() == null ||
                config.getContent().getSubmitterNumber() == 0 ||
                config.getContent().getFormat() == null ||
                config.getContent().getFormat().isEmpty() ||
                config.getContent().getDestination() == null ||
                config.getContent().getDestination().isEmpty();
    }

    private void initializeView() {
        getView().setHeader(this.header);
        getView().setPresenter(this);
    }

    private void initializeViewFields(
            Boolean viewEnabled,
            String name,
            String description,
            String packaging,
            String format,
            String charset,
            String destination,
            Integer submitter,
            String type,
            Boolean enabled) {
        View view = getView();
        view.name.setText(name);
        view.name.setEnabled(false);  // Name is hardcoded to disabled
        view.description.setText(description);
        view.description.setEnabled(viewEnabled);
        view.packaging.setText(packaging);
        view.packaging.setEnabled(false);  // Packaging is hardcoded to disabled
        view.format.setText(format);
        view.format.setEnabled(viewEnabled);
        view.charset.setText(charset);
        view.charset.setEnabled(false);  // Charset is hardcoded to disabled
        view.destination.setText(destination);
        view.destination.setEnabled(viewEnabled);
        view.submitter.setText(submitter == null ? "" : String.valueOf(submitter));
        view.submitter.setEnabled(viewEnabled);
        view.type.setText(type);
        view.type.setEnabled(false);  // Type is hardcoded to disabled
        view.enabled.setValue(enabled);
        view.enabled.setEnabled(viewEnabled);
        view.status.setText("");
    }

    private void initializeViewFields() {
        initializeViewFields(false, "", "", "", "", "", "", null, "", false);
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        initializeViewFields(
                true, // Enable all fields
                config.getContent().getName(),
                config.getContent().getDescription(),
                config.getContent().getPackaging(),
                config.getContent().getFormat(),
                config.getContent().getCharset(),
                config.getContent().getDestination(),
                config.getContent().getSubmitterNumber(),
                config.getContent().getType().toString(),
                config.getContent().isEnabled());
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


