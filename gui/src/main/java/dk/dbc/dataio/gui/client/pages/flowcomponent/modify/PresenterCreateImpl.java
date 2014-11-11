package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flow Component Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     * @param clientFactory, clientFactory
     * @param texts, the constants for flow component modify
     */
    public PresenterCreateImpl(ClientFactory clientFactory, Texts texts) {
        super(clientFactory, texts);
        view = clientFactory.getFlowComponentCreateView();
    }

    /**
     * getModel - initializes the model
     * When starting the form, the fields should be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new FlowComponentModel();
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Flow in the database
     */
    @Override
    void saveModel() {
        view.status.setText(texts.status_SavingFlowComponent());
        flowStoreProxy.createFlowComponent(model, new SaveFlowComponentModelFilteredAsyncCallback());
    }

}
