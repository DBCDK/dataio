package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import dk.dbc.dataio.gui.client.i18n.MainConstants;
import dk.dbc.dataio.gui.client.places.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.places.FlowCreatePlace;
import dk.dbc.dataio.gui.client.places.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.places.SinkCreatePlace;
import dk.dbc.dataio.gui.client.places.SubmitterCreatePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class MainPanel extends DockLayoutPanel {
    private final MainConstants constants = GWT.create(MainConstants.class);
    public static final String GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION = "navigationbuttonflowcreation";
    public static final String GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_CREATION = "navigationbuttonflowcomponentcreation";
    public static final String GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION = "navigationbuttonsubmittercreation";
    public static final String GUIID_NAVIGATION_MENU_ITEM_FLOWBINDER_CREATION = "navigationbuttonflowbindercreation";
    public static final String GUIID_NAVIGATION_MENU_ITEM_SINK_CREATION = "navigationbuttonsinkcreation";
    public static final String GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_SHOW = "navigationbuttonflowcomponentsshow";
    private static final String GUIID_MAIN_PANEL_LAYOUT = "main-panel-layout";

    public final ContentPanel contentPanel = new ContentPanel("content-panel-layout");
    private final HeaderLabelPanel headerLabel = new HeaderLabelPanel(constants.header_DataIO(), "header-panel-layout");
    private final NavigationPanel navigationPanel = new NavigationPanel("navigation-panel-layout");

    private PlaceController placeController = null;

    public MainPanel(ClientFactory clientFactory) {
        super(Style.Unit.PX);
        setStylePrimaryName(GUIID_MAIN_PANEL_LAYOUT);
        addWest(navigationPanel, 250);
        addNorth(headerLabel, 30);
        add(contentPanel);
        placeController = clientFactory.getPlaceController();
    }

    private Place newPlace(int index) {
        switch (index) {
            case 0: return new FlowCreatePlace();
            case 1: return new FlowComponentCreatePlace();
            case 2: return new SubmitterCreatePlace();
            case 3: return new FlowbinderCreatePlace();
            case 4: return new SinkCreatePlace();
            case 5: return new FlowComponentsShowPlace();
            // ...
            default: return null;
        }
    }

    private static class HeaderLabelPanel extends Label {
        public HeaderLabelPanel(String label, String styleName) {
            super();
            setStylePrimaryName(styleName);
            setText(label);
        }
    }

    private static class ContentPanel extends SimpleLayoutPanel {
        public ContentPanel(String styleName) {
            setStylePrimaryName(styleName);
        }
    }

    private class NavigationPanel extends FlowPanel {
        public NavigationPanel(String styleName) {
            setStylePrimaryName(styleName);
            add(new Image("images/dbclogo.gif"));
            add(new NavigationButton(0, constants.header_FlowCreation(), GUIID_NAVIGATION_MENU_ITEM_FLOW_CREATION));
            add(new NavigationButton(1, constants.header_FlowComponentCreation(), GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_CREATION));
            add(new NavigationButton(2, constants.header_SubmitterCreation(), GUIID_NAVIGATION_MENU_ITEM_SUBMITTER_CREATION));
            add(new NavigationButton(3, constants.header_FlowbinderCreation(), GUIID_NAVIGATION_MENU_ITEM_FLOWBINDER_CREATION));
            add(new NavigationButton(4, constants.header_SinkCreation(), GUIID_NAVIGATION_MENU_ITEM_SINK_CREATION));
            add(new NavigationButton(5, constants.header_FlowComponentsShow(), GUIID_NAVIGATION_MENU_ITEM_FLOW_COMPONENT_SHOW));
            // ...
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
                headerLabel.setText(constants.header_DataIO() + " >> " + caption);
                placeController.goTo(newPlace(subPanelIndex));
            }
        }
    }
    
}
