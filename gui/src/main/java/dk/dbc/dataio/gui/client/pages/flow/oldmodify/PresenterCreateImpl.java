package dk.dbc.dataio.gui.client.pages.flow.oldmodify;

import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flow Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     * @param clientFactory, clientFactory
     * @param texts, the constants for flow modify
     */
    public PresenterCreateImpl(ClientFactory clientFactory, Texts texts) {
        super(clientFactory, texts);
        view = clientFactory.getOldFlowCreateView();
    }

    /**
     * getModel - initializes the model
     * When starting the form, the fields should be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new FlowModel();
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Flow in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.createFlow(model, new SaveFlowModelAsyncCallback());
    }

}
