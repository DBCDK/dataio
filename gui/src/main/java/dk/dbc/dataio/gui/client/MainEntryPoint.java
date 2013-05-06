/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.presenters.FlowEditActivity;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.views.FlowEditView;
import dk.dbc.dataio.gui.views.FlowEditViewImpl;

public class MainEntryPoint implements EntryPoint {

    final static String CONTEXT_HEADER = "DBC DATAINDSYSTEM";
    final static String GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION = "navigationbuttoncreation";
    final static String GUIID_NAVIGATION_MENU_BUTTON_VIEW_AND_DELETION = "navigationbuttonviewanddelete";

    private MasterPanel masterPanel = new MasterPanel(Style.Unit.PX, "master-layout");
    private HeaderLabelPanel headerLabel = new HeaderLabelPanel(CONTEXT_HEADER, "header-layout");
    private NavigationPanel navigationPanel = new NavigationPanel("navigation-layout");
    private ContentPanel contentPanel = new ContentPanel("content-layout");
    
    /**
     * The entry point method, called automatically by loading a module that
     * declares an implementing class as an entry-point
     */
    @Override
    public void onModuleLoad() {
		ClientFactory clientFactory = GWT.create(ClientFactory.class);

		// Start ActivityManager for the main widget with our ActivityMapper
//		ActivityMapper activityMapper = new AppActivityMapper(clientFactory);

        FlowEditView flowEditView = clientFactory.getFlowEditView();
        /* FlowEditView.Presenter flowEditPresenter = */ new FlowEditActivity(flowEditView);

        RootLayoutPanel.get().add(masterPanel);
        masterPanel.addNorth(headerLabel, 40);
        masterPanel.addWest(navigationPanel, 220);
        masterPanel.add(contentPanel);
        contentPanel.add(flowEditView);
    }

    private class HeaderLabelPanel extends Label {
        public HeaderLabelPanel(String label, String styleName) {
            super();
            setStylePrimaryName(styleName);
            setText(label);
        }
    }

    private class MasterPanel extends DockLayoutPanel {
        public MasterPanel(Style.Unit unit, String styleName) {
            super(unit);
            setStylePrimaryName(styleName);
        }
    }

    private class ContentPanel extends DeckLayoutPanel {
        public ContentPanel(String styleName) {
            setStylePrimaryName(styleName);
        }
    }

    private class NavigationPanel extends VerticalPanel {
        Button button = new Button(FlowEditViewImpl.CONTEXT_HEADER);
        public NavigationPanel(String styleName) {
    		setStylePrimaryName(styleName);
            add(button);
            button.getElement().setId(GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION);
            button.addClickHandler(new ButtonHandler(0));
        }
        private class ButtonHandler implements ClickHandler {
            private int subPanelIndex;
            public ButtonHandler(int subPanelIndex) {
                this.subPanelIndex = subPanelIndex;
            }
            @Override
            public void onClick(ClickEvent event) {
                contentPanel.showWidget(subPanelIndex);
                headerLabel.setText(CONTEXT_HEADER + " > " + FlowEditViewImpl.CONTEXT_HEADER);
            }
        }
    }
    
}
