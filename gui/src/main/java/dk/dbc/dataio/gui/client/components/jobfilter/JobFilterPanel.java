package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.events.HasJobFilterPanelHandlers;
import dk.dbc.dataio.gui.client.events.JobFilterPanelEvent;
import dk.dbc.dataio.gui.client.events.JobFilterPanelHandler;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Iterator;

/**
 * This class implements a SimplePanel, with a deleteButton added to the right of the panel:
 * <pre>
 * {@code
 * +------------------------------------------------------------+
 * | Panel content...                           +-----+ +-----+ |
 * |                                            | +/- | |  X  | |
 * |                                            +-----+ +-----+ |
 * +------------------------------------------------------------+
 * }
 *
 * In UiBinder, the component is used as follows:
 *
 * {@code
 * <ui:with field="img" type="dk.dbc.dataio.gui.client.resources.Resources"/>
 * ...
 * <dio:JobFilterPanel title="Panel Title">
 *    <g:Label>Panel content...</g:Label>
 * </dio:JobFilterPanel>
 * }</pre>
 */
public class JobFilterPanel extends Composite implements HasWidgets, HasJobFilterPanelHandlers {
    interface TitledJobFilterPanelUiBinder extends UiBinder<HTMLPanel, JobFilterPanel> {
    }

    protected Resources resources;

    private static TitledJobFilterPanelUiBinder ourUiBinder = GWT.create(TitledJobFilterPanelUiBinder.class);

    JobFilterPanelHandler jobFilterPanelHandler = null;  // This is package private because of test - should be private

    @UiField
    PushButton invertButton;
    @UiField
    PushButton deleteButton;
    @UiField
    SimplePanel content;

    private Boolean invertFilter = true;

    /**
     * Constructor taking the title of the panel and the deleteButton image as parameters (mandatory in UI Binder)
     *
     * @param title        The title of the panel
     * @param resources    the resource for the panel
     * @param invertFilter True if filter is inverted, false if not
     */
    @UiConstructor
    JobFilterPanel(String title, Resources resources, boolean invertFilter) {
        initWidget(ourUiBinder.createAndBindUi(this));
        setTitle(title);
        this.resources = resources;
        this.invertFilter = invertFilter;
        setDeleteButtonImage(resources);
        setInvertButtonImage(this.invertFilter);
    }

    @UiHandler("invertButton")
    void invertButtonClicked(ClickEvent event) {
        if (invertFilter) {
            invertFilter = false;
            triggerJobFilterPanelEvent(JobFilterPanelEvent.JobFilterPanelButton.MINUS_BUTTON);
        } else {
            invertFilter = true;
            triggerJobFilterPanelEvent(JobFilterPanelEvent.JobFilterPanelButton.PLUS_BUTTON);
        }
        setInvertButtonImage(invertFilter);
    }

    @UiHandler("deleteButton")
    void deleteButtonClicked(ClickEvent event) {
        triggerJobFilterPanelEvent(JobFilterPanelEvent.JobFilterPanelButton.REMOVE_BUTTON);
    }

    /**
     * Test whether this filter inverted
     *
     * @return True if the filter is inverted, false if not
     */
    public boolean isInvertFilter() {
        return invertFilter;
    }

    /**
     * Set the filter inverted
     *
     * @param invert Sets whether the job filter is inverted
     */
    public void setInvertFilter(boolean invert) {
        invertFilter = invert;
        setInvertButtonImage(invert);
    }

    /**
     * Adds a widget to the panel
     *
     * @param widget The widget to add to the panel
     */
    @Override
    public void add(Widget widget) {
        this.content.add(widget);
    }

    /**
     * Clears all widgets from the panel
     */
    @Override
    public void clear() {
        this.content.clear();
    }

    /**
     * Gets an iterator for the contained widgets.
     *
     * @return The iterator for the contained widgets.
     */
    @Override
    public Iterator<Widget> iterator() {
        return this.content.iterator();
    }

    /**
     * Removes a widget from this panel
     *
     * @param widget The widget to be removed
     * @return True if the widget was present
     */
    @Override
    public boolean remove(Widget widget) {
        return this.content.remove(widget);
    }

    /**
     * Sets the delete deleteButton image
     *
     * @param resources The resources to be used for fetching the deleteButton images
     */
    private void setDeleteButtonImage(Resources resources) {
        this.deleteButton.getUpFace().setImage(new Image(resources.deleteUpButton()));
        this.deleteButton.getDownFace().setImage(new Image(resources.deleteDownButton()));
    }

    /**
     * Sets the invert button image
     * Please note, that an invert button shall be shown with a Plus sign, since we want to show the action: Plus means make not inverted
     * Similarly, a Minus sign means not-inverted - in the meaning, that a click on the button makes it inverted.
     *
     * @param inverted Determines whether to show a minus button (true) or a plus button (false)
     */
    private void setInvertButtonImage(boolean inverted) {
        if (inverted) {
            this.invertButton.getUpFace().setImage(new Image(resources.plusUpButton()));
            this.invertButton.getDownFace().setImage(new Image(resources.plusDownButton()));
        } else {
            this.invertButton.getUpFace().setImage(new Image(resources.minusUpButton()));
            this.invertButton.getDownFace().setImage(new Image(resources.minusDownButton()));
        }
    }

    /*
     * HasJobFilterPanelHandlers overrides
     */

    /**
     * Adds a JobFilterPanel handler to be fired upon click on one of the buttons
     *
     * @param handler The Job Filter Panel handler
     * @return A Handler Registration object for destroying the handler when no longer in use.
     */
    @Override
    public HandlerRegistration addJobFilterPanelHandler(JobFilterPanelHandler handler) {
        jobFilterPanelHandler = handler;
        return () -> jobFilterPanelHandler = null;
    }


    /*
     * Private
     */

    /**
     * Triggers a ClickEvent
     *
     * @param button The button, that is being triggered
     */
    protected void triggerJobFilterPanelEvent(JobFilterPanelEvent.JobFilterPanelButton button) {
        if (jobFilterPanelHandler != null) {
            jobFilterPanelHandler.onJobFilterPanelButtonClick(new JobFilterPanelEvent() {
                @Override
                public JobFilterPanelButton getJobFilterPanelButton() {
                    return button;
                }
            });
        }
    }

}
