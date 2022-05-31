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

public class FormatFilter extends BaseFlowBinderFilter {
    interface FormatFilterUiBinder extends UiBinder<HTMLPanel, FormatFilter> {
    }

    private static FormatFilterUiBinder ourUiBinder = GWT.create(FormatFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public FormatFilter() {
        this("", false);
    }

    FormatFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    FormatFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    PromptedTextBox format;

    /**
     * Event handler for handling changes in the format value
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("format")
    @SuppressWarnings("unused")
    void formatValueChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.formatFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = format.getValue();
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        final GwtStringClause formatClause = new GwtStringClause()
                .withIdentifier("flow_binders:content.format")
                .withOperator(GwtStringClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(value)
                .withNegated(isInvertFilter());

        return Collections.singletonList(formatClause);
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
            format.setValue(filterParameter, true);
        }
    }

    @Override
    public String getParameter() {
        return format.getValue();
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus
     * at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        format.setFocus(focused);
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return format.addChangeHandler(changeHandler);
    }
}
