package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import dk.dbc.dataio.gui.client.components.Tooltip;

import java.util.List;


public class PromptedList extends PromptedData implements HasValue<String>, HasValueChangeHandlers<String> {
    private boolean valueChangeHandlerInitialized = false;

    @UiField
    final ListBox listBox;


    /**
     * UI Constructor - carries the parameters to be used by UI Binder
     *
     * @param guiId        The GUI Id to be used for this particular DOM element
     * @param prompt       The Prompt text to be used for the List Box
     * @param multiSelect  A boolean, telling if this is a multiselection list box
     * @param visibleItems Counts the number of visible items
     */
    public @UiConstructor PromptedList(String guiId, String prompt, boolean multiSelect, int visibleItems) {
        super(guiId, prompt);
        listBox = new ListBox();
        listBox.setMultipleSelect(multiSelect);
        listBox.setVisibleItemCount(visibleItems);
        listBox.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        if (visibleItems > 1) {
            listBox.setWidth("300px");
        }
        setEnabled(false);  // When empty, disable list box
        add(listBox);
    }

    /**
     * UI Child - allows child elements under the "item" element. <br>
     * Furthermore, an attribute named "value" is allowed in the "item" element <br>
     * In UI Binder, use the following format for the PromptedList: <br>
     * <pre>
     * {@code
     *   <dio:PromptedList visibleItems="1" guiId="sinktypeselection" prompt="Sink type" multiSelect="false" enabled="true">
     *      <dio:item value="ES_SINK_TYPE"><g:Label>ES sink</g:Label></dio:item>
     *      <dio:item value="UPDATE_SINK_TYPE"><g:Label>Update sink</g:Label></dio:item>
     *      <dio:item value="DUMMY_SINK_TYPE"><g:Label>Dummy sink</g:Label></dio:item>
     *   </dio:PromptedList>
     * }
     * </pre>
     *
     * @param text  The containing text in the "item" element
     * @param value The value of the "value" attribute
     */
    @UiChild(tagname = "item")
    public void addItem(Label text, String value) {
        listBox.addItem(text.getText(), value);
    }

    /**
     * Sets the Tool Tip to be used. Tool Tips will be shown, whenever the user hovers over the component
     *
     * @param toolTip The Tool Tip to be shown to the user
     */
    public void setToolTip(String toolTip) {
        if (!toolTip.isEmpty()) {
            new Tooltip(listBox, toolTip);
        }

    }

    /**
     * Clears the text of the listbox
     */
    @Override
    public void clear() {
        listBox.clear();
    }

    /**
     * Adds an item to the list of available items in the listbox<br>
     * Only the textual value of the item is given here (the text displayed in the list)
     *
     * @param text The textual value of the item
     */
    public void addAvailableItem(String text) {
        listBox.addItem(text);
    }

    /**
     * Adds an item to the list of available items in the listbox<br>
     * Both the textual value and the key value of the item is given
     *
     * @param text The textual value of the item
     * @param key  The key value of the item
     */
    public void addAvailableItem(String text, String key) {
        listBox.addItem(text, key);
    }

    /**
     * Adds a list of available items to the listbox
     *
     * @param items The list of items to be added to the listbox
     */
    public void setAvailableItems(List<String> items) {
        listBox.clear();
        for (String item : items) {
            addAvailableItem(item);
        }
    }

    /**
     * Fetches the selected item from the list box.
     *
     * @return The displayed text of the selected item
     */
    public String getSelectedText() {
        int selectedRevisionIndex = listBox.getSelectedIndex();
        if (selectedRevisionIndex < 0) {
            return null;
        }
        // TODO: Lav Exception handling istedet for at returnere null
        return listBox.getItemText(selectedRevisionIndex);
    }

    /**
     * Fetches the key of the selected item from the list box
     *
     * @return The key of the selected item
     */
    public String getSelectedKey() {
        int selectedRevisionIndex = listBox.getSelectedIndex();
        if (selectedRevisionIndex < 0) {
            return null;
        }
        // TODO: Lav Exception handling istedet for at returnere null
        return listBox.getValue(selectedRevisionIndex);
    }

    /**
     * Sets the selected item in the listbox. Use the integer index to point out the selected item.
     *
     * @param selected The index value of the selected item as an integer
     */
    public void setSelected(int selected) {
        listBox.setItemSelected(selected, true);
    }

    /**
     * Set the listbox to be enabled or disabled
     *
     * @param enabled Sets the listbox enabled if true, disabled if false
     */
    public void setEnabled(boolean enabled) {
        listBox.setEnabled(enabled);
    }

    /**
     * Sets the selection of the listbox. Use the displayed text to point out the item to be selected.
     *
     * @param text The displayed text of the item to select
     */
    public void setSelectedText(String text) {
        if (text != null) {
            for (int i = 0; i < listBox.getItemCount(); i++) {
                if (text.equals(listBox.getItemText(i))) {
                    listBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Sets the selection of the listbox. Use the key value of the item to be used to point out the item to be selected.
     *
     * @param value The key value of the item to select
     */
    public void setSelectedValue(String value) {
        if (value != null) {
            for (int i = 0; i < listBox.getItemCount(); i++) {
                if (value.equals(listBox.getValue(i))) {
                    listBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Fires a ChangeEvent
     */
    public void fireChangeEvent() {
        class ListBoxChangedEvent extends ChangeEvent {
        }
        listBox.fireEvent(new ListBoxChangedEvent());
    }

    /**
     * Adds a ChangeHandler to the PromptedList
     *
     * @param handler The change handler to add
     * @return a HandlerRegistration object
     */
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return listBox.addChangeHandler(handler);
    }

    /**
     * Gets the displayed text of the selected item
     *
     * @return The displayed text of the selected item
     */
    @Override
    public String getValue() {
        return getSelectedText();
    }

    /**
     * Sets the selection of the listbox. Use the displayed text to point out the selection.
     *
     * @param value The displayed text of the item to be selected
     */
    @Override
    public void setValue(String value) {
        setSelectedText(value);
    }

    /**
     * Sets the selection of the listbox. Use the displayed text to point out the selection.<br>
     * If the supplied boolean is true, do also fire a ChangeEvent
     *
     * @param value      The displayed text of the item to be selected
     * @param fireEvents If true, do fire a ChangeEvent
     */
    @Override
    public void setValue(String value, boolean fireEvents) {
        setSelectedText(value);
        if (fireEvents) {
            ValueChangeEvent.fire(this, value);
        }
    }

    /**
     * Adds a ValueChangeHandler to the PromptedList component
     *
     * @param handler The ValueChangeHandler to add
     * @return A HandlerRegistration object to be used to remove the change handler
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
        if (!valueChangeHandlerInitialized) {
            valueChangeHandlerInitialized = true;
            addChangeHandler(event -> ValueChangeEvent.fire(this, getValue()));
        }
        return addHandler(handler, ValueChangeEvent.getType());
    }

}
