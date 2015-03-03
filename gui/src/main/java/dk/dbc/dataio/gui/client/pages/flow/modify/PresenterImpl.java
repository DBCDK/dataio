package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class represents the create flow activity encompassing saving
 * of flow data in the flow store via RPC proxy
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    private final static String EMPTY = "";
    protected final Texts texts;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected View view;

    // Application Models
    protected FlowModel model = new FlowModel();
    protected List<FlowComponentModel> availableFlowComponentModels = new ArrayList<FlowComponentModel>();


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
        availableFlowComponentModels = new ArrayList<FlowComponentModel>();
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
        if (name != null) {
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
        if (description != null) {
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
        if (flowComponents != null) {
            List<FlowComponentModel> flowComponentModels = new ArrayList<FlowComponentModel>();
            for (Map.Entry<String, String> entry: flowComponents.entrySet()) {
                flowComponentModels.add(getFlowComponentModel(entry.getKey()));
            }
            model.setFlowComponents(flowComponentModels);
        }
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
        view.status.setText(EMPTY);
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
    protected void updateAllFieldsAccordingToCurrentState() {
        view.name.setText(model.getFlowName());
        view.name.setEnabled(true);
        view.description.setText(model.getDescription());
        view.description.setEnabled(true);
        view.flowComponents.clear();
        for (FlowComponentModel flowComponentModel: model.getFlowComponents()) {
            view.flowComponents.addValue(flowComponentModel.getName(), Long.toString(flowComponentModel.getId()));
        }
        view.flowComponents.setEnabled(true);
    }

    void initializeAvailableFlowComponentModels() {
        flowStoreProxy.findAllFlowComponents(new FindAllFlowComponentsAsyncCallback());
    }

    private void onFailureSendExceptionToView(Throwable e) {
        view.setErrorText(e.getClass().getName() + " - " + e.getMessage() + " - " + Arrays.toString(e.getStackTrace()));
    }


     /*
     * Protected methods
     */

    /**
     * Method used to set the model after a successful update or a save
     * @param model The model to save
     */
    protected void setFlowModel(FlowModel model) {
        this.model = model;
    }


    /*
     * Local class
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
