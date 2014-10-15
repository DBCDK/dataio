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


public class ListEntry extends DataEntry implements HasValue<String> {

    @UiField final ListBox listBox = new ListBox();

    
    public @UiConstructor ListEntry(String guiId, String prompt) {
        super(guiId, prompt);
        listBox.addStyleName(DataEntry.DATA_ENTRY_INPUT_BOX_CLASS);
        setEnabled(false);  // When empty, disable list box
        add(listBox);
    }

    @Override
    public void clear() {
        listBox.clear();
    }
    
    public void setAvailableItem(String text) {
        listBox.addItem(text);
    }
    
    public void setAvailableItem(String text, String key) {
        listBox.addItem(text, key);
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
        for (int i=0; i<listBox.getItemCount(); i++) {
            if (value == listBox.getItemText(i)) {
                listBox.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    public void setValue(String value, boolean fireEvents) {
        setValue(value);
        if(fireEvents) {
            ValueChangeEvent.fire(this, value);
        }
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> stringValueChangeHandler) {
        return null;
    }
}
