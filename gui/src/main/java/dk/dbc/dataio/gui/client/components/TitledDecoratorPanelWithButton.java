package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;

/**
 * This class combines the TitledDecoratorPanel and the SimplePanelWithButton panels
 *
 * <pre>
 * {@code
 * +- Panel Title --------------------------------------+
 * | Panel Content                              +-----+ |
 * |                                            | btn | |
 * |                                            +-----+ |
 * +----------------------------------------------------+
}
 *
 * In UiBinder, the component is used as follows:
 *
 * {@code
 * <ui:with field="img" type="dk.dbc.dataio.gui.client.resources.Resources"/>
 * ...
 * <dio:TitledDecoratorPanelWithButton title="Panel Title" buttonImage="{img.deleteButton}">
 *    <g:Label>Panel content...</g:Label>
 * </dio:TitledDecoratorPanelWithButton>
 * }</pre>
 */
public class TitledDecoratorPanelWithButton extends Composite implements HasWidgets, HasClickHandlers {
    interface TitledDecoratorPanelWithButtonUiBinder extends UiBinder<HTMLPanel, TitledDecoratorPanelWithButton> {
    }

    private static TitledDecoratorPanelWithButtonUiBinder ourUiBinder = GWT.create(TitledDecoratorPanelWithButtonUiBinder.class);

    public TitledDecoratorPanelWithButton() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiField TitledDecoratorPanel decorator;
    @UiField SimplePanelWithButton panel;

    /**
     * Constructor taking the title of the panel and the button image as parameters (mandatory in UI Binder)
     * @param title The title of the Decorated panel
     */
    @UiConstructor
    public TitledDecoratorPanelWithButton(String title, ImageResource buttonImage) {
        initWidget(ourUiBinder.createAndBindUi(this));
        setTitle(title);
        this.panel.setButtonImage(buttonImage);
    }

    /**
     * Adds a widget to the TitledDecoratorPanel
     * @param widget The widget to add
     */
    @Override
    public void add(Widget widget) {
        this.panel.add(widget);
    }

    /**
     * Clears all widgets from the TitledDecoratorPanel
     */
    @Override
    public void clear() {
        this.panel.clear();
    }

    /**
     * Gets an iterator for the contained widgets.
     * @return The iterator for the contained widgets.
     */
    @Override
    public Iterator<Widget> iterator() {
        return this.panel.iterator();
    }

    /**
     * Removes a widget in the TitledDecoratorPanel
     * @param widget The widget to be removed
     * @return True if the widget was present
     */
    @Override
    public boolean remove(Widget widget) {
        return this.panel.remove(widget);
    }

    /**
     * Adds a click handler
     * @param clickHandler The click handler to add
     * @return The Handler Registration object
     */
    @Override
    public HandlerRegistration addClickHandler(ClickHandler clickHandler) {
        return panel.addClickHandler(clickHandler);
    }

    /**
     * Sets the title of the TitledDecoratorPanel
     * @param title The title of the TitledDecoratorPanel
     */
    public void setTitle(String title) {
        this.decorator.setTitle(title);
    }

    /**
     * Gets the title of the TitledDecoratorPanel
     * @return The title of the TitledDecoratorPanel
     */
    public String getTitle() {
        return this.decorator.getTitle();
    }


}