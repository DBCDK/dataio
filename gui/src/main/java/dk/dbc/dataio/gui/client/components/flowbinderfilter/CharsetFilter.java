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

public class CharsetFilter extends BaseFlowBinderFilter {
    interface CharsetFilterUiBinder extends UiBinder<HTMLPanel, CharsetFilter> {
    }

    private static CharsetFilterUiBinder ourUiBinder = GWT.create(CharsetFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public CharsetFilter() {
        this("", false);
    }

    CharsetFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    CharsetFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    PromptedTextBox charset;

    /**
     * Event handler for handling changes in the charset value
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("charset")
    @SuppressWarnings("unused")
    void charsetValueChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.charsetFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = charset.getValue();
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        final GwtStringClause charsetClause = new GwtStringClause()
                .withIdentifier("flow_binders:content.charset")
                .withOperator(GwtStringClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(value)
                .withNegated(isInvertFilter());

        return Collections.singletonList(charsetClause);
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
            charset.setValue(filterParameter, true);
        }
    }

    @Override
    public String getParameter() {
        return charset.getValue();
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus
     * at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        charset.setFocus(focused);
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return charset.addChangeHandler(changeHandler);
    }
}
