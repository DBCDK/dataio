package dk.dbc.dataio.gui.views;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 *
 * @author slf
 */
public class MainPanel extends SimpleLayoutPanel {
    final static String CONTEXT_HEADER = "DBC DATAINDSYSTEM";
    public final static String GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION = "navigationbuttoncreation";
    final static String GUIID_NAVIGATION_MENU_BUTTON_VIEW_AND_DELETION = "navigationbuttonviewanddelete";
    private MasterPanel masterPanel = new MasterPanel(Style.Unit.PX, "master-layout");
    private HeaderLabelPanel headerLabel = new HeaderLabelPanel(CONTEXT_HEADER, "header-layout");
    private NavigationPanel navigationPanel = new NavigationPanel("navigation-layout");
    private ContentPanel contentPanel = new ContentPanel("content-layout");

    public MainPanel(ClientFactory clientFactory) {
        this.add(masterPanel);
        masterPanel.addNorth(headerLabel, 40);
        masterPanel.addWest(navigationPanel, 220);
        masterPanel.add(contentPanel);
        RootLayoutPanel.get().add(this);
        contentPanel.add(clientFactory.getFlowEditView());
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
            button.getElement().setId(GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION);
            button.addClickHandler(new ButtonHandler(0));
            add(button);
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
