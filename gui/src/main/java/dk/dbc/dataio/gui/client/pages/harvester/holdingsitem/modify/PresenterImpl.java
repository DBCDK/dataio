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

package dk.dbc.dataio.gui.client.pages.harvester.holdingsitem.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.HoldingsItemHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Abstract Presenter Implementation Class for Harvester Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;

    private List<RRHarvesterConfig> availableRrHarvesterConfigs = null;


    // Application Models
    protected HoldingsItemHarvesterConfig config = null;

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
        fetchAvailableRrHarvesterConfigs();
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
     * A signal to the presenter, saying that the resource field has been changed
     * @param resource, the new resource value
     */
    @Override
    public void resourceChanged(String resource) {
        if (config != null) {
            config.getContent().withResource(resource);
        }
    }

    /**
     * A signal to the presenter, saying that the rrHarvesters field has been changed
     * @param rrHarvesters, the new rrHarvesters values
     */
    @Override
    public void rrHarvestersChanged(List<String> rrHarvesters) {
        if (config != null) {
            ArrayList<Long> rrHarvestersValues = new ArrayList<>(rrHarvesters.size());
            if (!rrHarvesters.isEmpty()) {
                rrHarvesters.forEach(value -> rrHarvestersValues.add(Long.valueOf(value)));
            }
            config.getContent().withRrHarvesters(rrHarvestersValues);
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
     * Signals the Presenter with the information, that the Add button has been pressed on the rrHarvesters MultiListbox
     */
    public void rrHarvestersAddButtonPressed() {
        if (availableRrHarvesterConfigs != null ) {
            getView().rrHarvestersListBox.clear();
            getNonSelectedRrHarvesters().forEach(
                harvester -> getView().rrHarvestersListBox.addItem(harvester.getContent().getId(), String.valueOf(harvester.getId()))
            );
            getView().rrHarvestersListBox.show();
        }
    }

    /**
     * Signals the Presenter with the information, that the Remove button has been pressed on the rrHarvesters MultiListbox
     */
    public void rrHarvestersRemoveButtonPressed() {
        String selectedItem = getView().rrHarvesters.getSelectedItem();
        if (selectedItem != null) {
            config.getContent().getRrHarvesters().remove(Long.valueOf(selectedItem));
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     *
     * @param selection The list of RR Harvesters (as a Map), when OK button is pressed
     */
    public void rrHarvestersListBoxOkButtonPressed(Map<String, String> selection) {
        if (selection != null) {
            Set<Long> harvesters = new LinkedHashSet<>();  // Preserve order in the set
            harvesters.addAll(config.getContent().getRrHarvesters());  // Put in a set to avoid duplicates
            selection.keySet().forEach(key -> harvesters.add(Long.valueOf(key)));
            final ArrayList<Long> result = new ArrayList<>();
            result.addAll(harvesters);
            config.getContent().withRrHarvesters(result);
            updateAllFieldsAccordingToCurrentState();
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
     * Method used to set the model after a successful save
     * @param config The config to save
     */
    void setHoldingsItemHarvesterConfig(HoldingsItemHarvesterConfig config) {
        this.config = config;
    }


    /*
     * Private methods
     */

    private boolean isInputFieldsEmpty(HoldingsItemHarvesterConfig config) {
        return  config == null ||
                config.getContent() == null ||
                config.getContent().getName() == null ||
                config.getContent().getName().isEmpty() ||
                config.getContent().getDescription() == null ||
                config.getContent().getDescription().isEmpty() ||
                config.getContent().getResource() == null ||
                config.getContent().getResource().isEmpty();
    }

    private void initializeView() {
        getView().setHeader(this.header);
        getView().setPresenter(this);
    }

    private void initializeViewFields(
            Boolean viewEnabled,
            String name,
            String description,
            String resource,
            List<Long> rrHarvesters,
            Boolean enabled) {
        View view = getView();
        view.name.setText(name);
        view.name.setEnabled(viewEnabled);
        view.description.setText(description);
        view.description.setEnabled(viewEnabled);
        view.resource.setText(resource);
        view.resource.setEnabled(viewEnabled);
        view.rrHarvesters.clear();
        if (rrHarvesters != null) {
            rrHarvesters.forEach(rrHarvester -> view.rrHarvesters.addValue(
                    GetHarvesterName(rrHarvester),
                    String.valueOf(rrHarvester)));
        }
        view.rrHarvesters.setEnabled(viewEnabled);
        if (availableRrHarvesterConfigs != null) {
            for (RRHarvesterConfig config: availableRrHarvesterConfigs) {
                view.rrHarvestersListBox.addItem(config.getContent().getId(), String.valueOf(config.getId()));
            }
        }
        view.enabled.setValue(enabled);
        view.enabled.setEnabled(viewEnabled);
        view.status.setText("");
    }

    private void initializeViewFields() {
        initializeViewFields(false, "", "", "", new ArrayList<>(), false);
    }

    void setAvailableRrHarvesterConfigs(List<RRHarvesterConfig> configs) {
        this.availableRrHarvesterConfigs = configs;
    }

    private List<RRHarvesterConfig> getNonSelectedRrHarvesters() {
        List<RRHarvesterConfig> nonSelectedHarvesters = new ArrayList<>();
        List<Long> selectedFlowComponentIds = new ArrayList<>();
        if (config != null) {
            config.getContent().getRrHarvesters().forEach(selectedFlowComponentIds::add);
        }
        if (availableRrHarvesterConfigs != null) {
            availableRrHarvesterConfigs.forEach(harvester -> {
                if (!selectedFlowComponentIds.contains(harvester.getId())) {
                    nonSelectedHarvesters.add(harvester);
                }
            });
        }
        return nonSelectedHarvesters;
    }

    private String GetHarvesterName(Long key) {
        if (availableRrHarvesterConfigs != null && key != null) {
            for (RRHarvesterConfig config: availableRrHarvesterConfigs) {
                if (key.equals(config.getId())) {
                    return config.getContent().getId();
                }
            }
        }
        return "";
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        initializeViewFields(
                true, // Enable all fields
                config.getContent().getName(),
                config.getContent().getDescription(),
                config.getContent().getResource(),
                config.getContent().getRrHarvesters(),
                config.getContent().isEnabled());
    }

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }

    private void fetchAvailableRrHarvesterConfigs() {
        commonInjector.getFlowStoreProxyAsync().findAllRRHarvesterConfigs(new FetchAvailableRRHarvesterConfigsCallback());
    }


    /*
     * Local classes
     */

    /**
     * Local call back class to be instantiated in the call to findAllRRHarvesterConfigs in flowstore proxy
     */
    class FetchAvailableRRHarvesterConfigsCallback extends FilteredAsyncCallback<List<RRHarvesterConfig>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }
        @Override
        public void onSuccess(List<RRHarvesterConfig> configs) {
            setAvailableRrHarvesterConfigs(configs);
            updateAllFieldsAccordingToCurrentState();
            getView().rrHarvestersChanged(null);  // Be sure that the view is aware of the new list of available RR Harvesters
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


