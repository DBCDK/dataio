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
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.popup.PopupMapEntry;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface HarvesterBinder extends UiBinder<HTMLPanel, View> {
    }

    private static HarvesterBinder uiBinder = GWT.create(HarvesterBinder.class);
    private ViewGinjector viewInjector = GWT.create(ViewGinjector.class);


    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }


    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }


    @UiField
    PromptedTextBox name;
    @UiField
    PromptedTextBox description;
    @UiField
    PromptedTextBox resource;
    @UiField
    PromptedTextBox consumerId;
    @UiField
    PromptedTextBox size;
    @UiField
    PromptedMultiList formatOverrides;
    @UiField
    PromptedCheckBox relations;
    @UiField
    PromptedCheckBox expand;
    @UiField
    PromptedCheckBox libraryRules;
    @UiField
    PromptedList harvesterType;
    @UiField
    PromptedTextBox holdingsTarget;
    @UiField
    PromptedTextBox destination;
    @UiField
    PromptedTextBox format;
    @UiField
    PromptedList type;
    @UiField
    PromptedTextArea note;
    @UiField
    PromptedCheckBox enabled;
    @UiField
    Label status;
    @UiField
    PopupMapEntry popupFormatOverrideEntry;
    @UiField
    Button updateButton;
    @UiField
    Button deleteButton;
    @UiField
    PopupBox<Label> confirmation;


    @SuppressWarnings("unused")
    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("description")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("resource")
    void resourceChanged(ValueChangeEvent<String> event) {
        presenter.resourceChanged(resource.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("consumerId")
    void consumerIdChanged(ValueChangeEvent<String> event) {
        presenter.consumerIdChanged(consumerId.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("size")
    void sizeChanged(ValueChangeEvent<String> event) {
        presenter.sizeChanged(size.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("relations")
    void relationsChanged(ValueChangeEvent<Boolean> event) {
        presenter.relationsChanged(relations.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("expand")
    void expandChanged(ValueChangeEvent<Boolean> event) {
        presenter.expandChanged(expand.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("libraryRules")
    void libraryRulesChanged(ValueChangeEvent<Boolean> event) {
        presenter.libraryRulesChanged(libraryRules.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("harvesterType")
    void imsHarvesterChanged(ValueChangeEvent<String> event) {
        presenter.harvesterTypeChanged(harvesterType.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("holdingsTarget")
    void holdingsTargetChanged(ValueChangeEvent<String> event) {
        presenter.holdingsTargetChanged(holdingsTarget.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("destination")
    void destinationChanged(ValueChangeEvent<String> event) {
        presenter.destinationChanged(destination.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("format")
    void formatChanged(ValueChangeEvent<String> event) {
        presenter.formatChanged(format.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("type")
    void typeChanged(ValueChangeEvent<String> event) {
        presenter.typeChanged(type.getSelectedKey());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("note")
    void noteChanged(ValueChangeEvent<String> event) {
        presenter.noteChanged(note.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("enabled")
    void enabledChanged(ValueChangeEvent<Boolean> event) {
        presenter.enabledChanged(enabled.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("popupFormatOverrideEntry")
    void popupFormatOverrideChanged(ValueChangeEvent<Map.Entry<String, String>> event) {
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("popupFormatOverrideEntry")
    void popupFormatOverrideOkButton(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            String overrideKey = popupFormatOverrideEntry.getValue().getKey();
            String overrideValue = popupFormatOverrideEntry.getValue().getValue();
            String error = presenter.formatOverrideAdded(overrideKey, overrideValue);  // First send the addition to the presenter, to assure, that the presenters model is changed accordingly
            if (error != null) {
                setErrorText(error);
            } else {
                formatOverrides.addValue(prepareFormatOverride(overrideKey, overrideValue), overrideKey);  // Then (if successful) add the entry to the Format Overrides list in the view
            }

        }
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("updateButton")
    void updateButtonPressed(ClickEvent event) {
        presenter.updateButtonPressed();
    }

    void setFormatOverrides(Map<String, String> formats) {
        this.formatOverrides.clear();
        for (String key : formats.keySet()) {
            this.formatOverrides.addValue(prepareFormatOverride(key, formats.get(key)), key);
        }
    }

    @SuppressWarnings("unused")
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
        final String SEPARATOR = " - ";
        return key + SEPARATOR + value;
    }

    private void removeItemFromView(String itemToRemove) {
        Map<String, String> overrides = formatOverrides.getValue();
        overrides.remove(itemToRemove);
        formatOverrides.setValue(overrides);
    }

}
