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
import com.google.gwt.user.client.ui.RadioButton;
import dk.dbc.dataio.gui.client.querylanguage.GwtIntegerClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Collections;
import java.util.List;

public class EnabledFilter extends BaseSubmitterFilter {
    private enum State {ENABLED, DISABLED}

    interface EnabledFilterUiBinder extends UiBinder<HTMLPanel, EnabledFilter> {
    }

    private static EnabledFilterUiBinder ourUiBinder = GWT.create(EnabledFilterUiBinder.class);

    private ChangeHandler callbackChangeHandler = null;

    @SuppressWarnings("unused")
    @UiConstructor
    public EnabledFilter() {
        this("", false);
    }

    EnabledFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    EnabledFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    RadioButton enabledRadioButton;
    @UiField
    RadioButton disabledRadioButton;

    /**
     * Event handler for handling changes in the selection of enabled status
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler(value = {"enabledRadioButton", "disabledRadioButton"})
    @SuppressWarnings("unused")
    void RadioButtonValueChanged(ValueChangeEvent<Boolean> event) {
        filterChanged();
        if (callbackChangeHandler != null) {
            callbackChangeHandler.onChange(null);
        }
    }

    @Override
    public String getName() {
        return texts.enabledFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        boolean isEnabled;
        if (enabledRadioButton.getValue()) {
            isEnabled = true;
        } else if (disabledRadioButton.getValue()) {
            isEnabled = false;
        } else {
            return Collections.emptyList();
        }
        final GwtIntegerClause enabledClause = new GwtIntegerClause()
                .withIdentifier("submitters:content.enabled")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(isEnabled ? 1 : 0)
                .withFlag(true)
                .withNegated(isInvertFilter());

        return Collections.singletonList(enabledClause);
    }

    /**
     * Sets the selection according to the value, setup in the parameter attribute<br>
     *
     * @param filterParameter filter value
     */
    @Override
    public void localSetParameter(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            enabledRadioButton.setValue(false);
            disabledRadioButton.setValue(false);
            switch (State.valueOf(filterParameter)) {
                case ENABLED:
                    enabledRadioButton.setValue(true);
                    break;
                case DISABLED:
                    disabledRadioButton.setValue(true);
                    break;
            }
        }
    }

    /**
     * Gets the parameter value for the filter
     *
     * @return The stored filter parameter for the specific job filter
     */
    @Override
    public String getParameter() {
        if (enabledRadioButton.getValue()) {
            return State.ENABLED.name();
        } else if (disabledRadioButton.getValue()) {
            return State.DISABLED.name();
        } else {
            return "";
        }
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        callbackChangeHandler = changeHandler;
        callbackChangeHandler.onChange(null);
        return () -> callbackChangeHandler = null;
    }
}
