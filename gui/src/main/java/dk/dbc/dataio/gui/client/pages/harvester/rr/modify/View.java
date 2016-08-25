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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.PopupBox;
import dk.dbc.dataio.gui.client.components.PopupMapEntry;
import dk.dbc.dataio.gui.client.components.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.PromptedPasswordTextBox;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

public class View extends ContentPanel<Presenter> implements IsWidget {
    private final String SEPARATOR = " - ";

    interface HarvesterBinder extends UiBinder<HTMLPanel, View> {}
    private static HarvesterBinder uiBinder = GWT.create(HarvesterBinder.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);


    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }


    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }


    @UiField PromptedTextBox name;
    @UiField PromptedTextBox resource;
    @UiField PromptedTextBox targetUrl;
    @UiField PromptedTextBox targetGroup;
    @UiField PromptedTextBox targetUser;
    @UiField PromptedPasswordTextBox targetPassword;
    @UiField PromptedTextBox consumerId;
    @UiField PromptedTextBox size;
    @UiField PromptedMultiList formatOverrides;
    @UiField PromptedCheckBox relations;
    @UiField PromptedCheckBox libraryRules;
    @UiField PromptedCheckBox imsHarvester;
    @UiField PromptedTextBox imsHoldingsTarget;
    @UiField PromptedTextBox destination;
    @UiField PromptedTextBox format;
    @UiField PromptedTextBox type;
    @UiField PromptedCheckBox enabled;
    @UiField Label status;
    @UiField PopupMapEntry popupFormatOverrideEntry;
    @UiField Button updateButton;
    @UiField Button deleteButton;
    @UiField PopupBox<Label> confirmation;



    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @UiHandler("resource")
    void resourceChanged(ValueChangeEvent<String> event) {
        presenter.resourceChanged(resource.getText());
        presenter.keyPressed();
    }

    @UiHandler("targetUrl")
    void targetUrlChanged(ValueChangeEvent<String> event) {
        presenter.targetUrlChanged(targetUrl.getText());
        presenter.keyPressed();
    }

    @UiHandler("targetGroup")
    void targetGroupChanged(ValueChangeEvent<String> event) {
        presenter.targetGroupChanged(targetGroup.getText());
        presenter.keyPressed();
    }

    @UiHandler("targetUser")
    void targetUserChanged(ValueChangeEvent<String> event) {
        presenter.targetUserChanged(targetUser.getText());
        presenter.keyPressed();
    }

    @UiHandler("targetPassword")
    void targetPasswordChanged(ValueChangeEvent<String> event) {
        presenter.targetPasswordChanged(targetPassword.getText());
        presenter.keyPressed();
    }

    @UiHandler("consumerId")
    void consumerIdChanged(ValueChangeEvent<String> event) {
        presenter.consumerIdChanged(consumerId.getText());
        presenter.keyPressed();
    }

    @UiHandler("size")
    void sizeChanged(ValueChangeEvent<String> event) {
        presenter.sizeChanged(size.getText());
        presenter.keyPressed();
    }

    @UiHandler("relations")
    void relationsChanged(ValueChangeEvent<Boolean> event) {
        presenter.relationsChanged(relations.getValue());
        presenter.keyPressed();
    }

    @UiHandler("libraryRules")
    void libraryRulesChanged(ValueChangeEvent<Boolean> event) {
        presenter.libraryRulesChanged(libraryRules.getValue());
        presenter.keyPressed();
    }

    @UiHandler("imsHarvester")
    void imsHarvesterChanged(ValueChangeEvent<Boolean> event) {
        presenter.imsHarvesterChanged(imsHarvester.getValue());
        presenter.keyPressed();
    }

    @UiHandler("imsHoldingsTarget")
    void imsHoldingsTargetChanged(ValueChangeEvent<String> event) {
        presenter.imsHoldingsTargetChanged(imsHoldingsTarget.getText());
        presenter.keyPressed();
    }

    @UiHandler("destination")
    void destinationChanged(ValueChangeEvent<String> event) {
        presenter.destinationChanged(destination.getText());
        presenter.keyPressed();
    }

    @UiHandler("format")
    void formatChanged(ValueChangeEvent<String> event) {
        presenter.formatChanged(format.getText());
        presenter.keyPressed();
    }

    @UiHandler("type")
    void typeChanged(ValueChangeEvent<String> event) {
        presenter.typeChanged(type.getText());
        presenter.keyPressed();
    }

    @UiHandler("enabled")
    void enabledChanged(ValueChangeEvent<Boolean> event) {
        presenter.enabledChanged(enabled.getValue());
        presenter.keyPressed();
    }

    @UiHandler("popupFormatOverrideEntry")
    void popupTextBoxChanged(ValueChangeEvent<Map.Entry<String, String>> event) {
        String overrideKey = event.getValue().getKey();
        String overrideValue = event.getValue().getValue();
        String error = presenter.formatOverrideAdded(overrideKey, overrideValue);  // First send the addition to the presenter, to assure, that the presenters model is changed accordingly
        if (error != null) {
            setErrorText(error);
        } else {
            formatOverrides.addValue(prepareFormatOverride(overrideKey, overrideValue), overrideKey);  // Then (if successful) add the entry to the Format Overrides list in the view
        }
        presenter.keyPressed();
    }

    @UiHandler("formatOverrides")
    void formatOverridesButtonPressed(ClickEvent event) {
        if (formatOverrides.isAddEvent(event)) {
            presenter.formatOverridesAddButtonPressed();
        } else if (formatOverrides.isRemoveEvent(event)) {
            String itemToRemove = formatOverrides.getSelectedItem();
            removeItemFromView(itemToRemove);  // Remove from View
            presenter.formatOverridesRemoveButtonPressed(itemToRemove);  // Remove from Model in Presenter
        }
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("updateButton")
    void updateButtonPressed(ClickEvent event) {
        presenter.updateButtonPressed();
    }

    public void setFormatOverrides(Map<String, String> formats) {
        this.formatOverrides.clear();
        for (String key: formats.keySet()) {
            this.formatOverrides.addValue(prepareFormatOverride(key, formats.get(key)), key);
        }
    }

    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        confirmation.show();
    }

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.deleteButtonPressed();
        }
    }



    /*
     * Private methods
     */

    private String prepareFormatOverride(String key, String value) {
        return key + SEPARATOR + value;
    }

    private void removeItemFromView(String itemToRemove) {
        Map<String, String> overrides = formatOverrides.getValue();
        overrides.remove(itemToRemove);
        formatOverrides.setValue(overrides);
    }

}
