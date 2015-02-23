package dk.dbc.dataio.gui.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.places.AppPlaceHistoryMapper;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxy;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;

public class ClientFactoryImpl implements ClientFactory {

    // Menu texts constants declarations
    private final static dk.dbc.dataio.gui.client.pages.navigation.Texts menuTexts = GWT.create(dk.dbc.dataio.gui.client.pages.navigation.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.submitter.show.Texts submittersShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.submitter.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flow.show.Texts flowsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flow.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowbinder.show.Texts flowBindersShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowbinder.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.sink.show.Texts sinksShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.sink.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.job.show.Texts jobsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.job.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.newJob.show.Texts newJobsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.newJob.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.javascriptlog.Texts javaScriptLogShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.javascriptlog.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts flowComponentsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flow.oldmodify.Texts oldFlowModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flow.oldmodify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flow.modify.Texts flowModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flow.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowcomponent.modify.Texts flowComponentModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowcomponent.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts flowBinderModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.sink.modify.Texts sinkModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.sink.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.submitter.modify.Texts submitterModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.submitter.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.faileditems.Texts failedItemsTexts = GWT.create(dk.dbc.dataio.gui.client.pages.faileditems.Texts.class);
    //private final static HarvestersShowTexts harvestersShowTexts = GWT.create(HarvestersShowTexts.class);

    // Image Resources
    private final static Resources RESOURCES = GWT.create(Resources.class);

    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);

    // History Mapper
    private final AppPlaceHistoryMapper historyMapper = GWT.create(AppPlaceHistoryMapper.class);

    // Proxies
    private final FlowStoreProxyAsync flowStoreProxyAsync = FlowStoreProxy.Factory.getAsyncInstance();
    private final JavaScriptProjectFetcherAsync javaScriptProjectFetcher = JavaScriptProjectFetcher.Factory.getAsyncInstance();
    private final SinkServiceProxyAsync sinkServiceProxyAsync = SinkServiceProxy.Factory.getAsyncInstance();
    private final JobStoreProxyAsync jobStoreProxyAsync = JobStoreProxy.Factory.getAsyncInstance();
    private final LogStoreProxyAsync logStoreProxyAsync = LogStoreProxy.Factory.getAsyncInstance();

    // Views
    private final dk.dbc.dataio.gui.client.pages.flow.oldmodify.View oldFlowCreateView = new dk.dbc.dataio.gui.client.pages.flow.oldmodify.ViewImpl(menuTexts.menu_FlowCreation(), oldFlowModifyTexts);
    private final dk.dbc.dataio.gui.client.pages.flow.oldmodify.View oldFlowEditView = new dk.dbc.dataio.gui.client.pages.flow.oldmodify.ViewImpl(menuTexts.menu_FlowEdit(), oldFlowModifyTexts);
    private final dk.dbc.dataio.gui.client.pages.flow.modify.View flowCreateView = new dk.dbc.dataio.gui.client.pages.flow.modify.View(menuTexts.menu_FlowCreation());
    private final dk.dbc.dataio.gui.client.pages.flow.modify.View flowEditView = new dk.dbc.dataio.gui.client.pages.flow.modify.View(menuTexts.menu_FlowEdit());
    private final dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View flowComponentCreateView = new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View(menuTexts.menu_FlowComponentCreation());
    private final dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View flowComponentEditView = new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View(menuTexts.menu_FlowComponentEdit());
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.View flowBinderCreateView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.View(menuTexts.menu_FlowBinderCreation());
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.View flowBinderEditView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.View(menuTexts.menu_FlowBinderEdit());
    private final dk.dbc.dataio.gui.client.pages.sink.modify.View sinkCreateView = new dk.dbc.dataio.gui.client.pages.sink.modify.View(menuTexts.menu_SinkCreation());
    private final dk.dbc.dataio.gui.client.pages.sink.modify.View sinkEditView = new dk.dbc.dataio.gui.client.pages.sink.modify.View(menuTexts.menu_SinkEdit());
    private final dk.dbc.dataio.gui.client.pages.flowcomponent.show.View flowComponentsShowView = new dk.dbc.dataio.gui.client.pages.flowcomponent.show.View(menuTexts.menu_FlowComponents(), flowComponentsShowTexts);
    private final dk.dbc.dataio.gui.client.pages.flow.show.View flowsShowView = new dk.dbc.dataio.gui.client.pages.flow.show.View(menuTexts.menu_Flows(), flowsShowTexts);
    private final dk.dbc.dataio.gui.client.pages.submitter.show.View submittersShowView = new dk.dbc.dataio.gui.client.pages.submitter.show.View(menuTexts.menu_Submitters(), submittersShowTexts);
    private final dk.dbc.dataio.gui.client.pages.job.show.View jobsShowView = new dk.dbc.dataio.gui.client.pages.job.show.View(menuTexts.menu_Jobs(), jobsShowTexts, RESOURCES);
    private final dk.dbc.dataio.gui.client.pages.newJob.show.View newJobsShowView = new dk.dbc.dataio.gui.client.pages.newJob.show.View(menuTexts.menu_Jobs(), newJobsShowTexts, RESOURCES);
    private final dk.dbc.dataio.gui.client.pages.javascriptlog.View javaScriptLogView = new dk.dbc.dataio.gui.client.pages.javascriptlog.View(menuTexts.menu_JavaScriptLogShow());
    private final dk.dbc.dataio.gui.client.pages.sink.show.View sinksShowView = new dk.dbc.dataio.gui.client.pages.sink.show.View(menuTexts.menu_Sinks(), sinksShowTexts);
    private final dk.dbc.dataio.gui.client.pages.flowbinder.show.View flowBindersShowView = new dk.dbc.dataio.gui.client.pages.flowbinder.show.View(menuTexts.menu_FlowBinders(), flowBindersShowTexts);
    private final dk.dbc.dataio.gui.client.pages.submitter.modify.View submitterCreateView = new dk.dbc.dataio.gui.client.pages.submitter.modify.View(menuTexts.menu_SubmitterCreation());
    private final dk.dbc.dataio.gui.client.pages.submitter.modify.View submitterEditView = new dk.dbc.dataio.gui.client.pages.submitter.modify.View(menuTexts.menu_SubmitterEdit());
    private final dk.dbc.dataio.gui.client.pages.faileditems.View faileditemsView = new dk.dbc.dataio.gui.client.pages.faileditems.View(failedItemsTexts.label_JobId(), failedItemsTexts);
    //private final HarvestersShowView harvestersShowView = new HarvestersShowViewImpl();

    public ClientFactoryImpl() {
    }

    // Event Bus
    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    // Place Controller
    @Override
    public PlaceController getPlaceController() {
        return placeController;
    }

    // History Mapper
    @Override
    public AppPlaceHistoryMapper getHistoryMapper() {
        return historyMapper;
    }

    // getPresenter
    @Override
    public com.google.gwt.activity.shared.Activity getPresenter(com.google.gwt.place.shared.Place place) {
        if (place instanceof dk.dbc.dataio.gui.client.pages.flow.oldmodify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.flow.oldmodify.PresenterCreateImpl(this, oldFlowModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flow.oldmodify.EditPlace) {
            return new dk.dbc.dataio.gui.client.pages.flow.oldmodify.PresenterEditImpl(place, this, oldFlowModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.flow.modify.PresenterCreateImpl(this, flowModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace) {
            return new dk.dbc.dataio.gui.client.pages.flow.modify.PresenterEditImpl(place, this, flowModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.PresenterCreateImpl(this, flowComponentModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditPlace) {
            return new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.PresenterEditImpl(place, this, flowComponentModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.submitter.modify.PresenterCreateImpl(this, submitterModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace) {
            return new dk.dbc.dataio.gui.client.pages.submitter.modify.PresenterEditImpl(place, this, submitterModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.flowbinder.modify.PresenterCreateImpl(this, flowBinderModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace) {
            return new dk.dbc.dataio.gui.client.pages.flowbinder.modify.PresenterEditImpl(place, this, flowBinderModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.sink.modify.PresenterCreateImpl(this, sinkModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace) {
            return new dk.dbc.dataio.gui.client.pages.sink.modify.PresenterEditImpl(place, this, sinkModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place) {
            return new dk.dbc.dataio.gui.client.pages.flowcomponent.show.PresenterImpl(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flow.show.Place) {
            return new dk.dbc.dataio.gui.client.pages.flow.show.PresenterImpl(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.submitter.show.Place) {
            return new dk.dbc.dataio.gui.client.pages.submitter.show.PresenterImpl(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.job.show.Place) {
            return new dk.dbc.dataio.gui.client.pages.job.show.PresenterImpl(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.newJob.show.Place) {
            return new dk.dbc.dataio.gui.client.pages.newJob.show.PresenterImpl(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.javascriptlog.JavaScriptLogPlace) {
            return new dk.dbc.dataio.gui.client.pages.javascriptlog.PresenterImpl(place, this, javaScriptLogShowTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.sink.show.Place) {
            return new dk.dbc.dataio.gui.client.pages.sink.show.PresenterImpl(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flowbinder.show.Place) {
            return new dk.dbc.dataio.gui.client.pages.flowbinder.show.PresenterImpl(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.faileditems.ShowPlace) {
            return new dk.dbc.dataio.gui.client.pages.faileditems.PresenterImpl(place, this, failedItemsTexts);
        }
//        if (place instanceof dk.dbc.dataio.gui.client.pages.harvester.show.HarvestersShowPlace) {
//            return new dk.dbc.dataio.gui.client.pages.harvester.show.HarvestersShowActivity(this);
//        }
        return null;
    }

    // Proxies
    @Override
    public FlowStoreProxyAsync getFlowStoreProxyAsync() {
        return flowStoreProxyAsync;
    }

    @Override
    public JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync() {
        return javaScriptProjectFetcher;
    }

    @Override
    public SinkServiceProxyAsync getSinkServiceProxyAsync() {
        return sinkServiceProxyAsync;
    }

    @Override
    public JobStoreProxyAsync getJobStoreProxyAsync() {
        return jobStoreProxyAsync;
    }

    @Override
    public LogStoreProxyAsync getLogStoreProxyAsync() {
        return logStoreProxyAsync;
    }

    // Views
    @Override
    public dk.dbc.dataio.gui.client.pages.flow.oldmodify.View getOldFlowCreateView() {
        return oldFlowCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.oldmodify.View getOldFlowEditView() {
        return oldFlowEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.modify.View getFlowCreateView() {
        return flowCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.modify.View getFlowEditView() {
        return flowEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View getFlowComponentCreateView() {
        return flowComponentCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View getFlowComponentEditView() {
        return flowComponentEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.submitter.modify.View getSubmitterCreateView() {
        return submitterCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.submitter.modify.View getSubmitterEditView() {
        return submitterEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowbinder.modify.View getFlowBinderCreateView() {
        return flowBinderCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowbinder.modify.View getFlowBinderEditView() {
        return flowBinderEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.sink.modify.View getSinkCreateView() {
        return sinkCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.sink.modify.View getSinkEditView() {
        return sinkEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowcomponent.show.View getFlowComponentsShowView() {
        return flowComponentsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.show.View getFlowsShowView() {
        return flowsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.submitter.show.View getSubmittersShowView() {
        return submittersShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.job.show.View getJobsShowView() {
        return jobsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.newJob.show.View getNewJobsShowView() {
        return newJobsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.javascriptlog.View getJavaScriptLogView() {
        return javaScriptLogView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.sink.show.View getSinksShowView() {
        return sinksShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flowbinder.show.View getFlowBindersShowView() {
        return flowBindersShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.faileditems.View getFaileditemsView() {
        return faileditemsView;
    }

//    @Override
//    public dk.dbc.dataio.gui.client.pages.harvester.show.HarvestersShowView getHarvestersShowView() {
//        return harvestersShowView;
//    }

}
