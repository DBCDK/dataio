package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;

/**
 * This class implements the DecoratorPanel with a title on the top of it
 * <pre>
 * {@code
 *    +- Panel Title --------------------------------------+
 *    | Panel Content                                      |
 *    +----------------------------------------------------+
 * }
 *
 * In UiBinder, the component is used as follows:
 *
 * {@code
 *       <dio:TitledDecoratorPanel title="Panel Title">
 *          <g:Label>Panel Content</g:Label>
 *       </dio:TitledDecoratorPanel>
 * }</pre>
 */
public class TitledDecoratorPanel extends Composite implements HasWidgets {
    interface TitledDecoraterPanelUiBinder extends UiBinder<HTMLPanel, TitledDecoratorPanel> {
    }

    private static TitledDecoraterPanelUiBinder ourUiBinder = GWT.create(TitledDecoraterPanelUiBinder.class);

    @UiField Label title;
    @UiField SimplePanel contentPanel;

    /**
     * Constructor taking the title of the panel as a parameter (mandatory in UI Binder)
     * @param title The title of the Decorated panel
     */
    @UiConstructor
    public TitledDecoratorPanel(String title) {
        initWidget(ourUiBinder.createAndBindUi(this));
        setTitle(title);
    }

    /**
     * Adds a widget to the TitledDecoratorPanel
     * @param widget The widget to add
     */
    @Override
    public void add(Widget widget) {
        this.contentPanel.add(widget);
    }

    /**
     * Clears all widgets from the TitledDecoratorPanel
     */
    @Override
    public void clear() {
        this.contentPanel.clear();
    }

    /**
     * Gets an iterator for the contained widgets.
     * @return The iterator for the contained widgets.
     */
    @Override
    public Iterator<Widget> iterator() {
        return this.contentPanel.iterator();
    }

    /**
     * Removes a widget in the TitledDecoratorPanel
     * @param widget The widget to be removed
     * @return True if the widget was present
     */
    @Override
    public boolean remove(Widget widget) {
        return this.contentPanel.remove(widget);
    }

    /**
     * Sets the title of the TitledDecoratorPanel
     * @param title The title of the TitledDecoratorPanel
     */
    public void setTitle(String title) {
        this.title.setText(title);
    }

    /**
     * Gets the title of the TitledDecoratorPanel
     * @return The title of the TitledDecoratorPanel
     */
    public String getTitle() {
        return this.title.getText();
    }

}