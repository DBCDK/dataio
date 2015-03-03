package dk.dbc.dataio.gui.client.components.MultiList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.PushButton;
import dk.dbc.dataio.gui.client.components.SortableList.SortableList;

import java.util.Map;

public class MultiListWidget extends Composite implements HasValue<Map<String, String>> {
    interface MultiListWidgetUiBinder extends UiBinder<HTMLPanel, MultiListWidget> {}
    private static MultiListWidgetUiBinder uiBinder = GWT.create(MultiListWidgetUiBinder.class);

    @UiField SortableList list;
    @UiField PushButton removeButton;
    @UiField PushButton addButton;


    @UiConstructor
    public MultiListWidget() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    /**
     * getValue fetches the all items in the list as a map of Key/Value pairs
     * @return All items in the list
     */
    @Override
    public Map<String, String> getValue() {
        return list.getValue();
    }

    /**
     * setValue replaces all items in the list with the supplied map
     * @param items The new map of list items
     */
    @Override
    public void setValue(Map<String, String> items) {
        list.setValue(items);
    }

    /**
     * setValue replaces all items in the list with the supplied map
     * @param items The new map of list items
     * @param fireEvent A boolean to determine, if an event is being fired upon change
     */
    @Override
    public void setValue(Map<String, String> items, boolean fireEvent) {
        list.setValue(items, fireEvent);
    }

    /**
     * Adds a ValueChangeHandler to the MultiList component, in order to be notified on changes
     * @param valueChangeHandler The Value Change Handler
     * @return A HandlerRegistration object to be used, when removing the ValueChangeHandler
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Map<String, String>> valueChangeHandler) {
        return list.addValueChangeHandler(valueChangeHandler);
    }

    /**
     * Fire a ValueChange event
     * @param gwtEvent The value of the ValueChange Event
     */
    @Override
    public void fireEvent(GwtEvent<?> gwtEvent) {
        list.fireEvent(gwtEvent);
    }

}
