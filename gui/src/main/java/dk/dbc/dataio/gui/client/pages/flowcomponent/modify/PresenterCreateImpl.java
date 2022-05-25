package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import dk.dbc.dataio.gui.client.model.FlowComponentModel;

/**
 * Concrete Presenter Implementation Class for Flow Component Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     *
     * @param header Breadcrumb header text
     */
    public PresenterCreateImpl(String header) {
        super(header);
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
        getView().status.setText(getTexts().status_SavingFlowComponent());
        commonInjector.getFlowStoreProxyAsync().createFlowComponent(model, new SaveFlowComponentModelFilteredAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {
    }

}
