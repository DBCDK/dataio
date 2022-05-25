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
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.querylanguage.GwtIntegerClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Collections;
import java.util.List;

public class FlowFilter extends BaseFlowBinderFilter {
    private final static String NOOP = "0";

    interface FlowFilterUiBinder extends UiBinder<HTMLPanel, FlowFilter> {
    }

    private static FlowFilterUiBinder ourUiBinder = GWT.create(FlowFilterUiBinder.class);

    /* Used while the list of available flows is being built up.
       When it has been fetched in the callback class, it is not used anymore... */
    private String filterParameter = "";

    /* As with filterParameter - see the comment above */
    private boolean invertFilter = false;

    FlowStoreProxyAsync flowStoreProxy;

    @SuppressWarnings("unused")
    @UiConstructor
    public FlowFilter() {
        this("", false);
    }

    FlowFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, GWT.create(FlowStoreProxy.class), invertFilter);
    }

    FlowFilter(Texts texts, Resources resources, String parameter, FlowStoreProxyAsync flowStoreProxy, boolean invertFilter) {
        super(texts, resources, invertFilter);
        this.flowStoreProxy = flowStoreProxy;
        initWidget(ourUiBinder.createAndBindUi(this));
        flowStoreProxy.findAllFlows(new FetchFlowsCallback());
        this.filterParameter = parameter;
        this.invertFilter = invertFilter;
    }

    @UiField
    PromptedList flowList;

    /**
     * Event handler for handling changes in the flow selection
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("flowList")
    @SuppressWarnings("unused")
    void flowSelectionChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.flowFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = getParameter();
        if (value == null || value.isEmpty() || NOOP.equals(value)) {
            return Collections.emptyList();
        }
        final GwtIntegerClause flowClause = new GwtIntegerClause()
                .withIdentifier("flow_binders:content.flowId")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(Integer.valueOf(value))
                .withNegated(isInvertFilter());

        return Collections.singletonList(flowClause);
    }

    @Override
    public void localSetParameter(String parameter) {
        if (filterParameter != null) {     // List of actual flow has not yet been found
            filterParameter = parameter;   // Replace current temporary flow value parameter
        }
        flowList.setSelectedValue(parameter);
    }

    @Override
    public String getParameter() {
        return filterParameter != null ? filterParameter : flowList.getSelectedKey();
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return flowList.addChangeHandler(changeHandler);
    }

    /**
     * Callback for the findAllFlows endpoint in the flow store
     */
    class FetchFlowsCallback extends FilteredAsyncCallback<List<FlowModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
        }

        @Override
        public void onSuccess(List<FlowModel> models) {
            flowList.addAvailableItem(texts.flowFilter_Choose(), NOOP);
            models.forEach(model -> flowList.addAvailableItem(model.getFlowName(), String.valueOf(model.getId())));
            flowList.setEnabled(true);
            setParameter(invertFilter, filterParameter);
            filterParameter = null;
        }
    }
}
