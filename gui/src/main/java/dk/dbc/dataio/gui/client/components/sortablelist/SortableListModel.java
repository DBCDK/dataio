package dk.dbc.dataio.gui.client.components.sortablelist;

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
import dk.dbc.dataio.gui.client.gquery.GQueryWrapper;
import gwtquery.plugins.draggable.client.DraggableOptions;
import gwtquery.plugins.draggable.client.events.DragContext;
import gwtquery.plugins.draggable.client.gwt.DraggableWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SortableListModel {

    static class SortableWidget {  // This is package private because of test - should be private
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

    List<SortableWidget> modelWidgets;  // This is package private because of test - should be private
    GQueryWrapper gQuery = null;
    ValueChangeHandler<Map<String, String>> valueChangeHandler = null;  // This is package private because of test - should be private
    private boolean enabled = false;
    private Boolean manualSorting = true;  // Manual sorting is default
    FlowPanel list;  // This is package private because of test - should be private
    private final String SELECTED = "sortable-widget-entry-selected";
    private final String NOT_SELECTED = "sortable-widget-entry-deselected";
    private final String DISABLED_SORTABLE_WIDGET = "sortable-widget-entry-disabled";

    /*
     * Constructor
     */
    SortableListModel(FlowPanel list) {
        this(list, new GQueryWrapper());
    }

    /*
     * Constructor - allows injection of GwtQuery wrapper
     * To be used for unit test
     */
    SortableListModel(FlowPanel list, GQueryWrapper gQueryWrapper) {
        this.list = list;
        this.gQuery = gQueryWrapper;
        modelWidgets = new ArrayList<>();
    }

    /**
     * Removes all entries in the list
     */
    void clear() {
        list.clear();
        modelWidgets = new ArrayList<>();
    }

    /**
     * Enables or disables all widgets in the model
     * Meaning, that each widget is grayed out, and no interaction can be done on them
     *
     * @param enabled True: Enable the widget, False: Disable the widget
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setDragEnable(enabled);
        setGrayed(!enabled);
    }

    /**
     * Gets the enabled  boolean, stating whether the component is enabled or not
     *
     * @return True: The component is enabled, False: The component is disabled
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * Gets the key value of the selected widget in the model
     *
     * @return The key value of the selected widget
     */
    public String getSelectedItem() {
        for (SortableWidget item : modelWidgets) {
            if (item.selected) {
                return item.key;
            }
        }
        return null;
    }

    /**
     * Adds one item to the bottom of the list
     * It does not trigger a ValueChangedEvent
     *
     * @param text The text as it is displayed in the list
     * @param key  The key for the list item
     */
    void add(String text, String key) {
        addItem(text, key);
        sortIfNeeded();
    }

    /**
     * Clears all items from the list, and replaces it with the supplied items
     * A boolean parameter direct the method to send a fireevent upon completion
     *
     * @param items     Items to set in the new list
     * @param fireEvent Boolean to direct the method to send a ValueChangeEvent
     */
    void put(Map<String, String> items, boolean fireEvent) {
        clear();
        for (Map.Entry<String, String> item : items.entrySet()) {
            addItem(item.getValue(), item.getKey());
        }
        sortIfNeeded();
        if (fireEvent) {
            triggerValueChangeEvent();
        }
    }

    /**
     * Clears all items from the list, and replaces it with the supplied items
     * Does not send a ValueChangeEvent
     *
     * @param items Items to set in the new list
     */
    void put(Map<String, String> items) {
        put(items, false);
    }

    /**
     * Gets the content of the list of widgets as a Map(Key, Value)
     *
     * @return Ordered Map of widgets
     */
    Map<String, String> get() {
        Map<String, String> result = new LinkedHashMap<>(modelWidgets.size());
        for (SortableWidget widget : modelWidgets) {
            result.put(widget.key, widget.value);
        }
        return result;
    }

    /**
     * Sets the sorting in the list to be Manual or Automatic
     *
     * @param manualSorting Manual sorting if true, Automatic if false
     */
    void setManualSorting(Boolean manualSorting) {
        this.manualSorting = manualSorting;
        sortIfNeeded();
    }

    /**
     * Refreshes all widgets on the display, to reflect the changes in the model
     */
    void refresh() {
        setDragEnable(false);
        list.clear();
        for (SortableWidget widget : modelWidgets) {
            widget.draggableWidget = getLabelDraggableWidget(widget.value);
            list.add(widget.draggableWidget);
            setSelected(widget.draggableWidget, widget.selected);
        }
    }

    void reOrder() {
        // First sort the widgets in the model according to their physical pixel position on the display
        Collections.sort(modelWidgets, (widget1, widget2) -> {
            int top1 = widget1.draggableWidget.getAbsoluteTop();
            int top2 = widget2.draggableWidget.getAbsoluteTop();
            return top1 < top2 ? -1 : top1 == top2 ? 0 : 1;
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
     * Sorts the list if manual sort is disabled
     */
    void sortIfNeeded() {
        if (!manualSorting) {
            Collections.sort(modelWidgets, (w1, w2) -> w1.value.compareToIgnoreCase(w2.value));
            refresh();
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
        return () -> valueChangeHandler = null;
    }


    /*
     * Private methods
     */

    /**
     * Adds one item to the bottom of the list
     * It does not trigger a ValueChangedEvent
     *
     * @param text The text as it is displayed in the list
     * @param key  The key for the list item
     */
    private void addItem(String text, String key) {
        DraggableWidget<Label> draggableLabel = getLabelDraggableWidget(text);
        list.add(draggableLabel);
        setSelected(draggableLabel, false);
        modelWidgets.add(new SortableWidget(key, text, false, draggableLabel));
    }

    /**
     * This method constructs a Draggable Label Widget from a text
     *
     * @param text The text, that should appear in the label
     * @return The Draggable Widget
     */
    private DraggableWidget<Label> getLabelDraggableWidget(String text) {
        Label label = new Label(text);
        DraggableWidget<Label> draggableLabel = new DraggableWidget<>(label);
        draggableLabel.setDraggableOptions(labelWidgetDraggableOptions(list));
        gQuery.$(draggableLabel).bind(Event.ONMOUSEDOWN, new MouseDownFunction());
        draggableLabel.setDisabledDrag(!enabled || !manualSorting);
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
        options.setContainment(gQuery.$(container.getParent()));
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

    /**
     * Triggers a ValueChangeEvent - to be called whenever the model changes
     */
    private void triggerValueChangeEvent() {
        if (valueChangeHandler != null) {
            valueChangeHandler.onValueChange(new ValueChangeEvent<Map<String, String>>(get()) {
            });
        }
    }

    /**
     * Enables or disables manual dragging in the list
     *
     * @param dragEnable Enables or disables manual dragging
     */
    private void setDragEnable(boolean dragEnable) {
        for (SortableWidget widget : modelWidgets) {
            if (manualSorting) {
                widget.draggableWidget.setDisabledDrag(!dragEnable);
            } else {
                widget.draggableWidget.setDisabledDrag(true);
            }
        }
    }

    /**
     * Sets if items in the list is grayed out
     *
     * @param grayed True makes all items grayed, false makes them normal
     */
    private void setGrayed(boolean grayed) {
        for (SortableWidget widget : modelWidgets) {
            if (grayed) {
                widget.draggableWidget.getOriginalWidget().addStyleName(DISABLED_SORTABLE_WIDGET);
            } else {
                widget.draggableWidget.getOriginalWidget().removeStyleName(DISABLED_SORTABLE_WIDGET);
            }
        }
    }

    /*
     * Private Classes
     */

    /**
     * This event handler class implements the mouse down functionality
     */
    class MouseDownFunction extends Function {  // Is package-private due to test
        public boolean f(Event event) {
            setOneSelected(gQuery.$(event));
            return true;
        }
    }

    /**
     * This event handler class implements the DragFunction to be activated, when stopping a drag
     */
    class DragStopClass implements DraggableOptions.DragFunction {  // Is package-private due to test
        @Override
        public void f(DragContext dragContext) {
            reOrder();
            triggerValueChangeEvent();
        }
    }
}
