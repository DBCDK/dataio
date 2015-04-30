package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class PromptedDualList extends PromptedData implements HasValue<Collection<String>>, HasValueChangeHandlers<Collection<String>> {
    private boolean valueChangeHandlerInitialized;

    @UiField final DualList dualList = new DualList();

    
    public @UiConstructor
    PromptedDualList(String guiId, String prompt) {
        super(guiId, prompt);
        dualList.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        setEnabled(false);  // When empty, disable dualList box
        add(dualList);
    }

    @Override
    public void clear() {
        dualList.clear();
    }
    
    public void addAvailableItem(String text) {
        dualList.addAvailableItem(text, text);  // Put the text itself as a key
    }
    
    public void addAvailableItem(String text, String key) {
        dualList.addAvailableItem(text, key);
    }

    public void setAvailableItems(Map<String, String> availableItems) {
        dualList.clearAvailableItems();
        for (Map.Entry<String, String> entry: availableItems.entrySet()) {
            dualList.addAvailableItem(entry.getValue(), entry.getKey());
        }
    }

    public void setSelectedItems(Map<String, String> selectedItems) {
        dualList.clearSelectedItems();
        for (Entry<String, String> selectedItem: selectedItems.entrySet()) {
            dualList.addSelectedItem(selectedItem.getValue(), selectedItem.getKey());
        }
    }

    public int getSelectedItemCount() {
        return dualList.getSelectedItemCount();
    }

    public Map<String, String> getSelectedItems() {
        Map<String, String> result = new TreeMap<String, String>();
        for (Entry<String, String> entry: dualList.getSelectedItems()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public Collection<String> getSelectedTexts() {
        Collection<String> result = new ArrayList<String>();
        for (Entry<String, String> entry: dualList.getSelectedItems()) {
            result.add(entry.getValue());
        }
        return result;
    }
    
    public void setEnabled(boolean enabled) {
        dualList.setEnabled(enabled);
    }
  
    public void fireChangeEvent() {
        class DualListChangedEvent extends ChangeEvent {}
        dualList.fireEvent(new DualListChangedEvent());
    }

    public void addChangeHandler(ChangeHandler handler) {
        dualList.addChangeHandler(handler);
    }

    @Override
    public Collection<String> getValue() {
        return getSelectedTexts();
    }

    @Override
    public void setValue(Collection<String> value) {
        dualList.setSelected(value);
    }

    @Override
    public void setValue(Collection<String> value, boolean fireEvents) {
        setValue(value);
        if (fireEvents) {
            ValueChangeEvent.fire(this, value);
        }
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Collection<String>> handler) {
        if (!valueChangeHandlerInitialized) {
            valueChangeHandlerInitialized = true;
            addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent event) {
                    ValueChangeEvent.fire(PromptedDualList.this, getValue());
                }
            });
        }
        return addHandler(handler, ValueChangeEvent.getType());
    }

}
