package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PromptedFileStoreUpload extends PromptedData {
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

    /**
     * Sets the text for the anchor
     * @param text The text value for the anchor
     */
    public void setText(String text) {
        anchor.setText(text);
    }

    /**
     * Fetches the text from the anchor
     * @return The text for the anchor
     */
    public String getText() {
        return anchor.getText();
    }

    /**
     * Sets the Href for the anchor
     * @param href The Href value for the anchor
     */
    public void setHref(String href) {
        if (anchor.getText().isEmpty()) {
            anchor.setText(href);
        }
        anchor.setHref(href);
    }

    public void setHrefAndText( String href) {
        anchor.setText( href );
        anchor.setHref( href );
    }

    /**
     * Fetches the text from the anchor
     * @return The text for the anchor
     */
    public String getHref() {
        return anchor.getHref();
    }

    /**
     * Sets the target for the anchor
     * @param target The target value for the anchor
     */
    public void setTarget(String target) {
        anchor.setTarget(target);
    }

    /**
     * Fetches the target from the anchor
     * @return The target for the anchor
     */
    public String getTarget() {
        return anchor.getTarget();
    }

    public HandlerRegistration addValueChangeHandler(FormPanel.SubmitCompleteHandler handler) {
        return form.addSubmitCompleteHandler(handler);
    }

}
