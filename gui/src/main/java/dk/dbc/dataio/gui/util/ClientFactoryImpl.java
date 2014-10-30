package dk.dbc.dataio.gui.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.pages.flow.show.FlowsShowActivity;
import dk.dbc.dataio.gui.client.pages.flow.show.FlowsShowPlace;
import dk.dbc.dataio.gui.client.pages.flow.show.FlowsShowTexts;
import dk.dbc.dataio.gui.client.pages.flow.show.FlowsShowView;
import dk.dbc.dataio.gui.client.pages.flow.show.FlowsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowActivity;
import dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowPlace;
import dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowTexts;
import dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowView;
import dk.dbc.dataio.gui.client.pages.flowbinder.show.FlowBindersShowViewImpl;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentCreateActivity;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentCreateEditTexts;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentCreateEditView;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentCreateEditViewImpl;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentEditActivity;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentEditPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.show.FlowComponentsShowActivity;
import dk.dbc.dataio.gui.client.pages.flowcomponent.show.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.show.FlowComponentsShowTexts;
import dk.dbc.dataio.gui.client.pages.flowcomponent.show.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.pages.flowcomponent.show.FlowComponentsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.javascriptlog.JavaScriptLogPlace;
import dk.dbc.dataio.gui.client.pages.javascriptlog.PresenterImpl;
import dk.dbc.dataio.gui.client.pages.job.show.JobsShowActivity;
import dk.dbc.dataio.gui.client.pages.job.show.JobsShowPlace;
import dk.dbc.dataio.gui.client.pages.job.show.JobsShowTexts;
import dk.dbc.dataio.gui.client.pages.job.show.JobsShowView;
import dk.dbc.dataio.gui.client.pages.job.show.JobsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.sink.show.SinksShowActivity;
import dk.dbc.dataio.gui.client.pages.sink.show.SinksShowPlace;
import dk.dbc.dataio.gui.client.pages.sink.show.SinksShowTexts;
import dk.dbc.dataio.gui.client.pages.sink.show.SinksShowView;
import dk.dbc.dataio.gui.client.pages.sink.show.SinksShowViewImpl;
import dk.dbc.dataio.gui.client.pages.submitter.show.SubmittersShowActivity;
import dk.dbc.dataio.gui.client.pages.submitter.show.SubmittersShowPlace;
import dk.dbc.dataio.gui.client.pages.submitter.show.SubmittersShowTexts;
import dk.dbc.dataio.gui.client.pages.submitter.show.SubmittersShowView;
import dk.dbc.dataio.gui.client.pages.submitter.show.SubmittersShowViewImpl;
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
import dk.dbc.dataio.gui.client.views.MenuItem;

public class ClientFactoryImpl implements ClientFactory {

    // Menu Item Id's
    public final static String GUIID_MENU_ITEM_SUBMITTERS_SHOW = "menuitemsubmittersshow";
    public final static String GUIID_MENU_ITEM_FLOWS_SHOW = "menuitemflowsshow";
    public final static String GUIID_MENU_ITEM_SINKS_SHOW = "menuitemsinksshow";
    public final static String GUIID_MENU_ITEM_JOBS_SHOW = "menuitemjobsshow";
    public final static String GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW = "menuitemflowcomponentsshow";
    public final static String GUIID_MENU_ITEM_FLOW_BINDERS_SHOW = "menuitemflowbindersshow";
    public final static String GUIID_MENU_ITEM_SUBMITTER_CREATE = "menuitemsubmittercreate";
    public final static String GUIID_MENU_ITEM_FLOW_CREATE = "menuitemflowcreate";
    public final static String GUIID_MENU_ITEM_FLOW_COMPONENT_CREATE = "menuitemflowcomponentcreate";
    public final static String GUIID_MENU_ITEM_FLOWBINDER_CREATE = "menuitemflowbindercreate";
    public final static String GUIID_MENU_ITEM_SINK_CREATE = "menuitemsinkcreate";
    //public final static String GUIID_MENU_ITEM_HARVESTERS_SHOW = "menuitemharvestersshow";

    // Menu texts constants declarations
    private final static SubmittersShowTexts submittersShowTexts = GWT.create(SubmittersShowTexts.class);
    private final static FlowsShowTexts flowsShowTexts = GWT.create(FlowsShowTexts.class);
    private final static FlowBindersShowTexts flowBindersShowTexts = GWT.create(FlowBindersShowTexts.class);
    private final static SinksShowTexts sinksShowTexts = GWT.create(SinksShowTexts.class);
    private final static JobsShowTexts jobsShowTexts = GWT.create(JobsShowTexts.class);
    private final static dk.dbc.dataio.gui.client.pages.javascriptlog.Texts javaScriptLogShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.javascriptlog.Texts.class);
    private final static FlowComponentsShowTexts flowComponentsShowTexts = GWT.create(FlowComponentsShowTexts.class);
    private final static dk.dbc.dataio.gui.client.pages.flow.modify.Texts flowModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flow.modify.Texts.class);
    private final static FlowComponentCreateEditTexts flowComponentCreateEditTexts = GWT.create(FlowComponentCreateEditTexts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts flowBinderModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.sink.modify.Texts sinkModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.sink.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.submitter.modify.Texts submitterModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.submitter.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.faileditems.Texts failedItemsTexts = GWT.create(dk.dbc.dataio.gui.client.pages.faileditems.Texts.class);
    //private final static HarvestersShowTexts harvestersShowTexts = GWT.create(HarvestersShowTexts.class);

    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);
    public final static Place NOWHERE = null;

    // History Mapper
    private final AppPlaceHistoryMapper historyMapper = GWT.create(AppPlaceHistoryMapper.class);

    // Proxies
    private final FlowStoreProxyAsync flowStoreProxyAsync = FlowStoreProxy.Factory.getAsyncInstance();
    private final JavaScriptProjectFetcherAsync javaScriptProjectFetcher = JavaScriptProjectFetcher.Factory.getAsyncInstance();
    private final SinkServiceProxyAsync sinkServiceProxyAsync = SinkServiceProxy.Factory.getAsyncInstance();
    private final JobStoreProxyAsync jobStoreProxyAsync = JobStoreProxy.Factory.getAsyncInstance();
    private final LogStoreProxyAsync logStoreProxyAsync = LogStoreProxy.Factory.getAsyncInstance();

    // Menu Structure
    private final MenuItem menuStructure;

    // Views
    private final dk.dbc.dataio.gui.client.pages.flow.modify.View flowCreateView = new dk.dbc.dataio.gui.client.pages.flow.modify.ViewImpl(flowModifyTexts.menu_FlowCreation(), flowModifyTexts);
    private final dk.dbc.dataio.gui.client.pages.flow.modify.View flowEditView = new dk.dbc.dataio.gui.client.pages.flow.modify.ViewImpl(flowModifyTexts.menu_FlowEdit(), flowModifyTexts);
    private final FlowComponentCreateEditView flowComponentCreateEditView = new FlowComponentCreateEditViewImpl();
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.View flowBinderCreateView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.View(flowBinderModifyTexts.menu_FlowBinderCreation());
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.View flowBinderEditView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.View(flowBinderModifyTexts.menu_FlowBinderEdit());
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.View flowbinderEditView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.View(flowBinderModifyTexts.menu_FlowBinderEdit());
    private final dk.dbc.dataio.gui.client.pages.sink.modify.View sinkCreateView = new dk.dbc.dataio.gui.client.pages.sink.modify.ViewImpl(sinkModifyTexts.menu_SinkCreation(), sinkModifyTexts);
    private final dk.dbc.dataio.gui.client.pages.sink.modify.View sinkEditView = new dk.dbc.dataio.gui.client.pages.sink.modify.ViewImpl(sinkModifyTexts.menu_SinkEdit(), sinkModifyTexts);
    private final FlowComponentsShowView flowComponentsShowView = new FlowComponentsShowViewImpl();
    private final FlowsShowView flowsShowView = new FlowsShowViewImpl();
    private final SubmittersShowView submittersShowView = new SubmittersShowViewImpl();
    private final JobsShowView jobsShowView = new JobsShowViewImpl();
    private final dk.dbc.dataio.gui.client.pages.javascriptlog.View javaScriptLogView = new dk.dbc.dataio.gui.client.pages.javascriptlog.View(javaScriptLogShowTexts.menu_JavaScriptLogShow());
    private final SinksShowView sinksShowView = new SinksShowViewImpl();
    private final FlowBindersShowView flowBindersShowView = new FlowBindersShowViewImpl();
    private final dk.dbc.dataio.gui.client.pages.submitter.modify.ViewImpl submitterCreateView = new dk.dbc.dataio.gui.client.pages.submitter.modify.ViewImpl(submitterModifyTexts.menu_SubmitterCreation(), submitterModifyTexts);
    private final dk.dbc.dataio.gui.client.pages.submitter.modify.ViewImpl submitterEditView = new dk.dbc.dataio.gui.client.pages.submitter.modify.ViewEditImpl(submitterModifyTexts.menu_SubmitterEdit(), submitterModifyTexts);
    private final dk.dbc.dataio.gui.client.pages.faileditems.View faileditemsView = new dk.dbc.dataio.gui.client.pages.faileditems.View(failedItemsTexts.label_JobId(), failedItemsTexts);
    //private final HarvestersShowView harvestersShowView = new HarvestersShowViewImpl();

    public ClientFactoryImpl() {
        // Submitters Main Menu
        MenuItem createSubmitter = new MenuItem(GUIID_MENU_ITEM_SUBMITTER_CREATE, submitterModifyTexts.menu_SubmitterCreation(), new dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace());
        MenuItem submittersMenu = new MenuItem(GUIID_MENU_ITEM_SUBMITTERS_SHOW, submittersShowTexts.menu_Submitters(), new SubmittersShowPlace(),
                                               createSubmitter);

        // Flows Main Menu
        MenuItem createFlow = new MenuItem(GUIID_MENU_ITEM_FLOW_CREATE, flowModifyTexts.menu_FlowCreation(), new dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace());
        MenuItem createFlowComponent = new MenuItem(GUIID_MENU_ITEM_FLOW_COMPONENT_CREATE, flowComponentCreateEditTexts.menu_FlowComponentCreation(), new FlowComponentCreatePlace());
        MenuItem showFlowComponents = new MenuItem(GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW, flowComponentsShowTexts.menu_FlowComponentsShow(), new FlowComponentsShowPlace());
        MenuItem createFlowBinder = new MenuItem(GUIID_MENU_ITEM_FLOWBINDER_CREATE, flowBinderModifyTexts.menu_FlowBinderCreation(), new dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace());
        MenuItem showFlowBinders = new MenuItem(GUIID_MENU_ITEM_FLOW_BINDERS_SHOW, flowBindersShowTexts.menu_FlowBindersShow(), new FlowBindersShowPlace());
        MenuItem flowsMenu = new MenuItem(GUIID_MENU_ITEM_FLOWS_SHOW, flowsShowTexts.menu_Flows(), new FlowsShowPlace(),
                                          createFlow,
                                          createFlowComponent,
                                          showFlowComponents,
                                          createFlowBinder,
                                          showFlowBinders);

        MenuItem createSink = new MenuItem(GUIID_MENU_ITEM_SINK_CREATE, sinkModifyTexts.menu_SinkCreation(), new dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace());
        MenuItem sinksMenu = new MenuItem(GUIID_MENU_ITEM_SINKS_SHOW, sinksShowTexts.menu_Sinks(), new SinksShowPlace(),
                createSink);

        // Jobs Main Menu
        MenuItem jobsMenu = new MenuItem(GUIID_MENU_ITEM_JOBS_SHOW, jobsShowTexts.menu_Jobs(), new JobsShowPlace());

        // Harvesters Main Menu
        //MenuItem harvestersMenu = new MenuItem(GUIID_MENU_ITEM_HARVESTERS_SHOW, harvestersShowTexts.menu_Harvesters(), new HarvestersShowPlace());

        // Toplevel Main Menu Container
        menuStructure = new MenuItem("toplevelmainmenu", "Toplevel Main Menu", NOWHERE,
                                     submittersMenu,
                                     flowsMenu,
                                     sinksMenu,
                                     jobsMenu/*,
                                     harvestersMenu*/);
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
    public com.google.gwt.activity.shared.Activity getPresenter(Place place) {
        if (place instanceof dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.flow.modify.PresenterCreateImpl(this, flowModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace) {
            return new dk.dbc.dataio.gui.client.pages.flow.modify.PresenterEditImpl(place, this, flowModifyTexts);
        }
        if (place instanceof FlowComponentCreatePlace) {
            return new FlowComponentCreateActivity(this);
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
        if (place instanceof FlowComponentEditPlace) {
            return new FlowComponentEditActivity(place,this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.sink.modify.PresenterCreateImpl(this, sinkModifyTexts);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace) {
            return new dk.dbc.dataio.gui.client.pages.sink.modify.PresenterEditImpl(place, this, sinkModifyTexts);
        }
        if (place instanceof FlowComponentsShowPlace) {
            return new FlowComponentsShowActivity(this);
        }
        if (place instanceof FlowsShowPlace) {
            return new FlowsShowActivity(this);
        }
        if (place instanceof SubmittersShowPlace) {
            return new SubmittersShowActivity(this);
        }
        if (place instanceof JobsShowPlace) {
            return new JobsShowActivity(this);
        }
        if (place instanceof JavaScriptLogPlace) {
            return new PresenterImpl(place, this, javaScriptLogShowTexts);
        }
        if (place instanceof SinksShowPlace) {
            return new SinksShowActivity(this);
        }
        if (place instanceof FlowBindersShowPlace) {
            return new FlowBindersShowActivity(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.faileditems.ShowPlace) {
            return new dk.dbc.dataio.gui.client.pages.faileditems.PresenterImpl(place, this, failedItemsTexts);
        }
//        if (place instanceof HarvestersShowPlace) {
//            return new HarvestersShowActivity(this);
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

    // Menu Structure
    @Override
    public MenuItem getMenuStructure() {
        return menuStructure;
    }

    // Views
    @Override
    public dk.dbc.dataio.gui.client.pages.flow.modify.View getFlowCreateView() {
        return flowCreateView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.flow.modify.View getFlowEditView() {
        return flowEditView;
    }

    @Override
    public FlowComponentCreateEditView getFlowComponentCreateEditView() {
        return flowComponentCreateEditView;
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
        return flowbinderEditView;
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
    public FlowComponentsShowView getFlowComponentsShowView() {
        return flowComponentsShowView;
    }

    @Override
    public FlowsShowView getFlowsShowView() {
        return flowsShowView;
    }

    @Override
    public SubmittersShowView getSubmittersShowView() {
        return submittersShowView;
    }

    @Override
    public JobsShowView getJobsShowView() {
        return jobsShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.javascriptlog.View getJavaScriptLogView() {
        return javaScriptLogView;
    }

    @Override
    public SinksShowView getSinksShowView() {
        return sinksShowView;
    }

    @Override
    public FlowBindersShowView getFlowBindersShowView() {
        return flowBindersShowView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.faileditems.View getFaileditemsView() {
        return faileditemsView;
    }

//    @Override
//    public HarvestersShowView getHarvestersShowView() {
//        return harvestersShowView;
//    }

}
