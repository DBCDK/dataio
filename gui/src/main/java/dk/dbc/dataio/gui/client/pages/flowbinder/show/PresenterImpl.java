package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class represents the show flow binders presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    private PlaceController placeController;
    private String header;
    View view;


    /**
     * Default constructor
     *
     * @param placeController PlaceController for navigation
     * @param view            Global flow binders View, necessary for keeping filter state, etc.
     * @param header          Breadcrumb header text
     */
    public PresenterImpl(PlaceController placeController, View view, String header) {
        this.placeController = placeController;
        this.view = view;
        this.header = header;
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        final AbstractBasePlace place = (AbstractBasePlace) placeController.getWhere();
        view.setPresenter(this);
        view.setHeader(this.header);
        view.flowBinderFilter.setPlace(place);
        containerWidget.setWidget(view.asWidget());
        fetchFlowBinders();
    }

    @Override
    public void setPlace(AbstractBasePlace place) {
        if (view != null && view.flowBinderFilter != null) {
            view.flowBinderFilter.updatePlace(place);
        }
    }

    /**
     * This method opens a new view, for editing the flowbinder in question
     *
     * @param model The model for the flowbinder to edit
     */
    @Override
    public void editFlowBinder(FlowBinderModel model) {
        placeController.goTo(new EditPlace(model));
    }

    /**
     * This method opens a new view, for creating a new Flowbinder.
     */
    @Override
    public void createFlowBinder() {
        ((SingleSelectionModel<FlowBinderModel>) (view.flowBindersTable.getSelectionModel())).clear();
        placeController.goTo(new CreatePlace());
    }

    /* This method fetches flow binders and hands them over to the view */
    private void fetchFlowBinders() {
        view.flowBindersTable.clear();
        final List<GwtQueryClause> clauses = view.flowBinderFilter.getValue();
        if (clauses.isEmpty()) {
            commonInjector.getFlowStoreProxyAsync().findAllFlowBinders(new FetchFlowBindersCallback());
        } else {
            commonInjector.getFlowStoreProxyAsync().queryFlowBinders(clauses, new FetchFlowBindersCallback());
        }
    }

    /**
     * This method deciphers if a flow binder has been added, updated or deleted.
     * The view and selection model are updated accordingly
     *
     * @param models the list of flow binders returned from flow store proxy
     */
    private void setFlowBindersAndDecipherSelection(Set<FlowBinderModel> dataProviderSet, List<FlowBinderModel> models) {
        if (dataProviderSet.size() > models.size() || dataProviderSet.size() == 0) {
            ((SingleSelectionModel<FlowBinderModel>) (view.flowBindersTable.getSelectionModel())).clear();
            view.flowBindersTable.setFlowBinders(models);
        } else {
            for (FlowBinderModel current : models) {
                if (!dataProviderSet.contains(current)) {
                    view.flowBindersTable.setFlowBinders(models);
                    ((SingleSelectionModel<FlowBinderModel>) (view.flowBindersTable.getSelectionModel())).setSelected(current, true);
                    break;
                }
            }
        }
    }

    /**
     * This class is the callback class for the findAllFlowBinders method in the Flow Store
     */
    protected class FetchFlowBindersCallback extends FilteredAsyncCallback<List<FlowBinderModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<FlowBinderModel> models) {
            setFlowBindersAndDecipherSelection(new HashSet<>(view.flowBindersTable.dataProvider.getList()), models);
        }
    }
}
