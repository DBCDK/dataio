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
import dk.dbc.dataio.gui.client.resource.ImageResources;
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
    private final static dk.dbc.dataio.gui.client.pages.submitter.show.Texts submittersShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.submitter.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flow.show.Texts flowsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flow.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowbinder.show.Texts flowBindersShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowbinder.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.sink.show.Texts sinksShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.sink.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.job.show.Texts jobsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.job.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.javascriptlog.Texts javaScriptLogShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.javascriptlog.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts flowComponentsShowTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flow.modify.Texts flowModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flow.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowcomponent.modify.Texts flowComponentModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowcomponent.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts flowBinderModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.flowbinder.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.sink.modify.Texts sinkModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.sink.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.submitter.modify.Texts submitterModifyTexts = GWT.create(dk.dbc.dataio.gui.client.pages.submitter.modify.Texts.class);
    private final static dk.dbc.dataio.gui.client.pages.faileditems.Texts failedItemsTexts = GWT.create(dk.dbc.dataio.gui.client.pages.faileditems.Texts.class);
    //private final static HarvestersShowTexts harvestersShowTexts = GWT.create(HarvestersShowTexts.class);

    // Image Resources
    private final static ImageResources imageResources = GWT.create(ImageResources.class);

    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);
    public final static com.google.gwt.place.shared.Place NOWHERE = null;

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
    private final dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View flowComponentCreateView = new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View(flowComponentModifyTexts.menu_FlowComponentCreation());
    private final dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View flowComponentEditView = new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.View(flowComponentModifyTexts.menu_FlowComponentEdit());
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.View flowBinderCreateView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.View(flowBinderModifyTexts.menu_FlowBinderCreation());
    private final dk.dbc.dataio.gui.client.pages.flowbinder.modify.View flowBinderEditView = new dk.dbc.dataio.gui.client.pages.flowbinder.modify.View(flowBinderModifyTexts.menu_FlowBinderEdit());
    private final dk.dbc.dataio.gui.client.pages.sink.modify.View sinkCreateView = new dk.dbc.dataio.gui.client.pages.sink.modify.View(sinkModifyTexts.menu_SinkCreation());
    private final dk.dbc.dataio.gui.client.pages.sink.modify.View sinkEditView = new dk.dbc.dataio.gui.client.pages.sink.modify.View(sinkModifyTexts.menu_SinkEdit());
    private final dk.dbc.dataio.gui.client.pages.flowcomponent.show.View flowComponentsShowView = new dk.dbc.dataio.gui.client.pages.flowcomponent.show.View(flowComponentsShowTexts.menu_FlowComponentsShow(), flowComponentsShowTexts);
    private final dk.dbc.dataio.gui.client.pages.flow.show.View flowsShowView = new dk.dbc.dataio.gui.client.pages.flow.show.View(flowsShowTexts.menu_Flows(), flowsShowTexts);
    private final dk.dbc.dataio.gui.client.pages.submitter.show.View submittersShowView = new dk.dbc.dataio.gui.client.pages.submitter.show.View(submittersShowTexts.menu_Submitters(), submittersShowTexts);
    private final dk.dbc.dataio.gui.client.pages.job.show.View jobsShowView = new dk.dbc.dataio.gui.client.pages.job.show.View(jobsShowTexts.menu_Jobs(), jobsShowTexts, imageResources);
    private final dk.dbc.dataio.gui.client.pages.javascriptlog.View javaScriptLogView = new dk.dbc.dataio.gui.client.pages.javascriptlog.View(javaScriptLogShowTexts.menu_JavaScriptLogShow());
    private final dk.dbc.dataio.gui.client.pages.sink.show.View sinksShowView = new dk.dbc.dataio.gui.client.pages.sink.show.View(sinksShowTexts.menu_Sinks(), sinksShowTexts);
    private final dk.dbc.dataio.gui.client.pages.flowbinder.show.View flowBindersShowView = new dk.dbc.dataio.gui.client.pages.flowbinder.show.View(flowBindersShowTexts.menu_FlowBindersShow(), flowBindersShowTexts);
    private final dk.dbc.dataio.gui.client.pages.submitter.modify.View submitterCreateView = new dk.dbc.dataio.gui.client.pages.submitter.modify.View(submitterModifyTexts.menu_SubmitterCreation());
    private final dk.dbc.dataio.gui.client.pages.submitter.modify.View submitterEditView = new dk.dbc.dataio.gui.client.pages.submitter.modify.View(submitterModifyTexts.menu_SubmitterEdit());
    private final dk.dbc.dataio.gui.client.pages.faileditems.View faileditemsView = new dk.dbc.dataio.gui.client.pages.faileditems.View(failedItemsTexts.label_JobId(), failedItemsTexts);
    //private final HarvestersShowView harvestersShowView = new HarvestersShowViewImpl();

    public ClientFactoryImpl() {
        // Submitters Main Menu
        MenuItem createSubmitter = new MenuItem(GUIID_MENU_ITEM_SUBMITTER_CREATE, submitterModifyTexts.menu_SubmitterCreation(), new dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace());
        MenuItem submittersMenu = new MenuItem(GUIID_MENU_ITEM_SUBMITTERS_SHOW, submittersShowTexts.menu_Submitters(), new dk.dbc.dataio.gui.client.pages.submitter.show.Place(),
                                               createSubmitter);

        // Flows Main Menu
        MenuItem createFlow = new MenuItem(GUIID_MENU_ITEM_FLOW_CREATE, flowModifyTexts.menu_FlowCreation(), new dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace());
        MenuItem createFlowComponent = new MenuItem(GUIID_MENU_ITEM_FLOW_COMPONENT_CREATE, flowComponentModifyTexts.menu_FlowComponentCreation(), new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace());
        MenuItem showFlowComponents = new MenuItem(GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW, flowComponentsShowTexts.menu_FlowComponentsShow(), new dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place());
        MenuItem createFlowBinder = new MenuItem(GUIID_MENU_ITEM_FLOWBINDER_CREATE, flowBinderModifyTexts.menu_FlowBinderCreation(), new dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace());
        MenuItem showFlowBinders = new MenuItem(GUIID_MENU_ITEM_FLOW_BINDERS_SHOW, flowBindersShowTexts.menu_FlowBindersShow(), new dk.dbc.dataio.gui.client.pages.flowbinder.show.Place());
        MenuItem flowsMenu = new MenuItem(GUIID_MENU_ITEM_FLOWS_SHOW, flowsShowTexts.menu_Flows(), new dk.dbc.dataio.gui.client.pages.flow.show.Place(),
                                          createFlow,
                                          createFlowComponent,
                                          showFlowComponents,
                                          createFlowBinder,
                                          showFlowBinders);

        MenuItem createSink = new MenuItem(GUIID_MENU_ITEM_SINK_CREATE, sinkModifyTexts.menu_SinkCreation(), new dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace());
        MenuItem sinksMenu = new MenuItem(GUIID_MENU_ITEM_SINKS_SHOW, sinksShowTexts.menu_Sinks(), new dk.dbc.dataio.gui.client.pages.sink.show.Place(),
                createSink);

        // Jobs Main Menu
        MenuItem jobsMenu = new MenuItem(GUIID_MENU_ITEM_JOBS_SHOW, jobsShowTexts.menu_Jobs(), new dk.dbc.dataio.gui.client.pages.job.show.Place());

        // Harvesters Main Menu
        // MenuItem harvestersMenu = new MenuItem(GUIID_MENU_ITEM_HARVESTERS_SHOW, harvestersShowTexts.menu_Harvesters(), new dk.dbc.dataio.gui.client.pages.harvester.show.HarvestersShowPlace());

        // Toplevel Main Menu Container
        menuStructure = new MenuItem("toplevelmainmenu", "Toplevel Main Menu", NOWHERE,
                                     jobsMenu,
                                     submittersMenu,
                                     flowsMenu,
                                     sinksMenu
                                     /*harvestersMenu*/);
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
