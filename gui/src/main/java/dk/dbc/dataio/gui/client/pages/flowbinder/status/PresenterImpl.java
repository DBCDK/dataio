package dk.dbc.dataio.gui.client.pages.flowbinder.status;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowBinderUsage;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.logging.Logger;


import java.util.List;

public class PresenterImpl extends AbstractActivity implements Presenter {
    private final Logger logger = Logger.getLogger(PresenterImpl.class.getName());
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    private PlaceController placeController;
    private String header;

    /**
     * Default constructor
     *
     * @param placeController The Placecontroller
     * @param header          breadcrumb Header text
     */
    public PresenterImpl(PlaceController placeController, String header) {
        logger.info("Presenter:"+header);
        this.placeController = placeController;
        this.header = header;
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        getView().setHeader(this.header);
        getView().setPresenter(this);
        containerWidget.setWidget(getView().asWidget());
        getFlowBindersUsageCached();
    }

    @Override
    public void showFlowBinder(long id) {
        EditPlace editPlace = new EditPlace(Long.toString(id));
        placeController.goTo(editPlace);
    }

    @Override
    public void getFlowBindersUsage() {
        commonInjector.getFlowStoreProxyAsync().getFlowBindersUsage(new GetFlowBindersUsageListFilteredAsyncCallback());
    }
    @Override
    public void getFlowBindersUsageCached() {
        commonInjector.getFlowStoreProxyAsync().getFlowBindersUsageCached(new GetFlowBindersUsageListFilteredAsyncCallback());
    }


    class GetFlowBindersUsageListFilteredAsyncCallback extends FilteredAsyncCallback<List<FlowBinderUsage>> {

        @Override
        public void onFilteredFailure(Throwable throwable) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(throwable, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(List<FlowBinderUsage> flowBinderUsages) {
            getView().setFlowbinderStatus(flowBinderUsages);
        }
    }

    private View getView() {
        return viewInjector.getView();
    }

}
