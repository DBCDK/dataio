package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.Arrays;
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
    protected FlowStoreProxyAsync flowStoreProxy;
    protected ViewWidget view;

    // Application Models
    protected FlowModel model;
    protected List<FlowComponentModel> availableFlowComponentModels;


    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method
     *
     * @param clientFactory clientFactory
     * @param texts         the texts for flow modify
     */
    public PresenterImpl(ClientFactory clientFactory, Texts texts) {
        this.texts = texts;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
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
        if (model != null && name != null) {
            model.setFlowName(name);
        }
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new value
     */
    @Override
    public void descriptionChanged(String description) {
        if (model != null && description != null) {
            model.setDescription(description);
        }
    }

    /**
     * A signal to the presenter, saying that the flow components field has been changed
     *
     * @param flowComponents, the list of flow components
     */
    @Override
    public void flowComponentsChanged(Map<String, String> flowComponents) {
        if (model != null && flowComponents != null) {
            List<FlowComponentModel> flowComponentModels = new ArrayList<FlowComponentModel>();
            for (Map.Entry<String, String> entry: flowComponents.entrySet()) {
                flowComponentModels.add(getFlowComponentModel(entry.getKey()));
            }
            model.setFlowComponents(flowComponentModels);
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
        if (model != null) {
            if (model.isInputFieldsEmpty()) {
                view.setErrorText(texts.error_InputFieldValidationError());
            } else {
                saveModel();
            }
        }
    }

    @Override
    public void addButtonPressed() {
        if (availableFlowComponentModels != null ) {
            Map<String, String> listOfComponents = new LinkedHashMap<String, String>();
            for (FlowComponentModel component : getNonSelectedFlowComponents()) {
                listOfComponents.put(component.getName(), String.valueOf(component.getId()));
            }
            selectFlowComponentDialogBox = new SelectFlowComponentDialogBox(listOfComponents, new SelectFlowComponentClickHandler());
        }
    }

    @Override
    public void removeButtonPressed() {
        if (model != null) {
            try {
                List<FlowComponentModel> flowComponentModels = model.getFlowComponents();
                flowComponentModels.remove(getFlowComponentModelIndex(view.flowComponents.getSelectedItem()));
                model.setFlowComponents(flowComponentModels);
                updateAllFieldsAccordingToCurrentState();
            } finally {
                // Exceptions are not caught intentionally here - If an exception occurs, nothing is being removed
            }
        }
    }


        /*
     * Protected methods
     */

    /**
     * This method opdates all the fields in the view according to the stored model.
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        if (model != null) {
            view.name.setText(model.getFlowName());
            view.name.setEnabled(true);
            view.description.setText(model.getDescription());
            view.description.setEnabled(true);
            view.flowComponents.clear();
            for (FlowComponentModel flowComponentModel: model.getFlowComponents()) {
                view.flowComponents.addValue(flowComponentModel.getName(), Long.toString(flowComponentModel.getId()));
            }
            if (availableFlowComponentModels != null) {
                view.flowComponents.setEnabled(true);
            }
        }
    }

    /**
     * Method used to set the model after a successful update or a save
     * @param model The model to save
     */
    protected void setFlowModel(FlowModel model) {
        this.model = model;
    }


    /*
     * Private methods
     */
    void initializeAvailableFlowComponentModels() {
        flowStoreProxy.findAllFlowComponents(new FindAllFlowComponentsAsyncCallback());
    }

    private void onFailureSendExceptionToView(Throwable e) {
        view.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
    }

    private List<FlowComponentModel> getNonSelectedFlowComponents() {
        List<FlowComponentModel> nonSelectedFlowComponents = new ArrayList<FlowComponentModel>();
        List<Long> selectedFlowComponentIds = new ArrayList<Long>();
        if (model != null) {
            for (FlowComponentModel selected: model.getFlowComponents()) {
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
        List<FlowComponentModel> flowComponentModels = model.getFlowComponents();
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
            onFailureSendExceptionToView(e);
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
            onFailureSendExceptionToView(e);
        }

        @Override
        public void onSuccess(FlowModel model) {
            view.status.setText(texts.status_FlowSuccessfullySaved());
            setFlowModel(model);
        }
    }

    /**
     * Local class to be used to establish a callback from the Select Flow Components Dialog Box
     */
    class SelectFlowComponentClickHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            if (model != null) {
                int selected = selectFlowComponentDialogBox.flowComponentsList.getSelectedIndex();
                if (selected >= 0) {
                    String value = selectFlowComponentDialogBox.flowComponentsList.getValue(selected);
                    FlowComponentModel selectedModel = getFlowComponentModel(value);
                    List<FlowComponentModel> flowComponentModels = model.getFlowComponents();
                    flowComponentModels.add(selectedModel);
                    model.setFlowComponents(flowComponentModels);
                    updateAllFieldsAccordingToCurrentState();
                }
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
