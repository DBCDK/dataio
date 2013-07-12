package dk.dbc.dataio.gui.client.views;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.client.places.SubmitterCreatePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 *
 * @author slf
 */
public class MainPanel extends DockLayoutPanel {
    final static String CONTEXT_HEADER = "DBC DATAINDSYSTEM";
    public final static String GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION = "navigationbuttonflowcreation";
    public final static String GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION = "navigationbuttonsubmittercreation";
    final static String GUIID_MAIN_PANEL_LAYOUT = "main-panel-layout";
    
    private HeaderLabelPanel headerLabel = new HeaderLabelPanel(CONTEXT_HEADER, "header-panel-layout");
    private NavigationPanel navigationPanel = new NavigationPanel("navigation-panel-layout");
    public ContentPanel contentPanel = new ContentPanel("content-panel-layout");
    private PlaceController placeController = null;

    public MainPanel(ClientFactory clientFactory) {
        super(Style.Unit.PX);
        setStylePrimaryName(GUIID_MAIN_PANEL_LAYOUT);
        addNorth(headerLabel, 40);
        addWest(navigationPanel, 220);
        add(contentPanel);
        placeController = clientFactory.getPlaceController();
    }

    private class HeaderLabelPanel extends Label {
        public HeaderLabelPanel(String label, String styleName) {
            super();
            setStylePrimaryName(styleName);
            setText(label);
        }
    }

    private Place newPlace(int index) {
        switch (index) {
            case 0: return new FlowCreatePlace();
            case 1: return new SubmitterCreatePlace();
            // ...
            default: return null;
        }
    }

    private class NavigationPanel extends VerticalPanel {
        public NavigationPanel(String styleName) {
            setStylePrimaryName(styleName);
            add(new NavigationButton(0, FlowCreateViewImpl.CONTEXT_HEADER, GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
            add(new NavigationButton(1, SubmitterCreateViewImpl.CONTEXT_HEADER, GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION));
            // ...
        }
    }

    private class ContentPanel extends SimpleLayoutPanel {
        public ContentPanel(String styleName) {
            setStylePrimaryName(styleName);
        }
    }

    private class NavigationButton extends Button {
        private String caption = null;
        
        public NavigationButton(int index, String caption, String id) {
            super(caption);
            this.caption = caption;
            addClickHandler(new ButtonHandler(index));
            getElement().setId(id);
        }

        private class ButtonHandler implements ClickHandler {
            private int subPanelIndex;
            public ButtonHandler(int subPanelIndex) {
                this.subPanelIndex = subPanelIndex;
            }
            @Override
            public void onClick(ClickEvent event) {
                headerLabel.setText(CONTEXT_HEADER + " > " + caption);
                placeController.goTo(newPlace(subPanelIndex));
            }
        }
    }
    
}
