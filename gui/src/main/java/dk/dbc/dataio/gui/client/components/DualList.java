package dk.dbc.dataio.gui.client.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 *
 * Implements a Dual List panel, for selecting multiple items in a series of
 * available items Inspired by Gardella Juan Pablo - gardellajuanpablo@gmail.com
 * https://bitbucket.org/gardellajuanpablo/duallist/src/b040d8237c8adc32f39a5bb8d2762b62ac07e28d/duallist/src/main/java/org/gwtcomponents/duallist/DualList.java?at=default
 *
 */
public class DualList extends HorizontalPanel {

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
    Button addItem = new Button(">");
    Button removeItem = new Button("<");
    VerticalPanel buttonPanel = new VerticalPanel();

    /**
     * Constructor
     */
    public DualList() {
        this.addStyleName("dual-list-component-class");
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
        buttonPanel.add(addItem);
        buttonPanel.add(removeItem);
        add(left);
        add(buttonPanel);
        add(right);
    }

    /**
     * Adds one item to the availableItems list
     *
     * @param item The item to add
     * @param value The value of the item to add
     * @see DualList#addAvailableItems(Collection) to bulk operations.
     */
    public void addAvailableItem(String item, String value) {
        left.addItem(item, value);
        enableOrDisableButtons();
    }

    /*
     * Adds a collection of item to the availableItems list
     *
     * @param items A collection of items to add
     */
    public void addAvailableItems(Collection<? extends Entry<String, String>> items) {
        populateList(left, items);
        enableOrDisableButtons();
    }

    /**
     * Adds a collection of item to the selectedItems list
     *
     * @param items
     */
    public void addSelectedItems(Collection<? extends Entry<String, String>> items) {
        populateList(right, items);
        enableOrDisableButtons();
    }

    /**
     * Clear both lists.
     */
    public void clear() {
        left.clear();
        right.clear();
        addItem.setEnabled(false);
        removeItem.setEnabled(false);
    }

    /**
     * Clear the availableItems Listbox
     */
    public void clearAvailableItems() {
        left.clear();
        addItem.setEnabled(false);
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
     * Clear the selectedItems Listbox
     */
    public void clearSelectedItems() {
        right.clear();
        removeItem.setEnabled(false);
    }

    private void enableOrDisableButtons() {
        removeItem.setEnabled(right.getItemCount() > 0);
        addItem.setEnabled(left.getItemCount() > 0);
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
        enableOrDisableButtons();
    }

    private static <K> void populateList(ListBox aListBox, Collection<? extends Entry<K, String>> lista) {
        aListBox.clear();
        for (Entry<K, String> p : lista) {
            aListBox.addItem(p.getValue(), p.getKey().toString());
        }
    }
}