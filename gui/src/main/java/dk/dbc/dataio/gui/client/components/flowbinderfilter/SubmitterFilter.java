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
import dk.dbc.dataio.gui.client.querylanguage.GwtIntegerClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubmitterFilter extends BaseFlowBinderFilter {
    interface SubmitterFilterUiBinder extends UiBinder<HTMLPanel, SubmitterFilter> {
    }

    private static SubmitterFilterUiBinder ourUiBinder = GWT.create(SubmitterFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public SubmitterFilter() {
        this("", false);
    }

    SubmitterFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    SubmitterFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(parameter);
    }

    @UiField
    PromptedTextBox submitter;

    /**
     * Event handler for handling changes in the submitter value
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("submitter")
    @SuppressWarnings("unused")
    void submitterValueChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.submitterFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = submitter.getValue();
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }

        // Entered value might contain a comma separated list of submitters numbers
        final String[] submitterNumbers = value.split("\\s*,\\s*");

        final List<GwtQueryClause> clauses = new ArrayList<>(submitterNumbers.length);
        for (String submitterNumber : submitterNumbers) {
            try {
                clauses.add(new GwtIntegerClause()
                        .withIdentifier("flow_binders:content.submitterIds")
                        .withOperator(GwtStringClause.BiOperator.JSON_LEFT_CONTAINS)
                        .withValue(Integer.parseInt(submitterNumber))
                        .withArrayProperty(true)
                        .withNegated(isInvertFilter()));
            } catch (NumberFormatException e) {
                GWT.log("Submitter value is not a number: " + submitterNumber);
            }
        }
        return clauses;
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
            submitter.setValue(filterParameter, true);
        }
    }

    @Override
    public String getParameter() {
        return submitter.getValue();
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus
     * at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        submitter.setFocus(focused);
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return submitter.addChangeHandler(changeHandler);
    }
}
