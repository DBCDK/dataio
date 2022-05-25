package dk.dbc.dataio.gui.client.components.sortablelist;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;

import java.util.Map;

public class SortableListWidget extends Composite implements HasValue<Map<String, String>> {
    interface SortableListWidgetUiBinder extends UiBinder<HTMLPanel, SortableListWidget> {
    }

    private static SortableListWidgetUiBinder ourUiBinder = GWT.create(SortableListWidgetUiBinder.class);

    protected SortableListModel model = null;

    @UiField
    FlowPanel list;


    /**
     * Constructor
     */
    public SortableListWidget() {
        initWidget(ourUiBinder.createAndBindUi(this));
        model = new SortableListModel(list);
    }


    /*
     * Implementations for the HasValue interface
     */

    /**
     * Fetches all items in the list in sorted order
     *
     * @return All items in the list in sorted order
     */
    @Override
    public Map<String, String> getValue() {
        return model.get();
    }

    /**
     * Clears all items from the list, and replaces it with the supplied items
     *
     * @param items     Items to set in the new list
     * @param fireEvent A boolean to determine, if an event is being fired upon change
     */
    @Override
    public void setValue(Map<String, String> items, boolean fireEvent) {
        model.put(items, fireEvent);
    }

    /**
     * Clears all items from the list, and replaces it with the supplied items
     *
     * @param items Items to set in the new list
     */
    @Override
    public void setValue(Map<String, String> items) {
        setValue(items, true);
    }

    /**
     * Adds a change handler to the list
     * Upon changes in the list, the associated handler will be activated
     *
     * @param changeHandler The Value Change Handler
     * @return A HandlerRegistration object
     */
    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<Map<String, String>> changeHandler) {
        return model.addValueChangeHandler(changeHandler);
    }

}
