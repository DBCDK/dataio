package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

import java.util.HashMap;
import java.util.Map;


/**
 * Abstract Presenter Implementation Class for Harvester Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;

    // Application Models
    protected RRHarvesterConfig model = null;

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
     * A signal to the presenter, saying that the name field has been changed
     *
     * @param name, the new name value
     */
    @Override
    public void nameChanged(String name) {
        if (model != null) {
            model.getContent().withId(name);
        }
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
     *
     * @param description, the new description value
     */
    @Override
    public void descriptionChanged(String description) {
        if (model != null) {
            model.getContent().withDescription(description);
        }
    }

    /**
     * A signal to the presenter, saying that the resource field has been changed
     *
     * @param resource, the new resource value
     */
    @Override
    public void resourceChanged(String resource) {
        if (model != null) {
            model.getContent().withResource(resource);
        }
    }

    /**
     * A signal to the presenter, saying that the consumerId field has been changed
     *
     * @param consumerId, the new consumerId value
     */
    @Override
    public void consumerIdChanged(String consumerId) {
        if (model != null) {
            model.getContent().withConsumerId(consumerId);
        }
    }

    /**
     * A signal to the presenter, saying that the size field has been changed
     *
     * @param size, the new size value
     */
    @Override
    public void sizeChanged(String size) {
        if (model != null) {
            model.getContent().withBatchSize(Integer.valueOf(size));
        }
    }

    /**
     * A signal to the presenter, saying that an entry has been added to formatOverrides
     *
     * @param overrideKey   The key value of the FormatOverride
     * @param overrideValue The text value of the FormatOverride
     * @return True if successful, false if number entry error
     */
    @Override
    public String formatOverrideAdded(String overrideKey, String overrideValue) {
        if (model != null) {
            if (overrideKey == null || overrideKey.isEmpty() || overrideValue == null || overrideValue.isEmpty()) {
                return getTexts().error_InputFieldValidationError();
            }
            try {
                model.getContent().withFormatOverridesEntry(Integer.valueOf(overrideKey), overrideValue);
                return null;
            } catch (Exception e) {
                return getTexts().error_NumericSubmitterValidationError();
            }
        }
        return getTexts().error_InputFieldValidationError();
    }

    /**
     * A signal to the presenter, saying that the relations field has been changed
     *
     * @param relations, the new relations value
     */
    @Override
    public void relationsChanged(Boolean relations) {
        if (model != null) {
            model.getContent().withIncludeRelations(relations);
        }
    }

    @Override
    public void expandChanged(Boolean expand) {
        if (model != null) {
            model.getContent().withExpand(expand);
        }
    }

    /**
     * A signal to the presenter, saying that the library rules field has been changed
     *
     * @param libraryRules, the new library rules value
     */
    @Override
    public void libraryRulesChanged(Boolean libraryRules) {
        if (model != null) {
            model.getContent().withIncludeLibraryRules(libraryRules);
        }
    }

    /**
     * A signal to the presenter, saying that the Harvester Type field has been changed
     *
     * @param harvesterType, the new Harvester Type value
     */
    @Override
    public void harvesterTypeChanged(String harvesterType) {
        if (model != null && harvesterType != null) {
            model.getContent().withHarvesterType(RRHarvesterConfig.HarvesterType.valueOf(harvesterType));
        }
    }

    /**
     * A signal to the presenter, saying that the Holdings Target field has been changed
     *
     * @param holdingsTarget, the new Holdings Target value
     */
    @Override
    public void holdingsTargetChanged(String holdingsTarget) {
        if (model != null) {
            model.getContent().withImsHoldingsTarget(holdingsTarget);
        }
    }

    /**
     * A signal to the presenter, saying that the destination field has been changed
     *
     * @param destination, the new destination value
     */
    @Override
    public void destinationChanged(String destination) {
        if (model != null) {
            model.getContent().withDestination(destination);
        }
    }

    /**
     * A signal to the presenter, saying that the format field has been changed
     *
     * @param format, the new format value
     */
    @Override
    public void formatChanged(String format) {
        if (model != null) {
            model.getContent().withFormat(format);
        }
    }

    /**
     * A signal to the presenter, saying that the type field has been changed
     *
     * @param type, the new type value
     */
    @Override
    public void typeChanged(String type) {
        if (model != null) {
            model.getContent().withType(JobSpecification.Type.valueOf(type));
        }
    }

    /**
     * A signal to the presenter, saying that the note field has been changed
     *
     * @param note, the new note value
     */
    @Override
    public void noteChanged(String note) {
        if (model != null) {
            model.getContent().withNote(note);
        }
    }

    /**
     * A signal to the presenter, saying that the enabled field has been changed
     *
     * @param enabled, the new enabled value
     */
    @Override
    public void enabledChanged(Boolean enabled) {
        if (model != null) {
            model.getContent().withEnabled(enabled);
        }
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        if (model != null) {
            getView().status.setText("");
        }
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (isInputFieldsEmpty(model)) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
        } else {
            saveModel();
        }
    }

    /**
     * A signal to the presenter, saying that the update button has been pressed
     */
    @Override
    public void updateButtonPressed() {
        /*
         * As input, we do have an instance of the OLDRRHarvesterConfig object (in the member data: model)
         * We want to change this to a RRHarvesterConfig object, therefore we create a new instance, and copy the dato from the old one into it.
         */
        model = new RRHarvesterConfig(model.getId(), model.getVersion(), model.getContent()); // Overwrite the old model with an RRHarvesterConfig - not an OLDRRHarvesterConfig object
        saveButtonPressed(); // Now do save it - simulate a push on the save button
    }

    /**
     * A signal to the presenter, saying that the add button on the Format Overrides list has been pressed
     */
    @Override
    public void formatOverridesAddButtonPressed() {
        if (model != null) {
            getView().popupFormatOverrideEntry.show();
        }
    }

    /**
     * Removes a FormatOverride from the list of FormatOverride's
     *
     * @param item The item, that has been removed
     */
    @Override
    public void formatOverridesRemoveButtonPressed(String item) {
        if (model != null) {
            removeFormatOverrideFromModel(item);
        }
    }

    /*
     * Protected methods
     */

    /**
     * Method used to set the model after a successful update or a save
     *
     * @param config The model to save
     */
    protected void setRRHarvesterConfig(RRHarvesterConfig config) {
        this.model = config;
    }

    /*
     * Private methods
     */

    private boolean isInputFieldsEmpty(RRHarvesterConfig config) {
        return config == null ||
                config.getContent() == null ||
                config.getContent().getId() == null ||
                config.getContent().getId().isEmpty() ||
                config.getContent().getDescription() == null ||
                config.getContent().getDescription().isEmpty() ||
                config.getContent().getResource() == null ||
                config.getContent().getResource().isEmpty() ||
                config.getContent().getConsumerId() == null ||
                config.getContent().getConsumerId().isEmpty() ||
                config.getContent().getHarvesterType() == null &&
                        (config.getContent().getImsHoldingsTarget() == null ||
                                config.getContent().getImsHoldingsTarget().isEmpty()) ||
                config.getContent().getDestination() == null ||
                config.getContent().getDestination().isEmpty() ||
                config.getContent().getFormat() == null ||
                config.getContent().getFormat().isEmpty();
    }

    private Map<String, String> integerStringMap2StringStringMap(Map<Integer, String> integerStringMap) {
        Map<String, String> stringStringMap = new HashMap<>();
        for (Integer key : integerStringMap.keySet()) {
            stringStringMap.put(key == null ? "" : String.valueOf(key), integerStringMap.get(key) == null ? "" : integerStringMap.get(key));
        }
        return stringStringMap;
    }

    private void removeFormatOverrideFromModel(String item) {
        model.getContent().getFormatOverrides().remove(Integer.valueOf(item));
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
            String consumerId,
            String size,
            Map<String, String> formatOverrides,
            Boolean relations,
            Boolean expand,
            Boolean libraryRules,
            RRHarvesterConfig.HarvesterType harvesterType,
            String holdingsTarget,
            String destination,
            String format,
            String type,
            String note,
            Boolean enabled,
            Boolean updateButtonVisible) {
        View view = getView();
        view.name.setText(name);
        view.name.setEnabled(viewEnabled);
        view.description.setText(description);
        view.description.setEnabled(viewEnabled);
        view.resource.setText(resource);
        view.resource.setEnabled(viewEnabled);
        view.consumerId.setText(consumerId);
        view.consumerId.setEnabled(viewEnabled);
        view.size.setText(size);
        view.size.setEnabled(viewEnabled);
        view.setFormatOverrides(formatOverrides);
        view.formatOverrides.setEnabled(viewEnabled);
        view.relations.setValue(relations);
        view.relations.setEnabled(viewEnabled);
        view.expand.setValue(expand);
        view.expand.setEnabled(viewEnabled);
        view.libraryRules.setValue(libraryRules);
        view.libraryRules.setEnabled(viewEnabled);
        view.harvesterType.clear();
        for (RRHarvesterConfig.HarvesterType t : RRHarvesterConfig.HarvesterType.values()) {
            view.harvesterType.addAvailableItem(t.toString());
        }
        view.harvesterType.setSelectedValue(harvesterType.toString());
        view.harvesterType.setEnabled(viewEnabled);
        view.holdingsTarget.setText(holdingsTarget);
        view.holdingsTarget.setEnabled(viewEnabled);
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
        view.note.setText(note);
        view.note.setEnabled(viewEnabled);
        view.enabled.setValue(enabled);
        view.enabled.setEnabled(viewEnabled);
        view.updateButton.setVisible(updateButtonVisible);
        view.status.setText("");
        view.popupFormatOverrideEntry.hide();
    }

    private void initializeViewFields() {
        initializeViewFields(false, "", "", "", "", "", new HashMap<>(),
                false, false, false, RRHarvesterConfig.HarvesterType.STANDARD, "",
                "", "", "", "", false, false);
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        Map<String, String> viewOverrides = integerStringMap2StringStringMap(model.getContent().getFormatOverrides());
        initializeViewFields(
                true, // Enable all fields
                model.getContent().getId(),
                model.getContent().getDescription(),
                model.getContent().getResource(),
                model.getContent().getConsumerId(),
                String.valueOf(model.getContent().getBatchSize()),
                viewOverrides,
                model.getContent().hasIncludeRelations(),
                model.getContent().expand(),
                model.getContent().hasIncludeLibraryRules(),
                model.getContent().getHarvesterType(),
                model.getContent().getImsHoldingsTarget(),
                model.getContent().getDestination(),
                model.getContent().getFormat(),
                model.getContent().getType().toString(),
                model.getContent().getNote(),
                model.getContent().isEnabled(),
                false);
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


