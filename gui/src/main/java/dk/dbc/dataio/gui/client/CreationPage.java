/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 *
 * @author damkjaer
 */
class CreationPage extends VerticalPanel {
   
    // Constants
    final static String GUIID_CREATION_TEXT_AREA = "creationtextarea";
    private static final int CREATION_TEXT_MAX_LENGTH = 160;

    // Local variables
    private final HorizontalPanel flowDescriptionPanel = new HorizontalPanel();
    private final Label flowDescriptionLabel = new Label("Skriv noget tekst. Max " + CREATION_TEXT_MAX_LENGTH + " tegn");
    private final TextArea flowDescriptionTextArea = new TextArea();
    private final Button saveButton = new Button("Gem");

   
    public CreationPage(final DataContentObject dataContentObject, final ViewPage viewPage) {
        setUpFlowDescriptionPanel();
        add(flowDescriptionPanel);
        saveButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dataContentObject.add(flowDescriptionTextArea.getText());
                viewPage.onUpdate();
            }
        });
        add(saveButton);
    }
    
    private void setUpFlowDescriptionPanel() {
        flowDescriptionPanel.add(flowDescriptionLabel);
        setUpFlowDescriptionTextArea();
        flowDescriptionPanel.add(flowDescriptionTextArea);
    }

    private void setUpFlowDescriptionTextArea() {
        flowDescriptionTextArea.setCharacterWidth(40);
        flowDescriptionTextArea.setVisibleLines(4);
        flowDescriptionTextArea.getElement().setAttribute("Maxlength", String.valueOf(CREATION_TEXT_MAX_LENGTH));
        flowDescriptionTextArea.getElement().setId(GUIID_CREATION_TEXT_AREA);
    }
}
