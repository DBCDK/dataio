package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    private final static String EMPTY = "";

    // Application Models
    protected List<FlowComponentModel> availableFlowComponentModels;
    protected PlaceController placeController;
    protected String header;

    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method
     *
     * @param placeController PlaceController for navigation
     * @param header          Breadcrumb header text
     */
    public PresenterImpl(PlaceController placeController, String header) {
        this.placeController = placeController;
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
        initializeAvailableFlowComponentModels();
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new value
     */
    @Override
    public void nameChanged(String name) {
        if (getView().model != null && name != null) {
            getView().model.setFlowName(name);
        }
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new value
     */
    @Override
    public void descriptionChanged(String description) {
        if (getView().model != null && description != null) {
            getView().model.setDescription(description);
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
            for (Map.Entry<String, String> entry : flowComponents.entrySet()) {
                flowComponentModels.add(getFlowComponentModel(entry.getKey()));
            }
            getView().model.setFlowComponents(flowComponentModels);
        }
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        getView().status.setText(EMPTY);
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (getView().model.isInputFieldsEmpty()) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        } else if (!getView().model.getDataioPatternMatches().isEmpty()) {
            getView().setErrorText(getTexts().error_NameFormatValidationError());
        } else {
            saveModel();
        }
    }

    @Override
    public void addButtonPressed() {
        if (availableFlowComponentModels != null) {
            getView().popupListBox.clear();
            for (FlowComponentModel component : getNonSelectedFlowComponents()) {
                getView().popupListBox.addItem(component.getName(), String.valueOf(component.getId()));
            }
            getView().popupListBox.show();
        }
    }

    @Override
    public void removeButtonPressed() {
        try {
            List<FlowComponentModel> flowComponentModels = getView().model.getFlowComponents();
            flowComponentModels.remove(getFlowComponentModelIndex(getView().flowComponents.getSelectedItem()));
            getView().model.setFlowComponents(flowComponentModels);
            updateAllFieldsAccordingToCurrentState();
        } catch (Exception e) {  // NOPMD
            // NOPMD
            // Exceptions are not caught intentionally here - If an exception occurs, nothing is being removed
        }
    }


    /**
     * Dialog Events for the PopupListBox component (FlowComponent Selector)
     */

    /**
     * Select a list of flow components from the list
     *
     * @param selectedKeys The list of keys of the selected Flow Components
     */
    @Override
    public void selectFlowComponentButtonPressed(Map<String, String> selectedKeys) {
        Set<FlowComponentModel> flowComponentModels = new LinkedHashSet<>();  // Preserve order in the set
        flowComponentModels.addAll(getView().model.getFlowComponents());  // Put in a set to avoid duplicates
        for (String id : selectedKeys.keySet()) {
            flowComponentModels.add(getFlowComponentModel(id));
        }
        final List<FlowComponentModel> result = new ArrayList<>();
        result.addAll(flowComponentModels);
        getView().model.setFlowComponents(result);
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * This method opens a new view, for creating a new flow component
     */
    @Override
    public void newFlowComponentButtonPressed() {
        getView().showAvailableFlowComponents = true;
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace());
    }


    /*
     * Protected methods
     */

    /**
     * This method updates all the fields in the view according to the stored model.
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        ViewWidget view = getView();
        view.name.setText(view.model.getFlowName());
        view.name.setEnabled(true);
        view.description.setText(view.model.getDescription());
        view.description.setEnabled(true);
        view.flowComponents.clear();
        for (FlowComponentModel flowComponentModel : view.model.getFlowComponents()) {
            view.flowComponents.addValue(flowComponentModel.getName(), Long.toString(flowComponentModel.getId()));
        }
        if (availableFlowComponentModels != null) {
            view.flowComponents.setEnabled(true);
            if (view.showAvailableFlowComponents) {
                addButtonPressed();
                view.showAvailableFlowComponents = false;
            }
        }
        view.name.setFocus(true);

    }

    /**
     * Method used to set the model after a successful update or a save
     *
     * @param model The model to save
     */
    protected void setFlowModel(FlowModel model) {
        this.getView().model = model;
    }


    /*
     * Private methods
     */
    protected ViewWidget getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }

    private void initializeView() {
        getView().setPresenter(this);
        getView().setHeader(this.header);
    }

    private void initializeViewFields() {
        ViewWidget view = getView();
        view.name.clearText();
        view.name.setEnabled(false);
        view.description.clearText();
        view.description.setEnabled(false);
        view.flowComponents.clear();
        view.flowComponents.setEnabled(false);
        view.status.setText("");
    }

    void initializeAvailableFlowComponentModels() {
        commonInjector.getFlowStoreProxyAsync().findAllFlowComponents(new FindAllFlowComponentsAsyncCallback());
    }

    private List<FlowComponentModel> getNonSelectedFlowComponents() {
        List<FlowComponentModel> nonSelectedFlowComponents = new ArrayList<>();
        List<Long> selectedFlowComponentIds = new ArrayList<>();
        if (getView().model != null) {
            for (FlowComponentModel selected : getView().model.getFlowComponents()) {
                selectedFlowComponentIds.add(selected.getId());
            }
        }
        if (availableFlowComponentModels != null) {
            for (FlowComponentModel available : availableFlowComponentModels) {
                if (!selectedFlowComponentIds.contains(available.getId())) {
                    nonSelectedFlowComponents.add(available);
                }
            }
        }
        return nonSelectedFlowComponents;
    }

    private FlowComponentModel getFlowComponentModel(String idString) {
        long id = Long.parseLong(idString);
        for (FlowComponentModel flowComponentModel : availableFlowComponentModels) {
            if (flowComponentModel.getId() == id) {
                return flowComponentModel;
            }
        }
        throw new IllegalArgumentException("FlowComponent not found");
    }

    private int getFlowComponentModelIndex(String idString) {
        long id = Long.parseLong(idString);
        List<FlowComponentModel> flowComponentModels = getView().model.getFlowComponents();
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
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
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
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(FlowModel model) {
            getView().status.setText(getTexts().status_FlowSuccessfullySaved());
            setFlowModel(new FlowModel());
            History.back();
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
