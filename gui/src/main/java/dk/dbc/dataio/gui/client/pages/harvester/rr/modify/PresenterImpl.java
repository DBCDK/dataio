/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    protected RRHarvesterConfig config = null;

    public PresenterImpl(String header) {
        this.header = header;
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
        initializeView();
        initializeViewFields();
        containerWidget.setWidget(getView().asWidget());
        initializeModel();
    }

    /**
     * A signal to the presenter, saying that the name field has been changed
     * @param name, the new name value
     */
    @Override
    public void nameChanged(String name) {
        if (config != null) {
            config.getContent().withId(name);
        }
    }

    /**
     * A signal to the presenter, saying that the description field has been changed
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
     * @param resource, the new resource value
     */
    @Override
    public void resourceChanged(String resource) {
        if (config != null) {
            config.getContent().withResource(resource);
        }
    }

    /**
     * A signal to the presenter, saying that the targetUrl field has been changed
     * @param targetUrl, the new targetUrl value
     */
    @Override
    public void targetUrlChanged(String targetUrl) {
        if (config != null) {
            config.getContent().getOpenAgencyTarget().setUrl(targetUrl);
        }
    }

    /**
     * A signal to the presenter, saying that the targetGroup field has been changed
     * @param targetGroup, the new targetGroup value
     */
    @Override
    public void targetGroupChanged(String targetGroup) {
        if (config != null) {
            config.getContent().getOpenAgencyTarget().setGroup(targetGroup);
        }
    }

    /**
     * A signal to the presenter, saying that the targetUser field has been changed
     * @param targetUser, the new targetUser value
     */
    @Override
    public void targetUserChanged(String targetUser) {
        if (config != null) {
            config.getContent().getOpenAgencyTarget().setUser(targetUser);
        }
    }

    /**
     * A signal to the presenter, saying that the targetPassword field has been changed
     * @param targetPassword, the new targetPassword value
     */
    @Override
    public void targetPasswordChanged(String targetPassword) {
        if (config != null) {
            config.getContent().getOpenAgencyTarget().setPassword(targetPassword);
        }
    }

    /**
     * A signal to the presenter, saying that the consumerId field has been changed
     * @param consumerId, the new consumerId value
     */
    @Override
    public void consumerIdChanged(String consumerId) {
        if (config != null) {
            config.getContent().withConsumerId(consumerId);
        }
    }

    /**
     * A signal to the presenter, saying that the size field has been changed
     * @param size, the new size value
     */
    @Override
    public void sizeChanged(String size) {
        if (config != null) {
            config.getContent().withBatchSize(Integer.valueOf(size));
        }
    }

    /**
     * A signal to the presenter, saying that an entry has been added to formatOverrides
     * @param overrideKey The key value of the FormatOverride
     * @param overrideValue The text value of the FormatOverride
     * @return True if successful, false if number entry error
     */
    @Override
    public String formatOverrideAdded(String overrideKey, String overrideValue) {
        if (config != null) {
            if (overrideKey == null || overrideKey.isEmpty() || overrideValue == null || overrideValue.isEmpty()) {
                return getTexts().error_InputFieldValidationError();
            }
            try {
                config.getContent().withFormatOverridesEntry(Integer.valueOf(overrideKey), overrideValue);
                return null;
            } catch (Exception e) {
                return getTexts().error_NumericSubmitterValidationError();
            }
        }
        return getTexts().error_InputFieldValidationError();
    }

    /**
     * A signal to the presenter, saying that the relations field has been changed
     * @param relations, the new relations value
     */
    @Override
    public void relationsChanged(Boolean relations) {
        if (config != null) {
            config.getContent().withIncludeRelations(relations);
        }
    }

    /**
     * A signal to the presenter, saying that the library rules field has been changed
     * @param libraryRules, the new library rules value
     */
    @Override
    public void libraryRulesChanged(Boolean libraryRules) {
        if (config != null) {
            config.getContent().withIncludeLibraryRules(libraryRules);
        }
    }

    /**
     * A signal to the presenter, saying that the IMS Harvester field has been changed
     * @param imsHarvester, the new IMS Harvester value
     */
    @Override
    public void imsHarvesterChanged(Boolean imsHarvester) {
        if (config != null) {
            if(imsHarvester) {
                config.getContent().withHarvesterType(RRHarvesterConfig.HarvesterType.IMS);
                getView().worldCatHarvester.setValue(false);
                getView().imsHoldingsTarget.setEnabled(imsHarvester);
            } else {
                config.getContent().withHarvesterType(RRHarvesterConfig.HarvesterType.STANDARD);
            }
        }
        getView().imsHoldingsTarget.setEnabled(imsHarvester);
    }

    /**
     * A signal to the presenter, saying that the WorldCat Harvester field has been changed
     * @param worldCatHarvester, the new WorldCat Harvester value
     */
    @Override
    public void worldCatHarvesterChanged(Boolean worldCatHarvester) {
        if (config != null) {
            if (worldCatHarvester) {
                config.getContent().withHarvesterType(RRHarvesterConfig.HarvesterType.WORLDCAT);
                getView().imsHarvester.setValue(false);
            } else {
                config.getContent().withHarvesterType(RRHarvesterConfig.HarvesterType.STANDARD);
            }
        }
    }

    /**
     * A signal to the presenter, saying that the IMS Holdings Target field has been changed
     * @param imsHoldingsTarget, the new IMS Holdings Target value
     */
    @Override
    public void imsHoldingsTargetChanged(String imsHoldingsTarget) {
        if (config != null) {
            config.getContent().withImsHoldingsTarget(imsHoldingsTarget);
        }
    }

    /**
     * A signal to the presenter, saying that the destination field has been changed
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
     * @param type, the new type value
     */
    @Override
    public void typeChanged(String type) {
        if (config != null) {
            config.getContent().withType(JobSpecification.Type.valueOf(type));
        }
    }

    /**
     * A signal to the presenter, saying that the note field has been changed
     * @param note, the new note value
     */
    @Override
    public void noteChanged(String note) {
        if (config != null) {
            config.getContent().withNote(note);
        }
    }

    /**
     * A signal to the presenter, saying that the enabled field has been changed
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

    /**
     * A signal to the presenter, saying that the update button has been pressed
     */
    @Override
    public void updateButtonPressed() {
        /*
         * As input, we do have an instance of the OLDRRHarvesterConfig object (in the member data: config)
         * We want to change this to a RRHarvesterConfig object, therefore we create a new instance, and copy the dato from the old one into it.
         */
        config = new RRHarvesterConfig(config.getId(), config.getVersion(), config.getContent()); // Overwrite the old config with an RRHarvesterConfig - not an OLDRRHarvesterConfig object
        saveButtonPressed(); // Now do save it - simulate a push on the save button
    }

    /**
     * A signal to the presenter, saying that the add button on the Format Overrides list has been pressed
     */
    @Override
    public void formatOverridesAddButtonPressed() {
        if (config != null) {
            getView().popupFormatOverrideEntry.show();
        }
    }

    /**
     * Removes a FormatOverride from the list of FormatOverride's
     * @param item The item, that has been removed
     */
    @Override
    public void formatOverridesRemoveButtonPressed(String item) {
        if (config != null) {
            removeFormatOverrideFromModel(item);
        }
    }

    /*
     * Protected methods
     */

    /**
     * Method used to set the model after a successful update or a save
     * @param config The config to save
     */
    protected void setRRHarvesterConfig(RRHarvesterConfig config) {
        this.config = config;
    }

    /*
     * Private methods
     */

    private boolean isInputFieldsEmpty(RRHarvesterConfig config) {
        return  config == null ||
                config.getContent() == null ||
                config.getContent().getId() == null ||
                config.getContent().getId().isEmpty() ||
                config.getContent().getDescription() == null ||
                config.getContent().getDescription().isEmpty() ||
                config.getContent().getResource() == null ||
                config.getContent().getResource().isEmpty() ||
                config.getContent().getOpenAgencyTarget() == null ||
                config.getContent().getOpenAgencyTarget().getUrl() == null ||
                config.getContent().getOpenAgencyTarget().getUrl().isEmpty() ||
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
        for (Integer key: integerStringMap.keySet()) {
            stringStringMap.put(key == null ? "" : String.valueOf(key), integerStringMap.get(key) == null ? "" : integerStringMap.get(key));
        }
        return stringStringMap;
    }

    private void removeFormatOverrideFromModel(String item) {
        config.getContent().getFormatOverrides().remove(Integer.valueOf(item));
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
            String targetUrl,
            String targetGroup,
            String targetUser,
            String targetPassword,
            String consumerId,
            String size,
            Map<String, String> formatOverrides,
            Boolean relations,
            Boolean libraryRules,
            Boolean harvesterType,
            Boolean worldCatHarvester,
            String imsHoldingsTarget,
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
        view.targetUrl.setText(targetUrl);
        view.targetUrl.setEnabled(viewEnabled);
        view.targetGroup.setText(targetGroup);
        view.targetGroup.setEnabled(viewEnabled);
        view.targetUser.setText(targetUser);
        view.targetUser.setEnabled(viewEnabled);
        view.targetPassword.setText(targetPassword);
        view.targetPassword.setEnabled(viewEnabled);
        view.consumerId.setText(consumerId);
        view.consumerId.setEnabled(viewEnabled);
        view.size.setText(size);
        view.size.setEnabled(viewEnabled);
        view.setFormatOverrides(formatOverrides);
        view.formatOverrides.setEnabled(viewEnabled);
        view.relations.setValue(relations);
        view.relations.setEnabled(viewEnabled);
        view.libraryRules.setValue(libraryRules);
        view.libraryRules.setEnabled(viewEnabled);
        view.imsHarvester.setValue(harvesterType);
        view.imsHarvester.setEnabled(viewEnabled);
        view.imsHoldingsTarget.setText(imsHoldingsTarget);
        view.imsHoldingsTarget.setEnabled(harvesterType && viewEnabled);
        view.worldCatHarvester.setEnabled(viewEnabled);
        view.worldCatHarvester.setValue(worldCatHarvester);
        view.destination.setText(destination);
        view.destination.setEnabled(viewEnabled);
        view.format.setText(format);
        view.format.setEnabled(viewEnabled);
        view.type.clear();
        for (JobSpecification.Type t: JobSpecification.Type.values()) {
            view.type.addAvailableItem(t.toString());
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
        initializeViewFields(false, "", "", "", "", "", "", "", "", "", new HashMap<>(), false, false, false, false, "", "", "", "", "", false, false);
    }

    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    protected void updateAllFieldsAccordingToCurrentState() {
        Map<String, String> viewOverrides = integerStringMap2StringStringMap(config.getContent().getFormatOverrides());
        initializeViewFields(
                true, // Enable all fields
                config.getContent().getId(),
                config.getContent().getDescription(),
                config.getContent().getResource(),
                config.getContent().getOpenAgencyTarget().getUrl(),
                config.getContent().getOpenAgencyTarget().getGroup(),
                config.getContent().getOpenAgencyTarget().getUser(),
                config.getContent().getOpenAgencyTarget().getPassword(),
                config.getContent().getConsumerId(),
                String.valueOf(config.getContent().getBatchSize()),
                viewOverrides,
                config.getContent().hasIncludeRelations(),
                config.getContent().hasIncludeLibraryRules(),
                config.getContent().getHarvesterType() == RRHarvesterConfig.HarvesterType.IMS ? true : false,
                config.getContent().getHarvesterType() == RRHarvesterConfig.HarvesterType.WORLDCAT ? true : false,
                config.getContent().getImsHoldingsTarget(),
                config.getContent().getDestination(),
                config.getContent().getFormat(),
                config.getContent().getType().toString(),
                config.getContent().getNote(),
                config.getContent().isEnabled(),
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


