package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;

/**
 * This class is a Anchor widget extended with a prompt label
 * The class extends the PromptedData class, that ensures a uniform presence
 */
public class PromptedAnchor extends PromptedData {

    @UiField
    final Anchor anchor = new Anchor();


    /**
     * Constructor
     * This is the @UiConstructor, meaning that the two parameters are mandatory inputs, when used by UiBinder
     *
     * @param guiId  The GUI Id
     * @param prompt The prompt label for the widget
     */
    @UiConstructor
    public PromptedAnchor(String guiId, String prompt) {
        super(guiId, prompt);
        anchor.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(anchor);
    }

    /**
     * Sets the text for the anchor
     *
     * @param text The text value for the anchor
     */
    public void setText(String text) {
        anchor.setText(text);
    }

    /**
     * Fetches the text from the anchor
     *
     * @return The text for the anchor
     */
    public String getText() {
        return anchor.getText();
    }

    /**
     * Sets the Href for the anchor
     *
     * @param href The Href value for the anchor
     */
    public void setHref(String href) {
        if (anchor.getText().isEmpty()) {
            anchor.setText(href);
        }
        anchor.setHref(href);
    }

    public void setHrefAndText(String href) {
        anchor.setText(href);
        anchor.setHref(href);
    }

    /**
     * Fetches the text from the anchor
     *
     * @return The text for the anchor
     */
    public String getHref() {
        return anchor.getHref();
    }

    /**
     * Sets the target for the anchor
     *
     * @param target The target value for the anchor
     */
    public void setTarget(String target) {
        anchor.setTarget(target);
    }

    /**
     * Fetches the target from the anchor
     *
     * @return The target for the anchor
     */
    public String getTarget() {
        return anchor.getTarget();
    }

}
