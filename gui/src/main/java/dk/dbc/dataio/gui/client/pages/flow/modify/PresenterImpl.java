package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    protected Texts texts;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected View view;
    protected FlowModel model;
    protected List<FlowComponentModel> availableFlowComponentModels;

    private final static String EMPTY = "";

    public PresenterImpl(ClientFactory clientFactory, Texts texts) {
        this.texts = texts;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        availableFlowComponentModels = new ArrayList<FlowComponentModel>();
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the CreatePlace or EditPlace are being invoked
     * This method is the start signal for the presenter
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initializeModel();
        initializeAvailableFlowComponentModels();
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     * @param name, the new name value
     */
    @Override
    public void nameChanged(String name) {
        model.setFlowName(name);
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     * @param description, the new name value
     */
    @Override
    public void descriptionChanged(String description) {
        model.setDescription(description);
    }

    /**
     * A signal to the presenter, saying that the flow components selection field has been changed
     */
    @Override
    public void flowComponentsChanged(Map<String, String> flowComponents) {
        List<FlowComponentModel> flowComponentModels = new ArrayList<FlowComponentModel>();
        for (Map.Entry<String, String> entry: flowComponents.entrySet()) {
            flowComponentModels.add(getFlowComponentModel(entry.getKey()));
        }
        model.setFlowComponents(flowComponentModels);
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

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        view.setStatusText(EMPTY);
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (model.isInputFieldsEmpty()) {
            view.setErrorText(texts.error_InputFieldValidationError());
        } else {
            saveModel();
        }
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

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        view.setName(model.getFlowName());
        view.setDescription(model.getDescription());
        view.setSelectedFlowComponents(FlowComponentModelList2Map(model.getFlowComponents()));
        view.setAvailableFlowComponents(FlowComponentModelList2Map(getNonSelectedFlowComponents()));
        view.setStatusText(EMPTY);
    }

    private List<FlowComponentModel> getNonSelectedFlowComponents() {
        List<FlowComponentModel> nonSelectedFlowComponents = new ArrayList<FlowComponentModel>();
        List<Long> selectedFlowComponentIds = new ArrayList<Long>();
        for (FlowComponentModel selected: model.getFlowComponents()) {
            selectedFlowComponentIds.add(selected.getId());
        }
        for (FlowComponentModel available:  availableFlowComponentModels) {
            if (!selectedFlowComponentIds.contains(available.getId())) {
                nonSelectedFlowComponents.add(available);
            }
        }
        return nonSelectedFlowComponents;
    }

    private Map<String, String> FlowComponentModelList2Map(List<FlowComponentModel> flowComponentModels) {
        Map<String, String> result = new HashMap<String, String>();
        for (FlowComponentModel flowComponentModel: flowComponentModels) {
            result.put(String.valueOf(flowComponentModel.getId()), flowComponentModel.getName());
        }
        return result;
    }


    /*
     * Protected methods
     */



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
            view.setStatusText(texts.status_FlowSuccessfullySaved());
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
