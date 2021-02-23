package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PromptedFileStoreUpload extends PromptedData {
    private final static String PROMPTEDFILESSTOREUPLOAD_REMOVE_BUTTON = "dio-PromptedFilesStoreUpload-remove-button";
    final VerticalPanel panel = new VerticalPanel();
    final FormPanel form = new FormPanel();
    final FileUpload fileUpload = new FileUpload();
    final Button uploadButton = new Button("Upload fil");
    final Button removeButton = new Button("Fjern fil");
    final Anchor anchor = new Anchor();

    @UiConstructor
    public PromptedFileStoreUpload(String guiId, String prompt) {
        super(guiId, prompt);
        anchor.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(anchor);
        removeButton.addStyleName(PROMPTEDFILESSTOREUPLOAD_REMOVE_BUTTON);
        add(removeButton);

        form.setAction("/addfile");
        form.setEncoding(FormPanel.ENCODING_MULTIPART);
        form.setMethod(FormPanel.METHOD_POST);
        panel.add(fileUpload);
        panel.add(uploadButton);

        uploadButton.addClickHandler(event -> {
            String filename = fileUpload.getFilename();
            if (filename.length() == 0) {
                Window.alert("No File Specified!");
            } else {
                form.submit();
            }
        });

        panel.setSpacing(10);

        form.add(panel);
        add(form);
    }

    public void setFileStoreLink(String href) {
        anchor.setText(href);
        anchor.setHref(href);
    }

    public HandlerRegistration addSubmitCompleteHandler(FormPanel.SubmitCompleteHandler handler) {
        return form.addSubmitCompleteHandler(handler);
    }

    public HandlerRegistration addRemoveFileClickHandler(ClickHandler handler) {
        return removeButton.addClickHandler(handler);
    }

}
