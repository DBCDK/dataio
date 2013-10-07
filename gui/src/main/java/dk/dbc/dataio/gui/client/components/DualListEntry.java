package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class DualListEntry extends DataEntry {
    public static final String DUAL_LIST_ENTRY_DUAL_LIST_CLASS = "dio-DualListEntry-DualList";

    private final DualList dualList = new DualList();

    
    public DualListEntry(String guiId, String prompt) {
        super(guiId, prompt);
        setStylePrimaryName(DUAL_LIST_ENTRY_DUAL_LIST_CLASS);
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

}
