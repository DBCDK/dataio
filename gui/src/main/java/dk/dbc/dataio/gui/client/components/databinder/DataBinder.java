package dk.dbc.dataio.gui.client.components.databinder;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import java.util.function.Consumer;

/**
 * Data Binder base class for synchronizing the data for the derived class with a model<br>
 * <br>
 * The class is on an experimental basis at the moment. It has not been finalized and is NOT ready for use.<br>
 * <br>
 * How to use:<br>
 * <br>
 * In a UI Binder file, add a component, as eg. the PromptedTextBox as follows:<br>
 * <pre>
 * {@code
 *   <li><databinder:DataBinder ui:field="number" guiId="submitternumberpanel" prompt="{txt.label_SubmitterNumber}"/></li>
 * }
 * </pre>
 * In the corresponding java file, do as follows:<br>
 * <pre>
 * {@code
 *   &amp;UiField(provided = true) DataBinder<String, PromptedTextBox> number;
 * }
 * </pre>
 * Because the UiField is 'provided', it needs to be instantiated manually:<br>
 * <pre>
 * {@code
 *   number = new DataBinder<>(new PromptedTextBox(), number -> {presenter.keyPressed(); presenter.numberChanged(number);});
 * }
 * </pre>
 * The lambda function specifies the data binding - in one direction - data is automatically transferred from the input box to the model.<br>
 * The other direction needs still to be implemented.<br>
 * <br>
 * Furthermore, the PromptedTextBox needs to implement the two additional interfaces: HasEnabled and Focusable<br>
 */
public class DataBinder<T, W extends IsWidget & HasValue<T> & HasEnabled & Focusable> extends FlowPanel implements IsWidget, HasValue<T>, HasEnabled, Focusable {
    W widget;
    //    Supplier supplier;
    Consumer consumer;

    public DataBinder(W widget/*, Supplier<T> supplier*/, Consumer<T> consumer) {
        super();
        this.widget = widget;
//        this.supplier = supplier;
        this.consumer = consumer;
        this.widget.addValueChangeHandler(event -> consumer.accept(event.getValue()));
        add(widget);
    }


    /*
     * Set parameters for optional attributes for the UiBinder elements
     */

    /**
     * Sets the Gui Id to be used to identify this component in the DOM
     *
     * @param guiId The Gui Id to identify this component in the DOM
     */
    public void setGuiId(String guiId) {
        getElement().setId(guiId);
    }


    /*
     * HasValue implementations
     */

    @Override
    public T getValue() {
        return widget.getValue();
    }

    @Override
    public void setValue(T value) {
        widget.setValue(value);
    }

    @Override
    public void setValue(T value, boolean fireEvents) {
        widget.setValue(value, fireEvents);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<T> handler) {
        return widget.addValueChangeHandler(handler);
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        widget.fireEvent(event);
    }


    /*
     * HasEnabled implementations
     */

    @Override
    public boolean isEnabled() {
        return widget.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        widget.setEnabled(enabled);
    }


    /*
     * Focusable implementations
     */

    @Override
    public int getTabIndex() {
        return widget.getTabIndex();
    }

    @Override
    public void setAccessKey(char key) {
        widget.setAccessKey(key);
    }

    @Override
    public void setFocus(boolean focused) {
        widget.setFocus(focused);
    }

    @Override
    public void setTabIndex(int index) {
        widget.setTabIndex(index);
    }


    /*
     * IsWidget implementation
     */
    @Override
    public Widget asWidget() {
        return widget.asWidget();
    }
}
