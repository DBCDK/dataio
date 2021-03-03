package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PromptedFileStoreUpload extends PromptedData {
    final VerticalPanel panel = new VerticalPanel();
    final FormPanel form = new FormPanel();
    final FileUpload fileUpload = new FileUpload();
    final Button uploadButton = new Button("Upload fil");

    @UiConstructor
    public PromptedFileStoreUpload(String guiId, String prompt) {
        super(guiId, prompt);

        form.setAction("/dk.dbc.dataio.gui.Main/FileStoreAddFile");
        form.setEncoding(FormPanel.ENCODING_MULTIPART);
        form.setMethod(FormPanel.METHOD_POST);
        // Name is not used for anything as the file it save to filestore
        // But if the value isn't set it simply doesn't work
        fileUpload.setName("PromptedFileStoreUpload");
        panel.add(fileUpload);
        panel.add(uploadButton);

        uploadButton.addClickHandler(event -> {
            String filename = fileUpload.getFilename();
            if (filename.length() == 0) {
                Window.alert("Der er ikke valgt en fil");
            } else {
                form.submit();
            }
        });

        form.add(panel);
        add(form);
    }

    public HandlerRegistration addSubmitCompleteHandler(FormPanel.SubmitCompleteHandler handler) {
        return form.addSubmitCompleteHandler(handler);
    }

}
