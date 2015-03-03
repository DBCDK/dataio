package dk.dbc.dataio.gui.client.components.SortableList;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.query.client.Function;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import gwtquery.plugins.draggable.client.DraggableOptions;
import gwtquery.plugins.draggable.client.events.DragContext;
import gwtquery.plugins.draggable.client.gwt.DraggableWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.gwt.query.client.GQuery.$;

public class SortableListModel {
    private class SortableWidget {
        String key;
        String value;
        boolean selected;
        DraggableWidget draggableWidget;

        public SortableWidget(String key, String value, boolean selected, DraggableWidget draggableWidget) {
            this.key = key;
            this.value = value;
            this.selected = selected;
            this.draggableWidget = draggableWidget;
        }
    }

    private List<SortableWidget> modelWidgets;
    private ValueChangeHandler<Map<String, String>> valueChangeHandler = null;
    private boolean enabled = false;
    private FlowPanel list;
    private final String SELECTED = "sortable-widget-entry-selected";
    private final String NOT_SELECTED = "sortable-widget-entry-deselected";
    private final String DISABLED_SORTABLE_WIDGET = "sortable-widget-entry-disabled";

    /*
     * Constructor
     */
    SortableListModel(FlowPanel list) {
        this.list = list;
        modelWidgets = new ArrayList<SortableWidget>();
    }

    /**
     * Removes all entries in the list
     */
    void clear() {
        list.clear();
        modelWidgets = new ArrayList<SortableWidget>();
    }

    /**
     * Enables or disables all widgets in the model
     * Meaning, that each widget is grayed out, and no interaction can be done on them
     *
     * @param enabled True: Enable the widget, False: Disable the widget
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        for (SortableWidget widget : modelWidgets) {
            widget.draggableWidget.setDisabledDrag(!enabled);
            if (enabled) {
                widget.draggableWidget.getOriginalWidget().removeStyleName(DISABLED_SORTABLE_WIDGET);
            } else {
                widget.draggableWidget.getOriginalWidget().addStyleName(DISABLED_SORTABLE_WIDGET);
            }
        }
    }

    /**
     * Gets the enabled  boolean, stating whether the component is enabled or not
     * @return True: The component is enabled, False: The component is disabled
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Adds one item to the bottom of the list
     * It does not trigger a ValueChangedEvent
     *
     * @param text The text as it is displayed in the list
     * @param key  The key for the list item
     */
    void add(String text, String key) {
        DraggableWidget<Label> draggableLabel = getLabelDraggableWidget(text);
        list.add(draggableLabel);
        setSelected(draggableLabel, false);
        modelWidgets.add(new SortableWidget(key, text, false, draggableLabel));
    }

    /**
     * Clears all items from the list, and replaces it with the supplied items
     *
     * @param items Items to set in the new list
     */
    void put(Map<String, String> items) {
        clear();
        for (Map.Entry<String, String> item : items.entrySet()) {
            add(item.getValue(), item.getKey());
        }
    }

    /**
     * Gets the content of the list of widgets as a Map(Key, Value)
     *
     * @return Ordered Map of widgets
     */
    Map<String, String> get() {
        Map<String, String> result = new LinkedHashMap<String, String>(modelWidgets.size());
        for (SortableWidget widget : modelWidgets) {
            result.put(widget.key, widget.value);
        }
        return result;
    }

    /**
     * Refreshes all widgets on the display, to reflect the changes in the model
     */
    void refresh() {
        for (SortableWidget widget : modelWidgets) {
            widget.draggableWidget.setDisabledDrag(true);
        }
        list.clear();
        for (SortableWidget widget : modelWidgets) {
            widget.draggableWidget = getLabelDraggableWidget(widget.value);
            list.add(widget.draggableWidget);
            setSelected(widget.draggableWidget, widget.selected);
        }
    }

    void reOrder() {
        // First sort the widgets in the model according to their physical pixel position on the display
        Collections.sort(modelWidgets, new Comparator<SortableWidget>() {
            @Override
            public int compare(SortableWidget widget1, SortableWidget widget2) {
                int top1 = widget1.draggableWidget.getAbsoluteTop();
                int top2 = widget2.draggableWidget.getAbsoluteTop();
                return top1 < top2 ? -1 : top1 == top2 ? 0 : 1;
            }
        });
        // Refresh the display according to the model
        refresh();
        }

    /**
     * Sets an item on the display to be selected and set all others not selected
     *
     * @param selectWidget The widget as a GQuery
     */
    void setOneSelected(GQuery selectWidget) {
        Element selectedElement = null;
        if (selectWidget != null && selectWidget.size() > 0) {
            selectedElement = selectWidget.get(0);
        }
        for (SortableWidget widget : modelWidgets) {
            boolean selected = selectedElement == widget.draggableWidget.getElement();
            setSelected(widget.draggableWidget, selected);
            widget.selected = selected;
        }
    }

    /**
     * Adds a change handler to the list
     * Upon changes in the list, the associated handler will be activated
     *
     * @param changeHandler The Value Change Handler
     * @return A HandlerRegistration object
     */
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<Map<String, String>> changeHandler) {
        valueChangeHandler = changeHandler;
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                valueChangeHandler = null;
            }
        };
    }


    /*
     * Private methods
     */

    /**
     * This method constructs a Draggable Label Widget from a text
     *
     * @param text The text, that should appear in the label
     * @return The Draggable Widget
     */
    private DraggableWidget<Label> getLabelDraggableWidget(String text) {
        Label label = new Label(text);
        DraggableWidget<Label> draggableLabel = new DraggableWidget<Label>(label);
        draggableLabel.setDraggableOptions(labelWidgetDraggableOptions(list));
        $(draggableLabel).bind(Event.ONMOUSEDOWN, new MouseDownFunction());
        return draggableLabel;
    }

    /**
     * Returns a DraggableOptions object, containing settings for a Draggable item
     *
     * @return DraggableOptions object
     */
    private DraggableOptions labelWidgetDraggableOptions(Widget container) {
        DraggableOptions options = new DraggableOptions();
        options.setAxis(DraggableOptions.AxisOption.Y_AXIS);
        options.setContainment($(container.getParent()));
        options.setOnDragStop(new DragStopClass());
        return options;
    }

    /**
     * Sets an item on the display to be selected or not selected
     *
     * @param widget   The widget as a DraggableWidget
     * @param selected True: Selected, False: Not selected
     */
    private void setSelected(DraggableWidget widget, boolean selected) {
        widget.addStyleName(selected ? SELECTED : NOT_SELECTED);
        widget.removeStyleName(selected ? NOT_SELECTED : SELECTED);
    }
    /*
     * Private Classes
     */

    /**
     * This event handler class implements the mouse down functionality
     */
    private class MouseDownFunction extends Function {
        public boolean f(Event event) {
            setOneSelected($(event));
            return true;
        }
    }

    /**
     * This event handler class implements the DragFunction to be activated, when stopping a drag
     */
    private class DragStopClass implements DraggableOptions.DragFunction {
        @Override
        public void f(DragContext dragContext) {
            reOrder();
            if (valueChangeHandler != null) {
                valueChangeHandler.onValueChange(new ValueChangeEvent<Map<String, String>>(get()) {});
            }
        }
    }
}
