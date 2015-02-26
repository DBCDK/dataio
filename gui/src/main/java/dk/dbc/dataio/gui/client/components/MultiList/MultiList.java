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

public class MultiList extends Composite implements HasValue<Map<String, String>> {
    interface MultiListUiBinder extends UiBinder<HTMLPanel, MultiList> {}
    private static MultiListUiBinder uiBinder = GWT.create(MultiListUiBinder.class);

    @UiField
    SortableList list;
    @UiField PushButton removeButton;
    @UiField PushButton addButton;


    @UiConstructor
    public MultiList() {
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
     * addValue adds another item to the bottom of the list
     * @param text The text for the item
     * @param key The key for the item
     */
    public void addValue(String text, String key) {
        list.add(text, key);
    }

    /**
     * addValue adds another item to the bottom of the list
     * @param text The text for the item
     * @param key The key for the item
     * @param fireEvent A boolean to determine, if an event is being fired upon change
     */
    public void addValue(String text, String key, boolean fireEvent) {
        list.add(text, key, fireEvent);
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

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Map<String, String>> valueChangeHandler) {
        return list.addValueChangeHandler(valueChangeHandler);
    }

    @Override
    public void fireEvent(GwtEvent<?> gwtEvent) {
        list.fireEvent(gwtEvent);
    }

    public void setEnabled(boolean enabled) {
        list.setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    public void clear() {
        list.clear();
    }

}
