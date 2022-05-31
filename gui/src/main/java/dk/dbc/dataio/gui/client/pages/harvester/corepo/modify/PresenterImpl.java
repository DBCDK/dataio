package dk.dbc.dataio.gui.client.pages.harvester.corepo.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

import java.util.List;


/**
 * Abstract Presenter Implementation Class for Harvester Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;

    private List<RRHarvesterConfig> availableRrHarvesterConfigs = null;


    // Application Models
    protected CoRepoHarvesterConfig config = null;

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
        fetchAvailableRrHarvesterConfigs();
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new name value
     */
    @Override
    public void nameChanged(String name) {
        if (config != null) {
            config.getContent().withName(name);
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
     * A signal to the presenter, saying that the resource field has been changed
     *
     * @param resource, the new resource value
     */
    @Override
    public void resourceChanged(String resource) {
        if (config != null) {
            config.getContent().withResource(resource);
        }
    }

    /**
     * A signal to the presenter, saying that the rrHarvester field has been changed
     *
     * @param rrHarvester, the new rrHarvester value
     */
    @Override
    public void rrHarvesterChanged(String rrHarvester) {
        if (config != null) {
            config.getContent().withRrHarvester(Long.valueOf(rrHarvester));
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
    void setCoRepoHarvesterConfig(CoRepoHarvesterConfig config) {
        this.config = config;
    }


    /*
     * Private methods
     */

    private boolean isInputFieldsEmpty(CoRepoHarvesterConfig config) {
        return config == null ||
                config.getContent() == null ||
                config.getContent().getName() == null ||
                config.getContent().getName().isEmpty() ||
                config.getContent().getDescription() == null ||
                config.getContent().getDescription().isEmpty() ||
                config.getContent().getResource() == null ||
                config.getContent().getResource().isEmpty();
    }

    private void initializeView() {
        getView().setHeader(this.header);
        getView().setPresenter(this);
    }

    private void initializeViewFields(
            Boolean viewEnabled,
            String name,
            String description,
            String resource,
            String rrHarvester,
            Boolean enabled) {
        View view = getView();
        view.name.setText(name);
        view.name.setEnabled(viewEnabled);
        view.description.setText(description);
        view.description.setEnabled(viewEnabled);
        view.resource.setText(resource);
        view.resource.setEnabled(viewEnabled);
        view.rrHarvester.clear();
        if (availableRrHarvesterConfigs != null) {
            for (RRHarvesterConfig config : availableRrHarvesterConfigs) {
                view.rrHarvester.addAvailableItem(config.getContent().getId(), String.valueOf(config.getId()));
            }
        }
        view.rrHarvester.setSelectedValue(rrHarvester);
        view.rrHarvester.setEnabled(viewEnabled);
        view.enabled.setValue(enabled);
        view.enabled.setEnabled(viewEnabled);
        view.status.setText("");
    }

    private void initializeViewFields() {
        initializeViewFields(false, "", "", "", "", false);
    }

    void setAvailableRrHarvesterConfigs(List<RRHarvesterConfig> configs) {
        this.availableRrHarvesterConfigs = configs;
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        initializeViewFields(
                true, // Enable all fields
                config.getContent().getName(),
                config.getContent().getDescription(),
                config.getContent().getResource(),
                String.valueOf(config.getContent().getRrHarvester()),
                config.getContent().isEnabled());
    }

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }

    private void fetchAvailableRrHarvesterConfigs() {
        commonInjector.getFlowStoreProxyAsync().findAllRRHarvesterConfigs(new FetchAvailableRRHarvesterConfigsCallback());
    }


    /*
     * Local classes
     */

    /**
     * Local call back class to be instantiated in the call to findAllRRHarvesterConfigs in flowstore proxy
     */
    class FetchAvailableRRHarvesterConfigsCallback extends FilteredAsyncCallback<List<RRHarvesterConfig>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<RRHarvesterConfig> configs) {
            setAvailableRrHarvesterConfigs(configs);
            updateAllFieldsAccordingToCurrentState();
            getView().rrHarvesterChanged(null);  // Be sure that the view is aware of the new list of available RR Harvesters
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


