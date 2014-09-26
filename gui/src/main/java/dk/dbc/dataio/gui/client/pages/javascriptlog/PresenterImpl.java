package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.util.ClientFactory;

public class PresenterImpl extends AbstractActivity implements Presenter {

    protected ClientFactory clientFactory;
    protected View view;
    private long failedItemId;
    protected Texts texts;
    private String iAmADummyVariable;
    private final static String NBSP = new String(new char[8]).replace("\0", "\u00A0");

    /**
     * Constructor
     * @param clientFactory
     * @param texts
     */
    public PresenterImpl(Place place, ClientFactory clientFactory, Texts texts) {
        this.clientFactory = clientFactory;
        this.texts = texts;
        JavaScriptLogPlace javaScriptLogPlace = (JavaScriptLogPlace) place;
        failedItemId = javaScriptLogPlace.getFailedItemId();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view = clientFactory.getJavaScriptLogView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initializeModel();
    }

    /**
     * Initializing the model
     * The method fetches the stored Sink, as given in the Place (referenced by this.id)
     */
    public void initializeModel() {
        getJavaScriptLog(failedItemId);
    }

    // Private methods
    private void getJavaScriptLog(final long failedItemId) {
        getJavaScriptLogDummyProxy(failedItemId, new GetJavaScriptLogFilteredAsyncCallback());
    }

    // TODO - dummy method since the proxy does not yet exist
    private void getJavaScriptLogDummyProxy(long failedItemId, GetJavaScriptLogFilteredAsyncCallback getJavaScriptLogFilteredAsyncCallback) {
        this.failedItemId = failedItemId;
        iAmADummyVariable = "2014-09-24 10:10:34.565 INFO JobStoreBean.java:49 log statement number 0\n" +
                "2014-09-24 10:10:34.587 INFO JobStoreBean.java:51 an error occurred\n" +
                "2014-09-24 10:10:34.606 ERROR JobStoreBean.java:59 Testing nested exceptions\n" +
                "dk.dbc.dataio.jobstore.types.JobStoreException: Caught IllegalStateException\n" +
                "\tat dk.dbc.dataio.jobstore.ejb.JobStoreBean.setupJobStore(JobStoreBean.java:56)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat java.lang.reflect.Method.invoke(Method.java:483)\n" +
                "\tat com.sun.ejb.containers.interceptors.BeanCallbackInterceptor.intercept(InterceptorManager.java:1035)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackChainImpl.invokeNext(CallbackChainImpl.java:72)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackInvocationContext.proceed(CallbackInvocationContext.java:205)\n" +
                "\tat org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:55)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat java.lang.reflect.Method.invoke(Method.java:483)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackInterceptor.intercept(InterceptorManager.java:986)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackChainImpl.invokeNext(CallbackChainImpl.java:72)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackInvocationContext.proceed(CallbackInvocationContext.java:205)\n" +
                "\tat com.sun.ejb.containers.interceptors.SystemInterceptorProxy.doCall(SystemInterceptorProxy.java:163)\n" +
                "\tat com.sun.ejb.containers.interceptors.SystemInterceptorProxy.init(SystemInterceptorProxy.java:125)\n" +
                "\tat sun.reflect.GeneratedMethodAccessor955.invoke(Unknown Source)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat java.lang.reflect.Method.invoke(Method.java:483)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackInterceptor.intercept(InterceptorManager.java:986)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackChainImpl.invokeNext(CallbackChainImpl.java:72)\n" +
                "\tat com.sun.ejb.containers.interceptors.InterceptorManager.intercept(InterceptorManager.java:412)\n" +
                "\tat com.sun.ejb.containers.interceptors.InterceptorManager.intercept(InterceptorManager.java:375)\n" +
                "\tat com.sun.ejb.containers.BaseContainer.intercept(BaseContainer.java:1949)\n" +
                "\tat com.sun.ejb.containers.AbstractSingletonContainer.createSingletonEJB(AbstractSingletonContainer.java:475)\n" +
                "\tat com.sun.ejb.containers.AbstractSingletonContainer.access$000(AbstractSingletonContainer.java:81)\n" +
                "\tat com.sun.ejb.containers.AbstractSingletonContainer$SingletonContextFactory.create(AbstractSingletonContainer.java:654)\n" +
                "\tat com.sun.ejb.containers.AbstractSingletonContainer.instantiateSingletonInstance(AbstractSingletonContainer.java:396)\n" +
                "\tat org.glassfish.ejb.startup.SingletonLifeCycleManager.initializeSingleton(SingletonLifeCycleManager.java:219)\n" +
                "\tat org.glassfish.ejb.startup.SingletonLifeCycleManager.initializeSingleton(SingletonLifeCycleManager.java:180)\n" +
                "\tat org.glassfish.ejb.startup.SingletonLifeCycleManager.doStartup(SingletonLifeCycleManager.java:158)\n" +
                "\tat org.glassfish.ejb.startup.EjbApplication.start(EjbApplication.java:166)\n" +
                "\tat org.glassfish.internal.data.EngineRef.start(EngineRef.java:122)\n" +
                "\tat org.glassfish.internal.data.ModuleInfo.start(ModuleInfo.java:291)\n" +
                "\tat org.glassfish.internal.data.ApplicationInfo.start(ApplicationInfo.java:352)\n" +
                "\tat com.sun.enterprise.v3.server.ApplicationLifecycle.deploy(ApplicationLifecycle.java:497)\n" +
                "\tat com.sun.enterprise.v3.server.ApplicationLifecycle.deploy(ApplicationLifecycle.java:219)\n" +
                "\tat org.glassfish.deployment.admin.DeployCommand.execute(DeployCommand.java:491)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$2$1.run(CommandRunnerImpl.java:527)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$2$1.run(CommandRunnerImpl.java:523)\n" +
                "\tat java.security.AccessController.doPrivileged(Native Method)\n" +
                "\tat javax.security.auth.Subject.doAs(Subject.java:360)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$2.execute(CommandRunnerImpl.java:522)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl.doCommand(CommandRunnerImpl.java:546)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl.doCommand(CommandRunnerImpl.java:1423)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl.access$1500(CommandRunnerImpl.java:108)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$ExecutionContext.execute(CommandRunnerImpl.java:1762)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$ExecutionContext.execute(CommandRunnerImpl.java:1674)\n" +
                "\tat org.glassfish.admin.rest.utils.ResourceUtil.runCommand(ResourceUtil.java:235)\n" +
                "\tat org.glassfish.admin.rest.utils.ResourceUtil.runCommand(ResourceUtil.java:257)\n" +
                "\tat org.glassfish.admin.rest.resources.TemplateListOfResource.createResource(TemplateListOfResource.java:134)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat java.lang.reflect.Method.invoke(Method.java:483)\n" +
                "\tat org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory$1.invoke(ResourceMethodInvocationHandlerFactory.java:81)\n" +
                "\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.invoke(AbstractJavaResourceMethodDispatcher.java:125)\n" +
                "\tat org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$ResponseOutInvoker.doDispatch(JavaResourceMethodDispatcherProvider.java:152)\n" +
                "\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.dispatch(AbstractJavaResourceMethodDispatcher.java:91)\n" +
                "\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:346)\n" +
                "\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:341)\n" +
                "\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:101)\n" +
                "\tat org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:224)\n" +
                "\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)\n" +
                "\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)\n" +
                "\tat org.glassfish.jersey.internal.Errors.process(Errors.java:315)\n" +
                "\tat org.glassfish.jersey.internal.Errors.process(Errors.java:297)\n" +
                "\tat org.glassfish.jersey.internal.Errors.process(Errors.java:267)\n" +
                "\tat org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:317)\n" +
                "\tat org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:198)\n" +
                "\tat org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:946)\n" +
                "\tat org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer.service(GrizzlyHttpContainer.java:331)\n" +
                "\tat org.glassfish.admin.rest.adapter.RestAdapter$2.service(RestAdapter.java:318)\n" +
                "\tat org.glassfish.admin.rest.adapter.RestAdapter.service(RestAdapter.java:181)\n" +
                "\tat com.sun.enterprise.v3.services.impl.ContainerMapper.service(ContainerMapper.java:246)\n" +
                "\tat org.glassfish.grizzly.http.server.HttpHandler.runService(HttpHandler.java:191)\n" +
                "\tat org.glassfish.grizzly.http.server.HttpHandler.doHandle(HttpHandler.java:168)\n" +
                "\tat org.glassfish.grizzly.http.server.HttpServerFilter.handleRead(HttpServerFilter.java:189)\n" +
                "\tat org.glassfish.grizzly.filterchain.ExecutorResolver$9.execute(ExecutorResolver.java:119)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeFilter(DefaultFilterChain.java:288)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeChainPart(DefaultFilterChain.java:206)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.execute(DefaultFilterChain.java:136)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.process(DefaultFilterChain.java:114)\n" +
                "\tat org.glassfish.grizzly.ProcessorExecutor.execute(ProcessorExecutor.java:77)\n" +
                "\tat org.glassfish.grizzly.portunif.PUFilter.handleRead(PUFilter.java:231)\n" +
                "\tat org.glassfish.grizzly.filterchain.ExecutorResolver$9.execute(ExecutorResolver.java:119)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeFilter(DefaultFilterChain.java:288)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeChainPart(DefaultFilterChain.java:206)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.execute(DefaultFilterChain.java:136)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.process(DefaultFilterChain.java:114)\n" +
                "\tat org.glassfish.grizzly.ProcessorExecutor.execute(ProcessorExecutor.java:77)\n" +
                "\tat org.glassfish.grizzly.portunif.PUFilter.handleRead(PUFilter.java:231)\n" +
                "\tat org.glassfish.grizzly.filterchain.ExecutorResolver$9.execute(ExecutorResolver.java:119)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeFilter(DefaultFilterChain.java:288)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeChainPart(DefaultFilterChain.java:206)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.execute(DefaultFilterChain.java:136)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.process(DefaultFilterChain.java:114)\n" +
                "\tat org.glassfish.grizzly.ProcessorExecutor.execute(ProcessorExecutor.java:77)\n" +
                "\tat org.glassfish.grizzly.nio.transport.TCPNIOTransport.fireIOEvent(TCPNIOTransport.java:838)\n" +
                "\tat org.glassfish.grizzly.strategies.AbstractIOStrategy.fireIOEvent(AbstractIOStrategy.java:113)\n" +
                "\tat org.glassfish.grizzly.strategies.WorkerThreadIOStrategy.run0(WorkerThreadIOStrategy.java:115)\n" +
                "\tat org.glassfish.grizzly.strategies.WorkerThreadIOStrategy.access$100(WorkerThreadIOStrategy.java:55)\n" +
                "\tat org.glassfish.grizzly.strategies.WorkerThreadIOStrategy$WorkerThreadRunnable.run(WorkerThreadIOStrategy.java:135)\n" +
                "\tat org.glassfish.grizzly.threadpool.AbstractThreadPool$Worker.doWork(AbstractThreadPool.java:564)\n" +
                "\tat org.glassfish.grizzly.threadpool.AbstractThreadPool$Worker.run(AbstractThreadPool.java:544)\n" +
                "\tat java.lang.Thread.run(Thread.java:745)\n" +
                "Caused by: java.lang.IllegalStateException: Trying to figure out how exceptions are handled\n" +
                "\tat dk.dbc.dataio.jobstore.ejb.JobStoreBean.setupJobStore(JobStoreBean.java:54)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat java.lang.reflect.Method.invoke(Method.java:483)\n" +
                "\tat com.sun.ejb.containers.interceptors.BeanCallbackInterceptor.intercept(InterceptorManager.java:1035)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackChainImpl.invokeNext(CallbackChainImpl.java:72)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackInvocationContext.proceed(CallbackInvocationContext.java:205)\n" +
                "\tat org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:55)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat java.lang.reflect.Method.invoke(Method.java:483)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackInterceptor.intercept(InterceptorManager.java:986)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackChainImpl.invokeNext(CallbackChainImpl.java:72)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackInvocationContext.proceed(CallbackInvocationContext.java:205)\n" +
                "\tat com.sun.ejb.containers.interceptors.SystemInterceptorProxy.doCall(SystemInterceptorProxy.java:163)\n" +
                "\tat com.sun.ejb.containers.interceptors.SystemInterceptorProxy.init(SystemInterceptorProxy.java:125)\n" +
                "\tat sun.reflect.GeneratedMethodAccessor955.invoke(Unknown Source)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat java.lang.reflect.Method.invoke(Method.java:483)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackInterceptor.intercept(InterceptorManager.java:986)\n" +
                "\tat com.sun.ejb.containers.interceptors.CallbackChainImpl.invokeNext(CallbackChainImpl.java:72)\n" +
                "\tat com.sun.ejb.containers.interceptors.InterceptorManager.intercept(InterceptorManager.java:412)\n" +
                "\tat com.sun.ejb.containers.interceptors.InterceptorManager.intercept(InterceptorManager.java:375)\n" +
                "\tat com.sun.ejb.containers.BaseContainer.intercept(BaseContainer.java:1949)\n" +
                "\tat com.sun.ejb.containers.AbstractSingletonContainer.createSingletonEJB(AbstractSingletonContainer.java:475)\n" +
                "\tat com.sun.ejb.containers.AbstractSingletonContainer.access$000(AbstractSingletonContainer.java:81)\n" +
                "\tat com.sun.ejb.containers.AbstractSingletonContainer$SingletonContextFactory.create(AbstractSingletonContainer.java:654)\n" +
                "\tat com.sun.ejb.containers.AbstractSingletonContainer.instantiateSingletonInstance(AbstractSingletonContainer.java:396)\n" +
                "\tat org.glassfish.ejb.startup.SingletonLifeCycleManager.initializeSingleton(SingletonLifeCycleManager.java:219)\n" +
                "\tat org.glassfish.ejb.startup.SingletonLifeCycleManager.initializeSingleton(SingletonLifeCycleManager.java:180)\n" +
                "\tat org.glassfish.ejb.startup.SingletonLifeCycleManager.doStartup(SingletonLifeCycleManager.java:158)\n" +
                "\tat org.glassfish.ejb.startup.EjbApplication.start(EjbApplication.java:166)\n" +
                "\tat org.glassfish.internal.data.EngineRef.start(EngineRef.java:122)\n" +
                "\tat org.glassfish.internal.data.ModuleInfo.start(ModuleInfo.java:291)\n" +
                "\tat org.glassfish.internal.data.ApplicationInfo.start(ApplicationInfo.java:352)\n" +
                "\tat com.sun.enterprise.v3.server.ApplicationLifecycle.deploy(ApplicationLifecycle.java:497)\n" +
                "\tat com.sun.enterprise.v3.server.ApplicationLifecycle.deploy(ApplicationLifecycle.java:219)\n" +
                "\tat org.glassfish.deployment.admin.DeployCommand.execute(DeployCommand.java:491)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$2$1.run(CommandRunnerImpl.java:527)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$2$1.run(CommandRunnerImpl.java:523)\n" +
                "\tat java.security.AccessController.doPrivileged(Native Method)\n" +
                "\tat javax.security.auth.Subject.doAs(Subject.java:360)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$2.execute(CommandRunnerImpl.java:522)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl.doCommand(CommandRunnerImpl.java:546)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl.doCommand(CommandRunnerImpl.java:1423)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl.access$1500(CommandRunnerImpl.java:108)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$ExecutionContext.execute(CommandRunnerImpl.java:1762)\n" +
                "\tat com.sun.enterprise.v3.admin.CommandRunnerImpl$ExecutionContext.execute(CommandRunnerImpl.java:1674)\n" +
                "\tat org.glassfish.admin.rest.utils.ResourceUtil.runCommand(ResourceUtil.java:235)\n" +
                "\tat org.glassfish.admin.rest.utils.ResourceUtil.runCommand(ResourceUtil.java:257)\n" +
                "\tat org.glassfish.admin.rest.resources.TemplateListOfResource.createResource(TemplateListOfResource.java:134)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
                "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
                "\tat java.lang.reflect.Method.invoke(Method.java:483)\n" +
                "\tat org.glassfish.jersey.server.model.internal.ResourceMethodInvocationHandlerFactory$1.invoke(ResourceMethodInvocationHandlerFactory.java:81)\n" +
                "\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.invoke(AbstractJavaResourceMethodDispatcher.java:125)\n" +
                "\tat org.glassfish.jersey.server.model.internal.JavaResourceMethodDispatcherProvider$ResponseOutInvoker.doDispatch(JavaResourceMethodDispatcherProvider.java:152)\n" +
                "\tat org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher.dispatch(AbstractJavaResourceMethodDispatcher.java:91)\n" +
                "\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:346)\n" +
                "\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:341)\n" +
                "\tat org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:101)\n" +
                "\tat org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:224)\n" +
                "\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)\n" +
                "\tat org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)\n" +
                "\tat org.glassfish.jersey.internal.Errors.process(Errors.java:315)\n" +
                "\tat org.glassfish.jersey.internal.Errors.process(Errors.java:297)\n" +
                "\tat org.glassfish.jersey.internal.Errors.process(Errors.java:267)\n" +
                "\tat org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:317)\n" +
                "\tat org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:198)\n" +
                "\tat org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:946)\n" +
                "\tat org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer.service(GrizzlyHttpContainer.java:331)\n" +
                "\tat org.glassfish.admin.rest.adapter.RestAdapter$2.service(RestAdapter.java:318)\n" +
                "\tat org.glassfish.admin.rest.adapter.RestAdapter.service(RestAdapter.java:181)\n" +
                "\tat com.sun.enterprise.v3.services.impl.ContainerMapper.service(ContainerMapper.java:246)\n" +
                "\tat org.glassfish.grizzly.http.server.HttpHandler.runService(HttpHandler.java:191)\n" +
                "\tat org.glassfish.grizzly.http.server.HttpHandler.doHandle(HttpHandler.java:168)\n" +
                "\tat org.glassfish.grizzly.http.server.HttpServerFilter.handleRead(HttpServerFilter.java:189)\n" +
                "\tat org.glassfish.grizzly.filterchain.ExecutorResolver$9.execute(ExecutorResolver.java:119)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeFilter(DefaultFilterChain.java:288)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeChainPart(DefaultFilterChain.java:206)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.execute(DefaultFilterChain.java:136)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.process(DefaultFilterChain.java:114)\n" +
                "\tat org.glassfish.grizzly.ProcessorExecutor.execute(ProcessorExecutor.java:77)\n" +
                "\tat org.glassfish.grizzly.portunif.PUFilter.handleRead(PUFilter.java:231)\n" +
                "\tat org.glassfish.grizzly.filterchain.ExecutorResolver$9.execute(ExecutorResolver.java:119)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeFilter(DefaultFilterChain.java:288)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeChainPart(DefaultFilterChain.java:206)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.execute(DefaultFilterChain.java:136)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.process(DefaultFilterChain.java:114)\n" +
                "\tat org.glassfish.grizzly.ProcessorExecutor.execute(ProcessorExecutor.java:77)\n" +
                "\tat org.glassfish.grizzly.portunif.PUFilter.handleRead(PUFilter.java:231)\n" +
                "\tat org.glassfish.grizzly.filterchain.ExecutorResolver$9.execute(ExecutorResolver.java:119)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeFilter(DefaultFilterChain.java:288)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.executeChainPart(DefaultFilterChain.java:206)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.execute(DefaultFilterChain.java:136)\n" +
                "\tat org.glassfish.grizzly.filterchain.DefaultFilterChain.process(DefaultFilterChain.java:114)\n" +
                "\tat org.glassfish.grizzly.ProcessorExecutor.execute(ProcessorExecutor.java:77)\n" +
                "\tat org.glassfish.grizzly.nio.transport.TCPNIOTransport.fireIOEvent(TCPNIOTransport.java:838)\n" +
                "\tat org.glassfish.grizzly.strategies.AbstractIOStrategy.fireIOEvent(AbstractIOStrategy.java:113)\n" +
                "\tat org.glassfish.grizzly.strategies.WorkerThreadIOStrategy.run0(WorkerThreadIOStrategy.java:115)\n" +
                "\tat org.glassfish.grizzly.strategies.WorkerThreadIOStrategy.access$100(WorkerThreadIOStrategy.java:55)\n" +
                "\tat org.glassfish.grizzly.strategies.WorkerThreadIOStrategy$WorkerThreadRunnable.run(WorkerThreadIOStrategy.java:135)\n" +
                "\tat org.glassfish.grizzly.threadpool.AbstractThreadPool$Worker.doWork(AbstractThreadPool.java:564)\n" +
                "\tat org.glassfish.grizzly.threadpool.AbstractThreadPool$Worker.run(AbstractThreadPool.java:544)\n" +
                "\tat java.lang.Thread.run(Thread.java:745)\n" +
                "\n" +
                "2014-09-24 10:10:34.614 INFO JobStoreBean.java:61 done\n";
        getJavaScriptLogFilteredAsyncCallback.onSuccess(iAmADummyVariable);
    }
    /**
     * Call back class to be instantiated in the call to get????? in ????? proxy
     */
    class GetJavaScriptLogFilteredAsyncCallback extends FilteredAsyncCallback<String> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_CannotFetchJavaScriptLog());
        }

        @Override
        public void onSuccess(String log) {
            view.htmlLabel.setHTML(formatLog(log));
        }
    }

    private String formatLog(String log) {
        log = log.replace("\t", NBSP);
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder().appendEscapedLines(log);
        return safeHtmlBuilder.toSafeHtml().asString();
    }

}
