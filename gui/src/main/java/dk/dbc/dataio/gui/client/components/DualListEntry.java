package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ListBox;
import java.util.Collection;
import java.util.Map;

public class DualListEntry extends DataEntry {
    public static final String DUAL_LIST_ENTRY_DUAL_LIST_CLASS = "dio-DualListEntry-DualList";

    private final DualList dualList = new DualList();

    
    public DualListEntry(String guiId, String prompt) {
        super(guiId, prompt);
        dualList.setStyleName(DUAL_LIST_ENTRY_DUAL_LIST_CLASS);
        setEnabled(false);  // When empty, disable dualList box
        add(dualList);
    }

    @Override
    public void clear() {
        dualList.clear();
    }
    
    public void setAvailableItem(String text) {
        dualList.addAvailableItem(text, text);  // Put the text itself as a key
    }
    
    public void setAvailableItem(String text, String key) {
        dualList.addAvailableItem(text, key);
    }

    public Collection<Map.Entry<String, String>> getSelectedItems() {
        return dualList.getSelectedItems();
    }

    public String[] getSelectedTexts() {
        return (String[]) dualList.getSelectedItems().toArray();
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

}
