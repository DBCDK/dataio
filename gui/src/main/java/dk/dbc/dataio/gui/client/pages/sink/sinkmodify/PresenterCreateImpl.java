package dk.dbc.dataio.gui.client.pages.sink.sinkmodify;

import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Sink Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     * @param clientFactory, clientFactory
     * @param constants, the constants for submitter modify
     */
    public PresenterCreateImpl(ClientFactory clientFactory, SinkModifyConstants constants) {
        super(clientFactory, constants);
        view = clientFactory.getSinkCreateView();
    }
    /**
     * getModel - initializes the model
     * When starting the form, the fields should be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new SinkModel();
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Sink in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.createSink(model, new SaveSinkModelFilteredAsyncCallback());
    }

}
