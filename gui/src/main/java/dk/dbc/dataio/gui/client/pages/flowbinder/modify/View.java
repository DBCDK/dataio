package dk.dbc.dataio.gui.client.pages.flowbinder.modify;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
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
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface FlowbinderBinder extends UiBinder<HTMLPanel, View> {
    }

    private static FlowbinderBinder uiBinder = GWT.create(FlowbinderBinder.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

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
    PromptedTextArea description;
    @UiField
    PromptedTextBox frame;
    @UiField
    PromptedTextBox format;
    @UiField
    PromptedTextBox charset;
    @UiField
    PromptedTextBox destination;
    @UiField
    PromptedList priority;
    @UiField
    PromptedList recordSplitter;
    @UiField
    PromptedMultiList submitters;
    @UiField
    PromptedList flow;
    @UiField
    PromptedList sink;
    @UiField
    HTMLPanel updateSinkSection;
    @UiField
    PromptedList queueProvider;
    @UiField
    Button deleteButton;
    @UiField
    Label status;
    @UiField
    PopupListBox popupListBox;
    @UiField
    PopupBox<Label> confirmation;

    @UiHandler("name")
    void nameChanged(BlurEvent event) {
        presenter.nameChanged(name.getText());
    }

    @UiHandler("description")
    void descriptionChanged(BlurEvent event) {
        presenter.descriptionChanged(description.getText());
    }

    @UiHandler("frame")
    void frameChanged(BlurEvent event) {
        presenter.frameChanged(frame.getText());
    }

    @UiHandler("format")
    void formatChanged(BlurEvent event) {
        presenter.formatChanged(format.getText());
    }

    @UiHandler("charset")
    void charsetChanged(BlurEvent event) {
        presenter.charsetChanged(charset.getText());
    }

    @UiHandler("destination")
    void destinationChanged(BlurEvent event) {
        presenter.destinationChanged(destination.getText());
    }

    @UiHandler("priority")
    void priorityChanged(ValueChangeEvent<String> event) {
        presenter.priorityChanged(priority.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("recordSplitter")
    void recordSplitterChanged(ValueChangeEvent<String> event) {
        presenter.recordSplitterChanged(recordSplitter.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("submitters")
    void submittersClicked(ClickEvent event) {
        if (submitters.isAddEvent(event)) {
            popupListBox.show();
        }
        if (submitters.isRemoveEvent(event)) {
            presenter.removeSubmitter(submitters.getSelectedItem());
        }
    }

    @UiHandler("submitters")
    void submittersChanged(ValueChangeEvent<Map<String, String>> event) {
        presenter.submittersChanged(submitters.getValue());
        presenter.keyPressed();
    }

    @UiHandler("flow")
    void flowChanged(ValueChangeEvent<String> event) {
        presenter.flowChanged(flow.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("sink")
    void sinkChanged(ValueChangeEvent<String> event) {
        presenter.sinkChanged(sink.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("queueProvider")
    void queueProviderChanged(ValueChangeEvent<String> event) {
        presenter.queueProviderChanged(queueProvider.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler({"name", "description", "frame", "format", "charset", "destination"})
    void keyPressedInField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        confirmation.show();
    }

    @UiHandler("popupListBox")
    void setPopupListBoxClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.addSubmitters(popupListBox.getValue());
        }
    }

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.deleteButtonPressed();
        }
    }

}
