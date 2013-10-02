package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ListBox;
import java.util.Map;

public class ListEntry extends DataEntry {
    public class Element {
        private String item;
        private String value;
        public Element(String item, String value) {
            this.item = item;
            this.value = value;
        }
        public String getItem() {
            return item;
        }
        public String getValue() {
            return value;
        }
    }
    public final Element emptyElement = new Element("", "");
    
    public static final String LIST_ENTRY_LIST_BOX_CLASS = "dio-TextEntry-ListBoxClass";

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
    
    public void setAvailableItem(String item, String value) {
        listBox.addItem(item, value);
    }
    
    public ListEntry.Element getSelectedItem() {
        int selectedRevisionIndex = listBox.getSelectedIndex();
        if (selectedRevisionIndex < 0) {
            return emptyElement;
        }
        return new ListEntry.Element(listBox.getItemText(selectedRevisionIndex), listBox.getValue(selectedRevisionIndex));
    }
    
    public void setEnabled(boolean enabled) {
        listBox.setEnabled(enabled);
    }
  
    public void fireChangeEvent() {
        class ListBoxChangedEvent extends ChangeEvent {}
        listBox.fireEvent(new ListBoxChangedEvent());
    }

    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
        return listBox.addKeyDownHandler(handler);
    }
    
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return listBox.addChangeHandler(handler);
    }

}
