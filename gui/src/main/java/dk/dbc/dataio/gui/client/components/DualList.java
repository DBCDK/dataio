package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * Implements a Dual List panel, for selecting multiple items in a series of
 * available items Inspired by Gardella Juan Pablo - gardellajuanpablo@gmail.com
 * https://bitbucket.org/gardellajuanpablo/duallist/src/b040d8237c8adc32f39a5bb8d2762b62ac07e28d/duallist/src/main/java/org/gwtcomponents/duallist/DualList.java?at=default
 *
 */
public class DualList extends FlowPanel {
    public static final String DUAL_LIST_COMPONENT_CLASS = "dio-DualList";
    public static final String DUAL_LIST_ADDITEM_CLASS = "dual-list-additem-class";
    public static final String DUAL_LIST_REMOVEITEM_CLASS = "dual-list-removeitem-class";
    public static final String DUAL_LIST_LEFT_SELECTION_PANE_CLASS = "dual-list-left-selection-pane-class";
    public static final String DUAL_LIST_SELECTION_BUTTONS_PANE_CLASS = "dual-list-selection-buttons-pane-class";
    public static final String DUAL_LIST_RIGHT_SELECTION_PANE_CLASS = "dual-list-right-selection-pane-class";
    
    public static final String MOVE_LEFT_BUTTON_IMAGE = GWT.getHostPageBaseURL() + "images/left.gif";
    public static final String DISABLED_MOVE_LEFT_BUTTON_IMAGE = GWT.getHostPageBaseURL() + "images/disabledleft.gif";
    public static final String MOVE_RIGHT_BUTTON_IMAGE = GWT.getHostPageBaseURL() + "images/right.gif";
    public static final String DISABLED_MOVE_RIGHT_BUTTON_IMAGE = GWT.getHostPageBaseURL() + "images/disabledright.gif";

    
    ChangeHandler callbackChangeHandler = null;

    private static class SimpleImmutableEntry<K, V> implements Entry<K, V>, java.io.Serializable {

        private static final long serialVersionUID = 7138329143949025153L;

        private static boolean eq(Object o1, Object o2) {
            return o1 == null ? o2 == null : o1.equals(o2);
        }
        private final K key;
        private final V value;

        @SuppressWarnings("unused")
        public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        public SimpleImmutableEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @SuppressWarnings("rawtypes")
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            return key + "=" + value;
        }
    }
    
    ListBox right = new ListBox(true);
    ListBox left = new ListBox(true);
    Image addItem = new Image();
    Image removeItem = new Image();
    FlowPanel buttonPanel = new FlowPanel();

    /**
     * Constructor
     */
    public DualList() {
        this.setStylePrimaryName(DUAL_LIST_COMPONENT_CLASS);
        this.left.setStylePrimaryName(DUAL_LIST_LEFT_SELECTION_PANE_CLASS);
        this.buttonPanel.setStylePrimaryName(DUAL_LIST_SELECTION_BUTTONS_PANE_CLASS);
        this.right.setStylePrimaryName(DUAL_LIST_RIGHT_SELECTION_PANE_CLASS);
        addItem.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                moveItems(left, right);
            }
        });
        removeItem.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                moveItems(right, left);
            }
        });
        enableOrDisableButtonsAndCheckListboxWidth();  // Show correct bitmaps according to content of lists
        addItem.setStylePrimaryName(DUAL_LIST_ADDITEM_CLASS);
        buttonPanel.add(addItem);
        removeItem.setStylePrimaryName(DUAL_LIST_REMOVEITEM_CLASS);
        buttonPanel.add(removeItem);
        add(left);
        add(buttonPanel);
        add(right);
    }

    /**
     * Adds one item to the availableItems list
     *
     * @param value The value of the item to add
     * @param key The item to add
     * @see DualList#setAvailableItems(Collection) to bulk operations.
     */
    public void addAvailableItem(String value, String key) {
        left.addItem(value, key);
        enableOrDisableButtonsAndCheckListboxWidth();
    }

    /*
     * Adds a collection of item to the availableItems list
     *
     * @param items A collection of items to add
     */
    public void setAvailableItems(Collection<? extends Entry<String, String>> items) {
        populateList(left, items);
        enableOrDisableButtonsAndCheckListboxWidth();
    }

    /**
     * Adds a collection of item to the selectedItems list
     *
     * @param items
     */
    public void addSelectedItems(Collection<? extends Entry<String, String>> items) {
        populateList(right, items);
        enableOrDisableButtonsAndCheckListboxWidth();
    }

    /**
     * Enable/disable both left and right lists.
     */
    public void setEnabled(boolean enable) {
        left.setEnabled(enable);
        right.setEnabled(enable);
    }
    
    /**
     * Clear both lists.
     */
    public void clear() {
        left.clear();
        right.clear();
        enableOrDisableButtonsAndCheckListboxWidth();  // Show correct bitmaps according to content of lists
    }

    /**
     * Clear the availableItems Listbox
     */
    public void clearAvailableItems() {
        left.clear();
        enableOrDisableButtonsAndCheckListboxWidth();  // Show correct bitmaps according to content of lists
    }

    /**
     * Return the number of selected items.
     *
     * @return the selected items.
     */
    public int getSelectedItemCount() {
        return right.getItemCount();
    }
    
    /**
     * Return the selected items.
     *
     * @return the selected items.
     */

    public Collection<Entry<String, String>> getSelectedItems() {
        int count = right.getItemCount();
        Collection<Entry<String, String>> selectedItems = new ArrayList<Entry<String, String>>(count);
        for (int i = 0; i < count; i++) {
            selectedItems.add(new SimpleImmutableEntry<String, String>(right.getValue(i), right
                    .getItemText(i)));
        }
        return selectedItems;
    }

    /**
     * Adds a changehandler, for detecting changes in one of the selection boxes
     * 
     * @param changeHandler 
     */
    public void addChangeHandler(ChangeHandler changeHandler) {
        callbackChangeHandler = changeHandler;
    }

    /**
     * Clear the selectedItems Listbox
     */
    public void clearSelectedItems() {
        right.clear();
        enableOrDisableButtonsAndCheckListboxWidth();
    }

    private void enableOrDisableButtonsAndCheckListboxWidth() {
        if (right.getItemCount() > 0) {
            removeItem.setUrl(MOVE_LEFT_BUTTON_IMAGE);
            right.removeStyleName("fixed-empty-listbox-width");
        } else {
            removeItem.setUrl(DISABLED_MOVE_LEFT_BUTTON_IMAGE);
            right.addStyleName("fixed-empty-listbox-width");
        }
        if (left.getItemCount() > 0) {
            addItem.setUrl(MOVE_RIGHT_BUTTON_IMAGE);
            left.removeStyleName("fixed-empty-listbox-width");
        } else {
            addItem.setUrl(DISABLED_MOVE_RIGHT_BUTTON_IMAGE);
            left.addStyleName("fixed-empty-listbox-width");
        }
    }

    private void moveItem(int index, ListBox source, ListBox target) {
        String value = source.getValue(index);
        String item = source.getItemText(index);
        target.addItem(item, value);
        source.removeItem(index);
    }

    private void moveItems(ListBox source, ListBox target) {
        int index = source.getSelectedIndex();
        while (index != -1) {
            moveItem(index, source, target);
            index = source.getSelectedIndex();
        }
        enableOrDisableButtonsAndCheckListboxWidth();
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);  // No need to supply a ChangeEvent
        }
    }

    private static <K> void populateList(ListBox aListBox, Collection<? extends Entry<K, String>> lista) {
        aListBox.clear();
        for (Entry<K, String> p : lista) {
            aListBox.addItem(p.getValue(), p.getKey().toString());
        }
    }
}
