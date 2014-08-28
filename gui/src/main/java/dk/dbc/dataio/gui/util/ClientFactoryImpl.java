package dk.dbc.dataio.gui.util;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateActivity;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateConstants;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateView;
import dk.dbc.dataio.gui.client.pages.flowbindercreate.FlowbinderCreateViewImpl;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowActivity;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowConstants;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowPlace;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowView;
import dk.dbc.dataio.gui.client.pages.flowbindersshow.FlowBindersShowViewImpl;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentCreateActivity;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentCreateEditConstants;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentCreateEditView;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentCreateEditViewImpl;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentEditActivity;
import dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit.FlowComponentEditPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowActivity;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowConstants;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowPlace;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowView;
import dk.dbc.dataio.gui.client.pages.flowcomponentsshow.FlowComponentsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateActivity;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateConstants;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateView;
import dk.dbc.dataio.gui.client.pages.flowcreate.FlowCreateViewImpl;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowActivity;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowConstants;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowPlace;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowView;
import dk.dbc.dataio.gui.client.pages.flowsshow.FlowsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowActivity;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowConstants;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowPlace;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowView;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowViewImpl;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreateActivity;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreateEditConstants;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreateEditView;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreateEditViewImpl;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkCreatePlace;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkEditActivity;
import dk.dbc.dataio.gui.client.pages.sinkcreateedit.SinkEditPlace;
import dk.dbc.dataio.gui.client.pages.sinkmodify.SinkModifyConstants;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowActivity;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowConstants;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowPlace;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowView;
import dk.dbc.dataio.gui.client.pages.sinksshow.SinksShowViewImpl;
import dk.dbc.dataio.gui.client.pages.submittermodify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submittermodify.EditPlace;
import dk.dbc.dataio.gui.client.pages.submittermodify.PresenterCreateImpl;
import dk.dbc.dataio.gui.client.pages.submittermodify.PresenterEditImpl;
import dk.dbc.dataio.gui.client.pages.submittermodify.SubmitterModifyConstants;
import dk.dbc.dataio.gui.client.pages.submittermodify.View;
import dk.dbc.dataio.gui.client.pages.submittermodify.ViewImpl;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowActivity;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowConstants;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowPlace;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowView;
import dk.dbc.dataio.gui.client.pages.submittersshow.SubmittersShowViewImpl;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
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
    private final static SubmittersShowConstants submittersShowConstants = GWT.create(SubmittersShowConstants.class);
    private final static FlowsShowConstants flowsShowConstants = GWT.create(FlowsShowConstants.class);
    private final static FlowBindersShowConstants flowBindersShowConstants = GWT.create(FlowBindersShowConstants.class);
    private final static SinksShowConstants sinksShowConstants = GWT.create(SinksShowConstants.class);
    private final static JobsShowConstants jobsShowConstants = GWT.create(JobsShowConstants.class);
    private final static FlowComponentsShowConstants flowComponentsShowConstants = GWT.create(FlowComponentsShowConstants.class);
    private final static FlowCreateConstants flowCreateConstants = GWT.create(FlowCreateConstants.class);
    private final static FlowComponentCreateEditConstants flowComponentCreateEditConstants = GWT.create(FlowComponentCreateEditConstants.class);
    private final static FlowbinderCreateConstants flowbinderCreateConstants = GWT.create(FlowbinderCreateConstants.class);
    private final static SinkCreateEditConstants SinkCreateEditConstants = GWT.create(SinkCreateEditConstants.class);
    private final static SinkModifyConstants sinkModifyConstants = GWT.create(SinkModifyConstants.class);
    private final static SubmitterModifyConstants submitterModifyConstants = GWT.create(SubmitterModifyConstants.class);
    //private final static HarvestersShowConstants harvestersShowConstants = GWT.create(HarvestersShowConstants.class);

    // Event Bus
    private final EventBus eventBus = new SimpleEventBus();

    // Place Controller
    private final PlaceController placeController = new PlaceController(eventBus);
    public final static Place NOWHERE = null;

    // Proxies
    private final FlowStoreProxyAsync flowStoreProxyAsync = FlowStoreProxy.Factory.getAsyncInstance();
    private final JavaScriptProjectFetcherAsync javaScriptProjectFetcher = JavaScriptProjectFetcher.Factory.getAsyncInstance();
    private final SinkServiceProxyAsync sinkServiceProxyAsync = SinkServiceProxy.Factory.getAsyncInstance();
    private final JobStoreProxyAsync jobStoreProxyAsync = JobStoreProxy.Factory.getAsyncInstance();

    // Menu Structure
    private final MenuItem menuStructure;

    // Views
    private final FlowCreateView flowCreateView = new FlowCreateViewImpl();
    private final FlowComponentCreateEditView flowComponentCreateEditView = new FlowComponentCreateEditViewImpl();
    private final FlowbinderCreateView flowbinderCreateView = new FlowbinderCreateViewImpl();
    private final SinkCreateEditView sinkCreateEditView = new SinkCreateEditViewImpl();
    private final dk.dbc.dataio.gui.client.pages.sinkmodify.View sinkCreateView =
            new dk.dbc.dataio.gui.client.pages.sinkmodify.ViewImpl(sinkModifyConstants.menu_SinkCreation(), sinkModifyConstants);
    private final FlowComponentsShowView flowComponentsShowView = new FlowComponentsShowViewImpl();
    private final FlowsShowView flowsShowView = new FlowsShowViewImpl();
    private final SubmittersShowView submittersShowView = new SubmittersShowViewImpl();
    private final JobsShowView jobsShowView = new JobsShowViewImpl();
    private final SinksShowView sinksShowView = new SinksShowViewImpl();
    private final FlowBindersShowView flowBindersShowView = new FlowBindersShowViewImpl();
    private final ViewImpl submitterCreateView = new ViewImpl(submitterModifyConstants.menu_SubmitterCreation(), submitterModifyConstants);
    private final ViewImpl submitterEditView = new ViewImpl(submitterModifyConstants.menu_SubmitterEdit(), submitterModifyConstants);
    //private final HarvestersShowView harvestersShowView = new HarvestersShowViewImpl();

    public ClientFactoryImpl() {
        // Submitters Main Menu
        MenuItem createSubmitter = new MenuItem(GUIID_MENU_ITEM_SUBMITTER_CREATE, submitterModifyConstants.menu_SubmitterCreation(), new CreatePlace());
        MenuItem submittersMenu = new MenuItem(GUIID_MENU_ITEM_SUBMITTERS_SHOW, submittersShowConstants.menu_Submitters(), new SubmittersShowPlace(),
                                               createSubmitter);

        // Flows Main Menu
        MenuItem createFlow = new MenuItem(GUIID_MENU_ITEM_FLOW_CREATE, flowCreateConstants.menu_FlowCreation(), new FlowCreatePlace());
        MenuItem createFlowComponent = new MenuItem(GUIID_MENU_ITEM_FLOW_COMPONENT_CREATE, flowComponentCreateEditConstants.menu_FlowComponentCreation(), new FlowComponentCreatePlace());
        MenuItem showFlowComponents = new MenuItem(GUIID_MENU_ITEM_FLOW_COMPONENTS_SHOW, flowComponentsShowConstants.menu_FlowComponentsShow(), new FlowComponentsShowPlace());
        MenuItem createFlowBinder = new MenuItem(GUIID_MENU_ITEM_FLOWBINDER_CREATE, flowbinderCreateConstants.menu_FlowbinderCreation(), new FlowbinderCreatePlace());
        MenuItem showFlowBinders = new MenuItem(GUIID_MENU_ITEM_FLOW_BINDERS_SHOW, flowBindersShowConstants.menu_FlowBindersShow(), new FlowBindersShowPlace());
        MenuItem flowsMenu = new MenuItem(GUIID_MENU_ITEM_FLOWS_SHOW, flowsShowConstants.menu_Flows(), new FlowsShowPlace(),
                                          createFlow,
                                          createFlowComponent,
                                          showFlowComponents,
                                          createFlowBinder,
                                          showFlowBinders);

        // Sinks Main Menu
        MenuItem createSink = new MenuItem(GUIID_MENU_ITEM_SINK_CREATE, SinkCreateEditConstants.menu_SinkCreation(), new SinkCreatePlace());
        MenuItem sinksMenu = new MenuItem(GUIID_MENU_ITEM_SINKS_SHOW, sinksShowConstants.menu_Sinks(), new SinksShowPlace(),
                                          createSink);

        // Jobs Main Menu
        MenuItem jobsMenu = new MenuItem(GUIID_MENU_ITEM_JOBS_SHOW, jobsShowConstants.menu_Jobs(), new JobsShowPlace());

        // Harvesters Main Menu
        //MenuItem harvestersMenu = new MenuItem(GUIID_MENU_ITEM_HARVESTERS_SHOW, harvestersShowConstants.menu_Harvesters(), new HarvestersShowPlace());

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

    @Override
    public Activity getPresenter(Place place) {
        if (place instanceof FlowCreatePlace) {
            return new FlowCreateActivity(this);
        }
        if (place instanceof FlowComponentCreatePlace) {
            return new FlowComponentCreateActivity(this);
        }
        if (place instanceof CreatePlace) {
            return new PresenterCreateImpl(this, submitterModifyConstants);
        }
        if (place instanceof EditPlace) {
            return new PresenterEditImpl(place, this, submitterModifyConstants);
        }
        if (place instanceof FlowbinderCreatePlace) {
            return new FlowbinderCreateActivity(this);
        }
        if (place instanceof FlowComponentEditPlace) {
            return new FlowComponentEditActivity(place,this);
        }
        if (place instanceof SinkCreatePlace) {
            return new SinkCreateActivity(this);
        }
        if (place instanceof dk.dbc.dataio.gui.client.pages.sinkmodify.CreatePlace) {
            return new dk.dbc.dataio.gui.client.pages.sinkmodify.PresenterCreateImpl(this, sinkModifyConstants);
        }
        if (place instanceof SinkEditPlace) {
            return new SinkEditActivity(place, this);
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
        if (place instanceof SinksShowPlace) {
            return new SinksShowActivity(this);
        }
        if (place instanceof FlowBindersShowPlace) {
            return new FlowBindersShowActivity(this);
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

    // Menu Structure
    @Override
    public MenuItem getMenuStructure() {
        return menuStructure;
    }

    // Views
    @Override
    public FlowCreateView getFlowCreateView() {
        return flowCreateView;
    }

    @Override
    public FlowComponentCreateEditView getFlowComponentCreateEditView() {
        return flowComponentCreateEditView;
    }

    @Override
    public View getSubmitterCreateView() {
        return submitterCreateView;
    }

    @Override
    public View getSubmitterEditView() {
        return submitterEditView;
    }

    @Override
    public FlowbinderCreateView getFlowbinderCreateView() {
        return flowbinderCreateView;
    }

    @Override
    public SinkCreateEditView getSinkCreateEditView() {
        return sinkCreateEditView;
    }

    @Override
    public dk.dbc.dataio.gui.client.pages.sinkmodify.View getSinkCreateView() {
        return sinkCreateView;
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
    public SinksShowView getSinksShowView() {
        return sinksShowView;
    }

    @Override
    public FlowBindersShowView getFlowBindersShowView() {
        return flowBindersShowView;
    }

//    @Override
//    public HarvestersShowView getHarvestersShowView() {
//        return harvestersShowView;
//    }

}
