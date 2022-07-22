package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;


/**
 * Abstract Presenter Implementation Class for Harvester Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;

    // Application Models
    protected TickleRepoHarvesterConfig config = null;

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
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the id field has been changed
     *
     * @param id, the new id value
     */
    @Override
    public void idChanged(String id) {
        if (config != null) {
            config.getContent().withId(id);
        }
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new name value
     */
    @Override
    public void nameChanged(String name) {
        if (config != null) {
            config.getContent().withDatasetName(name);
        }
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new description value
     */
    @Override
    public void descriptionChanged(String description) {
        if (config != null) {
            config.getContent().withDescription(description);
        }
    }

    /**
     * A signal to the presenter, saying that the destination field has been changed
     *
     * @param destination, the new destination value
     */
    @Override
    public void destinationChanged(String destination) {
        if (config != null) {
            config.getContent().withDestination(destination);
        }
    }

    /**
     * A signal to the presenter, saying that the format field has been changed
     *
     * @param format, the new format value
     */
    @Override
    public void formatChanged(String format) {
        if (config != null) {
            config.getContent().withFormat(format);
        }
    }

    /**
     * A signal to the presenter, saying that the type field has been changed
     *
     * @param type, the new type value
     */
    @Override
    public void typeChanged(String type) {
        if (config != null) {
            config.getContent().withType(JobSpecification.Type.valueOf(type));
        }
    }

    /**
     * A signal to the presenter, saying that the enabled field has been changed
     *
     * @param enabled, the new enabled value
     */
    @Override
    public void enabledChanged(Boolean enabled) {
        if (config != null) {
            config.getContent().withEnabled(enabled);
        }
    }

    /**
     * A signal to the presenter, saying that the notifications enabled field has been changed
     *
     * @param enabled, the new enabled value
     */
    @Override
    public void notificationsEnabledChanged(Boolean enabled) {
        if (config != null) {
            config.getContent().withNotificationsEnabled(enabled);
        }
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        if (config != null) {
            getView().status.setText("");
        }
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (isInputFieldsEmpty(config)) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        } else {
            saveModel();
        }
    }


    /*
     * Protected methods
     */

    /**
     * Method used to set the model after a successful save
     *
     * @param config The config to save
     */
    void setTickleRepoHarvesterConfig(TickleRepoHarvesterConfig config) {
        this.config = config;
    }


    /*
     * Private methods
     */

    private boolean isInputFieldsEmpty(TickleRepoHarvesterConfig config) {
        return config == null ||
                config.getContent() == null ||
                config.getContent().getId() == null ||
                config.getContent().getId().isEmpty() ||
                config.getContent().getDatasetName() == null ||
                config.getContent().getDatasetName().isEmpty() ||
                config.getContent().getDescription() == null ||
                config.getContent().getDescription().isEmpty() ||
                config.getContent().getDestination() == null ||
                config.getContent().getDestination().isEmpty() ||
                config.getContent().getFormat() == null ||
                config.getContent().getFormat().isEmpty() ||
                config.getContent().getType() == null ||
                config.getContent().getType().toString().isEmpty();
    }

    private void initializeView() {
        getView().setHeader(this.header);
        getView().setPresenter(this);
    }

    private void initializeViewFields(
            Boolean viewEnabled,
            String id,
            String name,
            String description,
            String destination,
            String format,
            String type,
            Boolean enabled,
            Boolean notificationsEnabled) {
        View view = getView();
        view.id.setText(id);
        view.id.setEnabled(viewEnabled);
        view.name.setText(name);
        view.name.setEnabled(viewEnabled);
        view.description.setText(description);
        view.description.setEnabled(viewEnabled);
        view.destination.setText(destination);
        view.destination.setEnabled(viewEnabled);
        view.format.setText(format);
        view.format.setEnabled(viewEnabled);
        view.type.clear();
        for (JobSpecification.Type t : JobSpecification.Type.values()) {
            if (t != JobSpecification.Type.COMPACTED && t != JobSpecification.Type.SUPER_TRANSIENT) {
                view.type.addAvailableItem(t.toString());
            }
        }
        view.type.setSelectedValue(type);
        view.type.setEnabled(viewEnabled);
        view.enabled.setValue(enabled);
        view.enabled.setEnabled(viewEnabled);
        view.notificationsEnabled.setValue(notificationsEnabled);
        view.notificationsEnabled.setEnabled(viewEnabled);
        view.status.setText("");
    }

    private void initializeViewFields() {
        initializeViewFields(false, "", "", "", "", "", "", false, false);
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        initializeViewFields(
                true, // Enable all fields
                config.getContent().getId(),
                config.getContent().getDatasetName(),
                config.getContent().getDescription(),
                config.getContent().getDestination(),
                config.getContent().getFormat(),
                config.getContent().getType().toString(),
                config.getContent().isEnabled(),
                config.getContent().hasNotificationsEnabled());
    }

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
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


