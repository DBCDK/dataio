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
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.querylanguage.GwtIntegerClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Collections;
import java.util.List;

public class SinkFilter extends BaseFlowBinderFilter {
    private final static String NOOP = "0";

    interface SinkFilterUiBinder extends UiBinder<HTMLPanel, SinkFilter> {
    }

    private static SinkFilterUiBinder ourUiBinder = GWT.create(SinkFilterUiBinder.class);

    /* Used while the list of available sinks is being built up.
       When it has been fetched in the callback class, it is not used anymore... */
    private String filterParameter = "";

    /* As with filterParameter - see the comment above */
    private boolean invertFilter = false;

    FlowStoreProxyAsync flowStoreProxy;

    @SuppressWarnings("unused")
    @UiConstructor
    public SinkFilter() {
        this("", false);
    }

    SinkFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, GWT.create(FlowStoreProxy.class), invertFilter);
    }

    SinkFilter(Texts texts, Resources resources, String parameter, FlowStoreProxyAsync flowStoreProxy, boolean invertFilter) {
        super(texts, resources, invertFilter);
        this.flowStoreProxy = flowStoreProxy;
        initWidget(ourUiBinder.createAndBindUi(this));
        flowStoreProxy.findAllSinks(new FetchSinksCallback());
        this.filterParameter = parameter;
        this.invertFilter = invertFilter;
    }

    @UiField
    PromptedList sinkList;

    /**
     * Event handler for handling changes in the sink selection
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("sinkList")
    @SuppressWarnings("unused")
    void sinkSelectionChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.sinkFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = getParameter();
        if (value == null || value.isEmpty() || NOOP.equals(value)) {
            return Collections.emptyList();
        }
        final GwtIntegerClause sinkClause = new GwtIntegerClause()
                .withIdentifier("flow_binders:content.sinkId")
                .withOperator(GwtQueryClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(Integer.valueOf(value))
                .withNegated(isInvertFilter());

        return Collections.singletonList(sinkClause);
    }

    @Override
    public void localSetParameter(String parameter) {
        if (filterParameter != null) {     // List of actual sinks has not yet been found
            filterParameter = parameter;   // Replace current temporary sink value parameter
        }
        sinkList.setSelectedValue(parameter);
    }

    @Override
    public String getParameter() {
        return filterParameter != null ? filterParameter : sinkList.getSelectedKey();
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return sinkList.addChangeHandler(changeHandler);
    }

    /**
     * Callback for the findAllSinks endpoint in the flow store
     */
    class FetchSinksCallback extends FilteredAsyncCallback<List<SinkModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
        }

        @Override
        public void onSuccess(List<SinkModel> models) {
            sinkList.addAvailableItem(texts.sinkFilter_Choose(), NOOP);
            models.forEach(model -> sinkList.addAvailableItem(model.getSinkName(), String.valueOf(model.getId())));
            sinkList.setEnabled(true);
            setParameter(invertFilter, filterParameter);
            filterParameter = null;
        }
    }
}
