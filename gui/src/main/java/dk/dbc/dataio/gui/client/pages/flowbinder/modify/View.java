package dk.dbc.dataio.gui.client.pages.flowbinder.modify;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.CheckBoxEntry;
import dk.dbc.dataio.gui.client.components.DualListEntry;
import dk.dbc.dataio.gui.client.components.ListEntry;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Collection;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface FlowbinderBinder extends UiBinder<HTMLPanel, View> {}
    private static FlowbinderBinder uiBinder = GWT.create(FlowbinderBinder.class);

    public View(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
    }

    @Override
    public void init() {
    }

    @UiField TextEntry name;
    @UiField TextAreaEntry description;
    @UiField TextEntry frame;
    @UiField TextEntry format;
    @UiField TextEntry charset;
    @UiField TextEntry destination;
    @UiField TextEntry recordSplitter;
    @UiField CheckBoxEntry sequenceAnalysis;
    @UiField DualListEntry submitters;
    @UiField ListEntry flow;
    @UiField ListEntry sink;
    @UiField Label status;

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

    @UiHandler("recordSplitter")
    void recordSplitterChanged(BlurEvent event) {
        presenter.recordSplitterChanged(recordSplitter.getText());
    }

    @UiHandler("sequenceAnalysis")
    void sequenceAnalysisChanged(ValueChangeEvent<Boolean> event) {
        presenter.sequenceAnalysisChanged(sequenceAnalysis.getValue());
        presenter.keyPressed();
    }

    @UiHandler("submitters")
    void submittersChanged(ValueChangeEvent<Collection<String>> event) {
        presenter.submittersChanged(submitters.getSelectedItems());
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

    @UiHandler("name")
    void keyPressedInNameField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("description")
    void keyPressedInDescriptionField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("frame")
    void keyPressedInFrameField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("format")
    void keyPressedInFormatField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("charset")
    void keyPressedInCharsetField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("destination")
    void keyPressedInDestinationField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("recordSplitter")
    void keyPressedInRecordsplitterField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

}
