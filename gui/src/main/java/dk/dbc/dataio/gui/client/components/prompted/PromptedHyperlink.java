package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Hyperlink;

/**
 * This class is a Hyperlink widget extended with a prompt label
 * The class extends the PromptedData class, that ensures a uniform presence
 */
public class PromptedHyperlink extends PromptedData {

    @UiField
    final Hyperlink hyperlink = new Hyperlink();


    /**
     * Constructor
     * This is the @UiConstructor, meaning that the two parameters are mandatory inputs, when used by UiBinder
     *
     * @param guiId  The GUI Id
     * @param prompt The prompt label for the widget
     */
    @UiConstructor
    public PromptedHyperlink(String guiId, String prompt) {
        super(guiId, prompt);
        hyperlink.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(hyperlink);
    }

    /**
     * Sets the text for the hyperlink
     *
     * @param text The text value for the hyperlink
     */
    public void setText(String text) {
        hyperlink.setText(text);
    }

    /**
     * Fetches the text from the hyperlink
     *
     * @return The text for the hyperlink
     */
    public String getText() {
        return hyperlink.getText();
    }

    /**
     * Sets the Target History Token for the hyperlink
     *
     * @param targetHistoryToken The Href value for the hyperlink
     */
    public void setTargetHistoryToken(String targetHistoryToken) {
        hyperlink.setTargetHistoryToken(targetHistoryToken);
    }

    /**
     * Fetches the Target History Token from the hyperlink
     *
     * @return The Target History Token for the hyperlink
     */
    public String getTargetHistoryToken() {
        return hyperlink.getTargetHistoryToken();
    }

}
