package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;

import java.util.Date;


public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    private static final long HOUR_IN_MS = 3600 * 1000;

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;
    protected InfomediaHarvesterConfig config = null;

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
        initializeViewFields();
        containerWidget.setWidget(getView().asWidget());
        initializeModel();
    }

    @Override
    public void idChanged(String id) {
        if (config != null) {
            config.getContent().withId(id);
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
    public void nextPublicationDateChanged(String date) {
        if (config != null) {
            if (date == null || date.trim().isEmpty()) {
                config.getContent()
                        .withNextPublicationDate(null);
            } else {
                // We have no easy way to adjust for DST on the
                // GWT client side, but since the harvester
                // truncates to day anyway, this will work
                // well enough to ensure that we don't end
                // up using the day before the one chosen, when
                // interpreting the point in time as UTC.
                final Date adjustedDate = new Date(Format.parseLongDateAsDate(date).getTime() + 2 * HOUR_IN_MS);
                config.getContent()
                        .withNextPublicationDate(adjustedDate);
            }
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
        if (ifInputFieldMissing(config)) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        } else {
            saveModel();
        }
    }

    /**
     * Sets the model after a successful save
     *
     * @param config the config to set
     */
    void setInfomediaHarvesterConfig(InfomediaHarvesterConfig config) {
        this.config = config;
    }

    private boolean ifInputFieldMissing(InfomediaHarvesterConfig config) {
        return config == null ||
                config.getContent() == null ||
                config.getContent().getId() == null ||
                config.getContent().getId().trim().isEmpty() ||
                config.getContent().getSchedule() == null ||
                config.getContent().getSchedule().trim().isEmpty() ||
                config.getContent().getDescription() == null ||
                config.getContent().getDescription().trim().isEmpty() ||
                config.getContent().getDestination() == null ||
                config.getContent().getDestination().trim().isEmpty() ||
                config.getContent().getFormat() == null ||
                config.getContent().getFormat().trim().isEmpty();
    }

    private void initializeView() {
        getView().setHeader(this.header);
        getView().setPresenter(this);
    }

    private void initializeViewFields(
            Boolean viewEnabled,
            String id,
            String schedule,
            String description,
            String destination,
            String format,
            String nextPublicationDate,
            Boolean enabled) {
        View view = getView();
        view.id.setText(id);
        view.id.setEnabled(viewEnabled);
        view.schedule.setText(schedule);
        view.schedule.setEnabled(viewEnabled);
        view.description.setText(description);
        view.description.setEnabled(viewEnabled);
        view.destination.setText(destination);
        view.destination.setEnabled(viewEnabled);
        view.format.setText(format);
        view.format.setEnabled(viewEnabled);
        view.nextPublicationDate.setValue(nextPublicationDate);
        view.enabled.setValue(enabled);
        view.enabled.setEnabled(viewEnabled);
        view.status.setText("");
    }

    private void initializeViewFields() {
        initializeViewFields(false, "", "", "", "", "", "", false);
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        String nextPublicationDate = "";
        if (config.getContent().getNextPublicationDate() != null) {
            nextPublicationDate = config.getContent()
                    .getNextPublicationDate().toString();
        }

        initializeViewFields(
                true, // Enable all fields
                config.getContent().getId(),
                config.getContent().getSchedule(),
                config.getContent().getDescription(),
                config.getContent().getDestination(),
                config.getContent().getFormat(),
                nextPublicationDate,
                config.getContent().isEnabled());
    }

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }
}


