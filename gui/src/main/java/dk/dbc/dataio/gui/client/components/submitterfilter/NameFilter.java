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
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Collections;
import java.util.List;

public class NameFilter extends BaseSubmitterFilter {
    interface NameFilterUiBinder extends UiBinder<HTMLPanel, NameFilter> {
    }

    private static NameFilterUiBinder ourUiBinder = GWT.create(NameFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public NameFilter() {
        this("", false);
    }

    NameFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    NameFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    PromptedTextBox name;

    /**
     * Event handler for handling changes in the name value
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("name")
    @SuppressWarnings("unused")
    void nameValueChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.nameFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = name.getValue();
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        final GwtStringClause nameClause = new GwtStringClause()
                .withIdentifier("submitters:content.name")
                .withOperator(GwtStringClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(value)
                .withNegated(isInvertFilter());

        return Collections.singletonList(nameClause);
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
            name.setValue(filterParameter, true);
        }
    }

    @Override
    public String getParameter() {
        return name.getValue();
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus
     * at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        name.setFocus(focused);
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return name.addChangeHandler(changeHandler);
    }
}
