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

package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    private final static String EMPTY = "";
    private SelectFlowComponentDialogBox selectFlowComponentDialogBox;

    protected final Texts texts;
    protected final ProxyErrorTexts proxyErrorTexts;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected ViewWidget view;

    // Application Models
    protected List<FlowComponentModel> availableFlowComponentModels;
    protected PlaceController placeController;


    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method
     *
     * @param clientFactory clientFactory
     */
    public PresenterImpl(ClientFactory clientFactory) {
        texts = clientFactory.getFlowModifyTexts();
        proxyErrorTexts = clientFactory.getProxyErrorTexts();
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
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
        initializeViewFields();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initializeAvailableFlowComponentModels();
        initializeModel();
    }

    private void initializeViewFields() {
        view.name.clearText();
        view.name.setEnabled(false);
        view.description.clearText();
        view.description.setEnabled(false);
        view.flowComponents.clear();
        view.flowComponents.setEnabled(false);
        view.status.setText("");
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new value
     */
    @Override
    public void nameChanged(String name) {
        if (view.model != null && name != null) {
            view.model.setFlowName(name);
        }
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new value
     */
    @Override
    public void descriptionChanged(String description) {
        if (view.model != null && description != null) {
            view.model.setDescription(description);
        }
    }

    /**
     * A signal to the presenter, saying that the flow components field has been changed
     *
     * @param flowComponents, the list of flow components
     */
    @Override
    public void flowComponentsChanged(Map<String, String> flowComponents) {
        if (flowComponents != null) {
            List<FlowComponentModel> flowComponentModels = new ArrayList<>();
            for (Map.Entry<String, String> entry: flowComponents.entrySet()) {
                flowComponentModels.add(getFlowComponentModel(entry.getKey()));
            }
            view.model.setFlowComponents(flowComponentModels);
        }
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        view.status.setText(EMPTY);
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (view.model.isInputFieldsEmpty()) {
            view.setErrorText(texts.error_InputFieldValidationError());
        } else if (!view.model.getDataioPatternMatches().isEmpty()) {
            view.setErrorText(texts.error_NameFormatValidationError());
        } else {
            saveModel();
        }
    }

    @Override
    public void addButtonPressed() {
        if (availableFlowComponentModels != null ) {
            Map<String, String> listOfComponents = new LinkedHashMap<>();
            for (FlowComponentModel component : getNonSelectedFlowComponents()) {
                listOfComponents.put(component.getName(), String.valueOf(component.getId()));
            }
            selectFlowComponentDialogBox = new SelectFlowComponentDialogBox(listOfComponents, new SelectFlowComponentClickHandler(), this);
        }
    }

    @Override
    public void removeButtonPressed() {
        try {
            List<FlowComponentModel> flowComponentModels = view.model.getFlowComponents();
            flowComponentModels.remove(getFlowComponentModelIndex(view.flowComponents.getSelectedItem()));
            view.model.setFlowComponents(flowComponentModels);
            updateAllFieldsAccordingToCurrentState();
        } catch (Exception e) {  // NOPMD
            // NOPMD
            // Exceptions are not caught intentionally here - If an exception occurs, nothing is being removed
        }
    }

    /**
     * This method opens a new view, for creating a new flow component
     */
    @Override
    public void newFlowComponentButtonPressed() {
        view.showAvailableFlowComponents = true;
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace());
    }


    /*
     * Protected methods
     */

    /**
     * This method opdates all the fields in the view according to the stored model.
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        view.name.setText(view.model.getFlowName());
        view.name.setEnabled(true);
        view.description.setText(view.model.getDescription());
        view.description.setEnabled(true);
        view.flowComponents.clear();
        for (FlowComponentModel flowComponentModel: view.model.getFlowComponents()) {
            view.flowComponents.addValue(flowComponentModel.getName(), Long.toString(flowComponentModel.getId()));
        }
        if (availableFlowComponentModels != null) {
            view.flowComponents.setEnabled(true);
            if(view.showAvailableFlowComponents) {
                addButtonPressed();
                view.showAvailableFlowComponents = false;
            }
        }
        view.name.setFocus(true);

    }

    /**
     * Method used to set the model after a successful update or a save
     * @param model The model to save
     */
    protected void setFlowModel(FlowModel model) {
        this.view.model = model;
    }


    /*
     * Private methods
     */
    void initializeAvailableFlowComponentModels() {
        flowStoreProxy.findAllFlowComponents(new FindAllFlowComponentsAsyncCallback());
    }

    private List<FlowComponentModel> getNonSelectedFlowComponents() {
        List<FlowComponentModel> nonSelectedFlowComponents = new ArrayList<>();
        List<Long> selectedFlowComponentIds = new ArrayList<>();
        if (view.model != null) {
            for (FlowComponentModel selected: view.model.getFlowComponents()) {
                selectedFlowComponentIds.add(selected.getId());
            }
        }
        if (availableFlowComponentModels != null) {
            for (FlowComponentModel available:  availableFlowComponentModels) {
                if (!selectedFlowComponentIds.contains(available.getId())) {
                    nonSelectedFlowComponents.add(available);
                }
            }
        }
        return nonSelectedFlowComponents;
    }

    private FlowComponentModel getFlowComponentModel(String idString) {
        long id = Long.parseLong(idString);
        for (FlowComponentModel flowComponentModel: availableFlowComponentModels) {
            if (flowComponentModel.getId() == id) {
                return flowComponentModel;
            }
        }
        throw new IllegalArgumentException("FlowComponent not found");
    }

    private int getFlowComponentModelIndex(String idString) {
        long id = Long.parseLong(idString);
        List<FlowComponentModel> flowComponentModels = view.model.getFlowComponents();
        for (int index = 0; index < flowComponentModels.size(); index++) {
            if (flowComponentModels.get(index).getId() == id) {
                return index;
            }
        }
        throw new IllegalArgumentException("FlowComponent not found");
    }

    /*
     * Local classes
     */

    /**
     * Local call back class to be instantiated in the call to findAllFlowComponents in flowstore proxy
     */
    class FindAllFlowComponentsAsyncCallback implements AsyncCallback<List<FlowComponentModel>> {
        @Override
        public void onFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, this.getClass().getCanonicalName()));
        }
        @Override
        public void onSuccess(List<FlowComponentModel> result) {
            availableFlowComponentModels = result;
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to createFlow or updateFlow in flowstore proxy
     */
    class SaveFlowModelAsyncCallback implements AsyncCallback<FlowModel> {
        @Override
        public void onFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, null));
        }

        @Override
        public void onSuccess(FlowModel model) {
            view.status.setText(texts.status_FlowSuccessfullySaved());
            setFlowModel(new FlowModel());
            History.back();
        }
    }

    /**
     * Local class to be used to establish a callback from the Select Flow Components Dialog Box
     */
    class SelectFlowComponentClickHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            int selected = selectFlowComponentDialogBox.flowComponentsList.getSelectedIndex();
            if (selected >= 0) {
                String value = selectFlowComponentDialogBox.flowComponentsList.getValue(selected);
                FlowComponentModel selectedModel = getFlowComponentModel(value);
                List<FlowComponentModel> flowComponentModels = view.model.getFlowComponents();
                flowComponentModels.add(selectedModel);
                view.model.setFlowComponents(flowComponentModels);
                updateAllFieldsAccordingToCurrentState();
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
