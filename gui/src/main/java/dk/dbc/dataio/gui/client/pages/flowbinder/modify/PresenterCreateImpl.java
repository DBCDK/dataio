package dk.dbc.dataio.gui.client.pages.flowbinder.modify;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;

/**
 * Concrete Presenter Implementation Class for Flowbinder Create
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
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
    }

    /**
     * initializeModel - initializes the model
     * When starting the form, the fields shall be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new FlowBinderModel();
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Flowbinder in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createFlowBinder(model, new SaveFlowBinderModelFilteredAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {
    }
}
