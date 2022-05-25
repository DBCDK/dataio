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
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PriorityFilter extends BaseSubmitterFilter {
    interface PriorityFilterUiBinder extends UiBinder<HTMLPanel, PriorityFilter> {
    }

    private static PriorityFilterUiBinder ourUiBinder = GWT.create(PriorityFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public PriorityFilter() {
        this("", false);
    }

    PriorityFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    PriorityFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        Arrays.stream(Priority.values())
                .forEach(value -> priorityList.addAvailableItem(value.name()));
        priorityList.setEnabled(true);
        setParameter(parameter);
    }

    @UiField
    PromptedList priorityList;

    /**
     * Event handler for handling changes in the priority selection
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("priorityList")
    @SuppressWarnings("unused")
    void prioritySelectionChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.priorityFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = getParameter();
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        final GwtStringClause recordSplitterClause = new GwtStringClause()
                .withIdentifier("submitters:content.priority")
                .withOperator(GwtStringClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(value)
                .withNegated(isInvertFilter());

        return Collections.singletonList(recordSplitterClause);
    }

    @Override
    public void localSetParameter(String parameter) {
        priorityList.setSelectedValue(parameter);
    }

    @Override
    public String getParameter() {
        return priorityList.getSelectedKey();
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return priorityList.addChangeHandler(changeHandler);
    }
}
