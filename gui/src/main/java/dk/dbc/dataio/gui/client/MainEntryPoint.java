/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Main entry point.
 *
 * @author damkjaer
 */
public class MainEntryPoint implements EntryPoint {

    final static String GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION = "navigationbuttoncreation";
    final static String GUIID_NAVIGATION_MENU_BUTTON_VIEW_AND_DELETION = "navigationbuttonviewanddelete";
    private DeckLayoutPanel contentPanel = new DeckLayoutPanel();

    private DataContentObject dataContentObject = new DataContentObject();
    
    /**
     * The entry point method, called automatically by loading a module that
     * declares an implementing class as an entry-point
     */
    public void onModuleLoad() {
        final DockLayoutPanel masterPanel = new DockLayoutPanel(Style.Unit.PX);
        final VerticalPanel navigationPanel = new VerticalPanel();
        final Button button1 = new Button("Opret");
        button1.getElement().setId(GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION);
        button1.addClickHandler(new ButtonHandler(0));
        final Button button2 = new Button("Se");
        button2.getElement().setId(GUIID_NAVIGATION_MENU_BUTTON_VIEW_AND_DELETION);
        button2.addClickHandler(new ButtonHandler(1));

        final Label headerLabel = new Label("DataIO");
        masterPanel.addNorth(headerLabel, 20);

        navigationPanel.add(button1);
        navigationPanel.add(button2);
        masterPanel.addWest(navigationPanel, 220);

        ViewPage viewPage = new ViewPage(dataContentObject);
        
        contentPanel.add(new FlowCreationWidget(dataContentObject, viewPage));
        contentPanel.add(viewPage);

        masterPanel.add(contentPanel);
        RootLayoutPanel.get().add(masterPanel);
    }

    private class ButtonHandler implements ClickHandler {

        private int subPanelIndex;

        public ButtonHandler(int subPanelIndex) {
            this.subPanelIndex = subPanelIndex;
        }

        @Override
        public void onClick(ClickEvent event) {
            contentPanel.showWidget(subPanelIndex);
        }
    }
}
