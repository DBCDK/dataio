package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ListBox;
import java.lang.String;


public class ListEntry extends DataEntry {
    public static final String LIST_ENTRY_LIST_BOX_CLASS = "dio-ListEntry-ListBox";

    private final ListBox listBox = new ListBox();

    
    public ListEntry(String guiId, String prompt) {
        super(guiId, prompt);
        listBox.setStyleName(LIST_ENTRY_LIST_BOX_CLASS);
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

}
