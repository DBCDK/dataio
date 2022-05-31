package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.Arrays;

/**
 * Abstract Presenter Implementation Class for Submitter Create and Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    final String USE_FLOWBINDER_PRIORITY_KEY = "-1";
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    protected String header;

    // Application Models
    protected SubmitterModel model = new SubmitterModel();


    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method
     *
     * @param header Breadcrumb header breadcrumb
     */
    public PresenterImpl(String header) {
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
        setAvailablePriorities();
        initializeModel();
    }


    /**
     * A signal to the presenter, saying that the number field has been changed
     *
     * @param number, the new number value
     */
    public void numberChanged(String number) {
        model.setNumber(number);
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new name value
     */
    public void nameChanged(String name) {
        model.setName(name);
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new description value
     */
    public void descriptionChanged(String description) {
        model.setDescription(description);
    }

    /**
     * A signal to the presenter, saying that the priority field has been changed
     *
     * @param priority, the new value
     */
    @Override
    public void priorityChanged(String priority) {
        if (priority == null || Integer.valueOf(priority) < 0) {
            model.setPriority(null);
        } else {
            model.setPriority(Integer.valueOf(priority));
        }
    }

    /**
     * A signal to the presenter, saying that the value of the disable field has changed
     *
     * @param isSubmitterDisabled, true if submitter is disabled, false if submitter is enabled.
     */
    @Override
    public void disabledStatusChanged(Boolean isSubmitterDisabled) {
        model.setEnabled(!isSubmitterDisabled);
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    public void keyPressed() {
        getView().status.setText("");
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    public void saveButtonPressed() {
        if (model != null) {
            if (model.isInputFieldsEmpty()) {
                getView().setErrorText(getTexts().error_InputFieldValidationError());
            } else if (!model.isNumberValid()) {
                getView().setErrorText(getTexts().error_NumberInputFieldValidationError());
            } else if (!model.getDataioPatternMatches().isEmpty()) {
                getView().setErrorText(getTexts().error_NameFormatValidationError());
            } else {
                saveModel();
            }
        }
    }

    /*
     * Private methods
     */
    private void initializeView() {
        getView().setPresenter(this);
        getView().setHeader(header);
    }

    public void initializeViewFields() {
        View view = getView();
        view.number.setValue("");
        view.number.setEnabled(false);
        view.name.setValue("");
        view.name.setEnabled(false);
        view.description.setValue("");
        view.description.setEnabled(false);
        view.priority.setSelectedValue(USE_FLOWBINDER_PRIORITY_KEY);
        view.priority.setEnabled(false);
        view.disabledStatus.setEnabled(true);
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        View view = getView();
        if (model.getId() == 0) {
            view.number.setEnabled(true);
        }
        view.number.setValue(model.getNumber());
        view.name.setValue(model.getName());
        view.name.setEnabled(true);
        view.description.setValue(model.getDescription());
        view.description.setEnabled(true);
        view.priority.setSelectedValue(model.getPriority() == null ? USE_FLOWBINDER_PRIORITY_KEY : model.getPriority().toString());
        view.priority.setEnabled(true);
        view.disabledStatus.setValue(!model.isEnabled());
        view.status.setText("");
        if (view.number.isEnabled()) {
            view.number.setFocus(true);
        } else if (view.name.isEnabled()) {
            view.name.setFocus(true);
        }
    }

    void setAvailablePriorities() {
        Arrays.stream(Priority.values()).forEach(priority -> getView().priority.addAvailableItem(formatPriority(priority), String.valueOf(priority.getValue())));
        getView().priority.addAvailableItem(getTexts().selection_UseFlowbinderPriority(), USE_FLOWBINDER_PRIORITY_KEY);
    }

    private String formatPriority(Priority priority) {
        switch (priority) {
            case LOW:
                return getTexts().selection_Low();
            case NORMAL:
                return getTexts().selection_Normal();
            case HIGH:
                return getTexts().selection_High();
            default:
                return "<unknown priority>";
        }
    }


    /*
     * Protected methods
     */

    /**
     * Method used to set the model after a successful update or a save
     *
     * @param model The model to save
     */
    protected void setSubmitterModel(SubmitterModel model) {
        this.model = model;
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
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(SubmitterModel model) {
            getView().status.setText(getTexts().status_SubmitterSuccessfullySaved());
            setSubmitterModel(model);
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

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }
}
