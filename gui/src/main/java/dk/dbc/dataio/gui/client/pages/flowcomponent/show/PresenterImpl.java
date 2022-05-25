package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class represents the show flow components presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    CommonGinjector commonGinjector = GWT.create(CommonGinjector.class);
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);

    private PlaceController placeController;
    private String header;

    /**
     * Default constructor
     *
     * @param placeController PlaceController for navigation
     * @param header          Breadcrumb header text
     */
    public PresenterImpl(PlaceController placeController, String header) {
        this.placeController = placeController;
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
        getView().setPresenter(this);
        getView().setHeader(this.header);
        containerWidget.setWidget(getView().asWidget());
        fetchFlowComponents();
    }


    /**
     * This method opens a new view, for editing the flow component in question
     *
     * @param model The model for the flow component to edit
     */
    @Override
    public void editFlowComponent(FlowComponentModel model) {
        placeController.goTo(new EditPlace(model));
    }

    /**
     * This method opens a new view, for creating a new flow component
     */
    @Override
    public void createFlowComponent() {
        getView().selectionModel.clear();
        placeController.goTo(new CreatePlace());
    }

    /*
     * Private methods
     */

    /**
     * This method fetches all flow components, and sends them to the view
     */
    private void fetchFlowComponents() {
        commonGinjector.getFlowStoreProxyAsync().findAllFlowComponents(new FetchFlowComponentsCallback());
    }

    /**
     * This method deciphers if a flow component has been added, updated or deleted.
     * The view and selection model are updated accordingly
     *
     * @param models the list of flow components returned from flow store proxy
     */
    private void setFlowComponentsAndDecipherSelection(Set<FlowComponentModel> dataProviderSet, List<FlowComponentModel> models) {
        if (dataProviderSet.size() > models.size() || dataProviderSet.size() == 0) {
            getView().selectionModel.clear();
            getView().setFlowComponents(models);
        } else {
            for (FlowComponentModel current : models) {
                if (!dataProviderSet.contains(current)) {
                    getView().setFlowComponents(models);
                    getView().selectionModel.setSelected(current, true);
                    break;
                }
            }
        }
    }

    /*
     * Private classes
     */
    View getView() {
        return viewGinjector.getView();
    }

    Texts getTexts() {
        return viewGinjector.getTexts();
    }

    /**
     * This class is the callback class for the findAllFlowComponents method in the Flow Store
     */
    protected class FetchFlowComponentsCallback extends FilteredAsyncCallback<List<FlowComponentModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonGinjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<FlowComponentModel> models) {
            setFlowComponentsAndDecipherSelection(new HashSet<FlowComponentModel>(getView().dataProvider.getList()), models);
        }
    }

}
