package dk.dbc.dataio.gui.client.pages.submitter.submittermodify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Abstract Presenter Implementation Class for Submitter Create and Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    protected SubmitterModifyConstants constants;
    protected FlowStoreProxyAsync flowStoreProxy;
    protected View view;
    protected SubmitterModel model;

    private final static String EMPTY = "";


    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method
     *
     * @param clientFactory, clientFactory
     * @param constants, the constants for submitter modify
     */
    public PresenterImpl(ClientFactory clientFactory, SubmitterModifyConstants constants) {
        this.constants = constants;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the number field has been changed
     * @param number, the new number value
     */
    public void numberChanged(String number) {
        model.setNumber(number);
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     * @param name, the new name value
     */
    public void nameChanged(String name) {
        model.setName(name);
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     * @param description, the new description value
     */
    public void descriptionChanged(String description) {
        model.setDescription(description);
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    public void keyPressed() {
        view.setStatusText(EMPTY);
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    public void saveButtonPressed() {
        saveModel();
    }



    /*
     * Private methods
     */

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        view.setNumber(model.getNumber());
        view.setName(model.getName());
        view.setDescription(model.getDescription());
        view.setStatusText(EMPTY);
    }


    /*
     * Protected methods
     */

    /**
     * A local method to be used to translate an exception to a readable text
     * @param e Exception
     * @return errorMessage, the error text
     */
    protected String getErrorText(Throwable e) {
        ProxyError errorCode = null;
        if (e instanceof ProxyException) {
            errorCode = ((ProxyException) e).getErrorCode();
        }
        final String errorMessage;
        if (errorCode == null) {
            errorMessage = e.getMessage();
        } else {
            switch (errorCode) {
                case NOT_ACCEPTABLE: errorMessage = constants.error_ProxyKeyViolationError();
                    break;
                case BAD_REQUEST: errorMessage = constants.error_ProxyDataValidationError();
                    break;
                default: errorMessage = e.getMessage();
                    break;
            }
        }
        return errorMessage;
    }


    /*
     * Local class
     */

    /**
     * Local call back class to be instantiated in the call to createSubmitter or updateSubmitter in flowstore proxy
     */
    class SaveSubmitterModelFilteredAsyncCallback extends FilteredAsyncCallback<SubmitterModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(getErrorText(e));
        }

        @Override
        public void onSuccess(SubmitterModel model) {
            view.setStatusText(constants.status_SubmitterSuccessfullySaved());
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
