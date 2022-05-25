package dk.dbc.dataio.gui.client.pages.flow.modify;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Concrete Presenter Implementation Class for Flow Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     *
     * @param placeController PlaceController for navigation
     * @param header          Breadcrumb header text
     */
    public PresenterCreateImpl(PlaceController placeController, String header) {
        super(placeController, header);
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
        getView().deleteButton.setVisible(false);
    }

    /**
     * initializeModel - initializes the model
     * When starting the form, the fields shall be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Flow in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createFlow(getView().model, new SaveFlowModelAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {
    }
}
