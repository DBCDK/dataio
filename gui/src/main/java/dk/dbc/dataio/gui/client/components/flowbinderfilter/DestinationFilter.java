package dk.dbc.dataio.gui.client.components.flowbinderfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Collections;
import java.util.List;

public class DestinationFilter extends BaseFlowBinderFilter {
    interface DestinationFilterUiBinder extends UiBinder<HTMLPanel, DestinationFilter> {
    }

    private static DestinationFilterUiBinder ourUiBinder = GWT.create(DestinationFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public DestinationFilter() {
        this("", false);
    }

    DestinationFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    DestinationFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    PromptedTextBox destination;

    /**
     * Event handler for handling changes in the destination value
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("destination")
    @SuppressWarnings("unused")
    void destinationValueChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.destinationFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = destination.getValue();
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        final GwtStringClause destinationsClause = new GwtStringClause()
                .withIdentifier("flow_binders:content.destination")
                .withOperator(GwtStringClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(value)
                .withNegated(isInvertFilter());

        return Collections.singletonList(destinationsClause);
    }

    /**
     * Sets the selection according to the key value, setup in the parameter attribute<br>
     * The value is given in url as a plain string
     *
     * @param filterParameter filter parameters to be used by this filter
     */
    @Override
    public void localSetParameter(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            destination.setValue(filterParameter, true);
        }
    }

    @Override
    public String getParameter() {
        return destination.getValue();
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus
     * at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        destination.setFocus(focused);
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return destination.addChangeHandler(changeHandler);
    }
}
