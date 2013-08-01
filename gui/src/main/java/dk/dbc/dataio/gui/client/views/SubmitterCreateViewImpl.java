package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.presenters.SubmitterCreatePresenter;

public class SubmitterCreateViewImpl extends VerticalPanel implements SubmitterCreateView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String CONTEXT_HEADER = "Submitter - opsætning";
    public static final String GUIID_SUBMITTER_CREATION_WIDGET = "submittercreationwidget";
    public static final String GUIID_SUBMITTER_CREATION_NAME_TEXT_BOX = "submittercreationnametextbox";
    public static final String GUIID_SUBMITTER_CREATION_NUMBER_TEXT_BOX = "submittercreationnumbertextbox";
    public static final String GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA = "submittercreationdescriptiontextarea";
    public static final String GUIID_SUBMITTER_CREATION_SAVE_BUTTON = "submittercreationsavebutton";
    public static final String GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL = "submittercreationsaveresultlabel";
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    public static final String SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    private static final int SUBMITTER_CREATION_DESCRIPTION_MAX_LENGTH = 160;
    
    // Local variables
    private SubmitterCreatePresenter presenter;
    private final SubmitterCreateViewImpl.SubmitterNamePanel submitterNamePanel = new SubmitterCreateViewImpl.SubmitterNamePanel();
    private final SubmitterCreateViewImpl.SubmitterNumberPanel submitterNumberPanel = new SubmitterCreateViewImpl.SubmitterNumberPanel();
    private final SubmitterCreateViewImpl.SubmitterDescriptionPanel submitterDescriptionPanel = new SubmitterCreateViewImpl.SubmitterDescriptionPanel();
    private final SubmitterCreateViewImpl.SubmitterSavePanel submitterSavePanel = new SubmitterCreateViewImpl.SubmitterSavePanel();
    
    public SubmitterCreateViewImpl() {
        getElement().setId(GUIID_SUBMITTER_CREATION_WIDGET);
        add(submitterNamePanel);
        add(submitterNumberPanel);
        add(submitterDescriptionPanel);
        add(submitterSavePanel);
    }

    @Override
    public void setPresenter(SubmitterCreatePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setData(String name, String description) {
        // set data
    }

    @Override
    public void displayError(String message) {
        Window.alert("Error: " + message);
    }

    @Override
    public void displaySuccess(String message) {
        submitterSavePanel.setStatusText(message);
    }

    @Override
    public void refresh() {
    }

    private class SubmitterNamePanel extends HorizontalPanel {

        private final Label label = new Label("Submitternavn");
        private final TextBox textBox = new TextBox();

        public SubmitterNamePanel() {
            super();
            add(label);
            textBox.getElement().setId(GUIID_SUBMITTER_CREATION_NAME_TEXT_BOX);
            textBox.addKeyDownHandler(new SubmitterCreateViewImpl.InputFieldKeyDownHandler());
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }

    private class SubmitterNumberPanel extends HorizontalPanel {

        private final Label label = new Label("Submitternummer");
        private final TextBox textBox = new TextBox();

        public SubmitterNumberPanel() {
            super();
            add(label);
            textBox.getElement().setId(GUIID_SUBMITTER_CREATION_NUMBER_TEXT_BOX);
            textBox.addKeyDownHandler(new SubmitterCreateViewImpl.InputFieldKeyDownHandler());
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }

    private class SubmitterDescriptionPanel extends HorizontalPanel {

        private final Label submitterDescriptionLabel = new Label("Beskrivelse");
        private final TextArea submitterDescriptionTextArea = new SubmitterDescriptionTextArea();

        public SubmitterDescriptionPanel() {
            add(submitterDescriptionLabel);
            add(submitterDescriptionTextArea);
        }

        public String getText() {
            return submitterDescriptionTextArea.getValue();
        }

        private class SubmitterDescriptionTextArea extends TextArea {

            public SubmitterDescriptionTextArea() {
                super();
                setCharacterWidth(40);
                setVisibleLines(4);
                getElement().setAttribute("Maxlength", String.valueOf(SUBMITTER_CREATION_DESCRIPTION_MAX_LENGTH));
                getElement().setId(GUIID_SUBMITTER_CREATION_DESCRIPTION_TEXT_AREA);
                addKeyDownHandler(new SubmitterCreateViewImpl.InputFieldKeyDownHandler());
            }
        }
    }

    private class SubmitterSavePanel extends HorizontalPanel {

        private final Button submitterSaveButton = new Button("Gem");
        private final Label submitterSaveResultLabel = new Label("");

        public SubmitterSavePanel() {
            submitterSaveResultLabel.getElement().setId(GUIID_SUBMITTER_CREATION_SAVE_RESULT_LABEL);
            add(submitterSaveResultLabel);
            submitterSaveButton.getElement().setId(GUIID_SUBMITTER_CREATION_SAVE_BUTTON);
            submitterSaveButton.addClickHandler(new SubmitterCreateViewImpl.SaveButtonHandler());
            add(submitterSaveButton);
        }

        public void setStatusText(String statusText) {
            submitterSaveResultLabel.setText(statusText);
        }
    }

    private class SaveButtonHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            String nameValue = submitterNamePanel.getText();
            String numberValue = submitterNumberPanel.getText();
            String descriptionValue = submitterDescriptionPanel.getText();
            if (!nameValue.isEmpty() && !numberValue.isEmpty() && !descriptionValue.isEmpty()) {
                presenter.saveSubmitter(submitterNamePanel.getText(), submitterNumberPanel.getText(), submitterDescriptionPanel.getText());
            } else {
                Window.alert(SUBMITTER_CREATION_INPUT_FIELD_VALIDATION_ERROR);
            }
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {

        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            submitterSavePanel.setStatusText("");
        }
    }
}
