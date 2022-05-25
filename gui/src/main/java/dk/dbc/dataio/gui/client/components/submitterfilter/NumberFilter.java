package dk.dbc.dataio.gui.client.components.submitterfilter;

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
import dk.dbc.dataio.gui.client.querylanguage.GwtIntegerClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Collections;
import java.util.List;

public class NumberFilter extends BaseSubmitterFilter {
    interface NumberFilterUiBinder extends UiBinder<HTMLPanel, NumberFilter> {
    }

    private static NumberFilterUiBinder ourUiBinder = GWT.create(NumberFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public NumberFilter() {
        this("", false);
    }

    NumberFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    NumberFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    PromptedTextBox number;

    /**
     * Event handler for handling changes in the number value
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("number")
    @SuppressWarnings("unused")
    void charsetValueChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.numberFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = number.getValue();
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            final GwtIntegerClause numberClause = new GwtIntegerClause()
                    .withIdentifier("submitters:content.number")
                    .withOperator(GwtStringClause.BiOperator.JSON_LEFT_CONTAINS)
                    .withValue(Integer.parseInt(value))
                    .withNegated(isInvertFilter());

            return Collections.singletonList(numberClause);
        } catch (NumberFormatException e) {
            GWT.log("Submitter number filter value is not a number: " + value);
        }
        return Collections.emptyList();
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
            number.setValue(filterParameter, true);
        }
    }

    @Override
    public String getParameter() {
        return number.getValue();
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus
     * at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        number.setFocus(focused);
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return number.addChangeHandler(changeHandler);
    }
}
