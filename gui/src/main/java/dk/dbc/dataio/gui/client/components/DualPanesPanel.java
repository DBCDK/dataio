package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;


public class DualPanesPanel extends FlowPanel {

    public static final String DUAL_PANES_PANEL_CLASS = "dio-DualPanesPanel";
    public static final String DUAL_PANES_PANEL_WIDGET_LEFT_CLASS = "dio-DualPanesPanel-WidgetLeftClass";
    public static final String DUAL_PANES_PANEL_WIDGET_RIGHT_CLASS = "dio-DualPanesPanel-WidgetRightClass";

    public DualPanesPanel(String guiId) {
        getElement().setId(guiId);
        setStylePrimaryName(DUAL_PANES_PANEL_CLASS);
    }

    public void setDualPanesPanelWidgets(Widget widgetLeft, Widget widgetRight) {
        setStylePrimaryName(DUAL_PANES_PANEL_CLASS);
        widgetLeft.setStylePrimaryName(DUAL_PANES_PANEL_WIDGET_LEFT_CLASS);
        add(widgetLeft);
        widgetRight.setStylePrimaryName(DUAL_PANES_PANEL_WIDGET_RIGHT_CLASS);
        add(widgetRight);
    }
}
