package dk.dbc.dataio.gui.client.pages.harvester.promat.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;


public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;
    protected PromatHarvesterConfig config = null;

    public PresenterImpl(String header) {
        this.header = header;
    }

    abstract void initializeModel();

    abstract void saveModel();

    /**
     * Called by PlaceManager whenever the PlaceCreate or PlaceEdit is invoked.
     * This method is the start signal for the presenter.
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        initializeView();
        containerWidget.setWidget(getView().asWidget());
        initializeModel();
    }

    @Override
    public void nameChanged(String name) {
        if (config != null) {
            config.getContent().withName(name);
        }
    }

    @Override
    public void scheduleChanged(String schedule) {
        if (config != null) {
            config.getContent().withSchedule(schedule);
        }
    }

    @Override
    public void descriptionChanged(String description) {
        if (config != null) {
            config.getContent().withDescription(description);
        }
    }

    @Override
    public void destinationChanged(String destination) {
        if (config != null) {
            config.getContent().withDestination(destination);
        }
    }

    @Override
    public void formatChanged(String format) {
        if (config != null) {
            config.getContent().withFormat(format);
        }
    }

    @Override
    public void enabledChanged(Boolean enabled) {
        if (config != null) {
            config.getContent().withEnabled(enabled);
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
        if (isInputFieldMissing()) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
            return;
        }
        saveModel();
    }


    /**
     * Sets the model after a successful save
     *
     * @param config the config to set
     */
    void setConfig(PromatHarvesterConfig config) {
        this.config = config;
        setViewFields();
    }

    private void initializeView() {
        final View view = getView();
        view.setHeader(this.header);
        view.setPresenter(this);
        view.name.setEnabled(true);
        view.schedule.setEnabled(true);
        view.description.setEnabled(true);
        view.destination.setEnabled(true);
        view.format.setEnabled(true);
        view.enabled.setEnabled(true);
    }

    private void setViewFields() {
        final View view = getView();
        final PromatHarvesterConfig.Content configContent = config.getContent();
        view.name.setText(configContent.getName());
        view.schedule.setText(configContent.getSchedule());
        view.description.setText(configContent.getDescription());
        view.destination.setText(configContent.getDestination());
        view.format.setText(configContent.getFormat());
        view.enabled.setValue(configContent.isEnabled());
        view.status.setText("");
    }

    private boolean isInputFieldMissing() {
        return config == null
                || config.getContent() == null
                || isUndefined(config.getContent().getName())
                || isUndefined(config.getContent().getSchedule())
                || isUndefined(config.getContent().getDescription())
                || isUndefined(config.getContent().getDestination())
                || isUndefined(config.getContent().getFormat());
    }

    private boolean isUndefined(String value) {
        return value == null || value.trim().isEmpty();
    }

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }
}

