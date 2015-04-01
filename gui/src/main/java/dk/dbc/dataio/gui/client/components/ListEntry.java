package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.ListBox;

import java.util.List;


public class ListEntry extends DataEntry implements HasValue<String> {
    private boolean valueChangeHandlerInitialized;

    @UiField final ListBox listBox;


    public @UiConstructor
    ListEntry(String guiId, String prompt, boolean multiSelect, int visibleItems) {
        super(guiId, prompt);
        listBox = new ListBox();
        listBox.setMultipleSelect(multiSelect);
        listBox.setVisibleItemCount(visibleItems);
        listBox.addStyleName(DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
        if (visibleItems > 1) {
            listBox.setWidth("300px");
        }
        setEnabled(false);  // When empty, disable list box
        add(listBox);
    }

    @Override
    public void clear() {
        listBox.clear();
    }

    public void addAvailableItem(String text) {
        listBox.addItem(text);
    }

    public void addAvailableItem(String text, String key) {
        listBox.addItem(text, key);
    }

    public void setAvailableItems(List<String> items) {
        listBox.clear();
        for(String item : items) {
            addAvailableItem(item);
        }
    }

    public String getSelectedText() {
        int selectedRevisionIndex = listBox.getSelectedIndex();
        if (selectedRevisionIndex < 0) {
            return null;
        }
        // TODO: Lav Exception handling istedet for at returnere null
        return listBox.getItemText(selectedRevisionIndex);
    }

    public String getSelectedKey() {
        int selectedRevisionIndex = listBox.getSelectedIndex();
        if (selectedRevisionIndex < 0) {
            return null;
        }
        // TODO: Lav Exception handling istedet for at returnere null
        return listBox.getValue(selectedRevisionIndex);
    }

    public void setSelected(int selected) {
        listBox.setItemSelected(selected, true);
    }

    public void setEnabled(boolean enabled) {
        listBox.setEnabled(enabled);
    }

    public void setSelectedItem(String value) {
        if(value != null) {
            for (int i = 0; i < listBox.getItemCount(); i++) {
                if (value.equals(listBox.getItemText(i))) {
                    listBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    public void fireChangeEvent() {
        class ListBoxChangedEvent extends ChangeEvent {}
        listBox.fireEvent(new ListBoxChangedEvent());
    }

    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return listBox.addChangeHandler(handler);
    }

    @Override
    public String getValue() {
        return getSelectedText();
    }

    @Override
    public void setValue(String value) {
        setSelectedItem(value);
    }

    @Override
    public void setValue(String value, boolean fireEvents) {
        setSelectedItem(value);
        if (fireEvents) {
            ValueChangeEvent.fire(this, value);
        }
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
        if (!valueChangeHandlerInitialized) {
            valueChangeHandlerInitialized = true;
            addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent event) {
                    ValueChangeEvent.fire(ListEntry.this, getValue());
                }
            });
        }
        return addHandler(handler, ValueChangeEvent.getType());
    }

}
