package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.javascript.JavaScriptProject;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectException;
import dk.dbc.dataio.commons.javascript.JavaScriptSubversionProject;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentView;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowBinderUsage;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher.fetchRequiredJavaScriptResult;
import dk.dbc.dataio.gui.client.querylanguage.GwtIntegerClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.server.modelmappers.FlowBinderModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.FlowComponentModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.FlowModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.SinkModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.SubmitterModelMapper;
import dk.dbc.dataio.gui.server.query.GwtQueryBuilder;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import dk.dbc.dataio.querylanguage.Ordering;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FlowStoreProxyImpl implements FlowStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyImpl.class);
    final Client client;
    private Client jobstoreClient;
    private final String baseUrl;
    private String jobstoreBaseUrl;
    private final String subversionScmEndpoint;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private JobStoreServiceConnector jobStoreServiceConnector;
    private JavaScriptSubversionProject javaScriptSubversionProject;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    FlowStoreProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        jobstoreClient = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("FLOWSTORE_URL");
        jobstoreBaseUrl = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("JOBSTORE_URL");
        jobStoreServiceConnector = new JobStoreServiceConnector(jobstoreClient, jobstoreBaseUrl);
        log.info("FlowStoreProxy: Using Base URL {}", baseUrl);
        subversionScmEndpoint = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("SUBVERSION_URL");
        flowStoreServiceConnector = new FlowStoreServiceConnector(client, baseUrl);
        javaScriptSubversionProject = new JavaScriptSubversionProject(subversionScmEndpoint);
    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    FlowStoreProxyImpl(FlowStoreServiceConnector flowStoreServiceConnector) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        subversionScmEndpoint = null;
        client = flowStoreServiceConnector.getClient();
        baseUrl = flowStoreServiceConnector.getBaseUrl();
        log.info("FlowStoreProxy: Using Base URL {}", baseUrl);
    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    FlowStoreProxyImpl(FlowStoreServiceConnector flowStoreServiceConnector, JavaScriptSubversionProject javaScriptSubversionProject) {
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.javaScriptSubversionProject = javaScriptSubversionProject;
        this.subversionScmEndpoint = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("SUBVERSION_URL");
        client = flowStoreServiceConnector.getClient();
        jobstoreClient = HttpClient.newClient();
        baseUrl = flowStoreServiceConnector.getBaseUrl();
        log.info("FlowStoreProxy: Using Base URL {}", baseUrl);
    }

    /*
     * Flows
     */
    @Override
    public FlowModel createFlow(FlowModel model) throws NullPointerException, ProxyException {
        final String callerMethodName = "createFlow";
        log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", model.getFlowName());
        Flow flow = null;
        List<FlowComponent> flowComponents;
        try {
            flowComponents = getFlowComponentsLatestVersion(model.getFlowComponents());
            flow = flowStoreServiceConnector.createFlow(FlowModelMapper.toFlowContent(model, flowComponents));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowModelMapper.toModel(flow);
    }

    public FlowModel updateFlow(FlowModel model) throws NullPointerException, ProxyException {
        final String callerMethodName = "updateFlow";
        log.trace("FlowStoreProxy: " + callerMethodName + "({});", model.getId(), model.getVersion());
        Flow flow = null;
        List<FlowComponent> flowComponents;
        try {
            // Retrieve the currently saved version of the flow
            Flow flowOpenedForUpdate = flowStoreServiceConnector.getFlow(model.getId());

            // If the flow has been updated by another user: Throw proxyException - conflict error
            if (model.getVersion() != flowOpenedForUpdate.getVersion()) {
                log.error("FlowStoreProxy: updateFlow - Concurrent Update Error");
                throw new ProxyException(ProxyError.CONFLICT_ERROR, "Concurrent Update Error");
            } else {
                flowComponents = getFlowComponents(flowOpenedForUpdate, model.getFlowComponents());
                flow = flowStoreServiceConnector.updateFlow(FlowModelMapper.toFlowContent(model, flowComponents), model.getId(), model.getVersion());
            }
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowModelMapper.toModel(flow);
    }

    @Override
    public void deleteFlow(long flowId, long version) throws NullPointerException, ProxyException {
        final String callerMethodName = "deleteFlow";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", flowId, version);
        try {
            flowStoreServiceConnector.deleteFlow(flowId, version);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
    }

    @Override
    public List<FlowModel> findAllFlows() throws ProxyException {
        final String callerMethodName = "findAllFlows";
        List<FlowView> flowViews = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            flowViews = flowStoreServiceConnector.findAllFlows();
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowModelMapper.fromListOfFlowViews(flowViews);
    }

    @Override
    public FlowModel getFlow(Long id) throws ProxyException {
        final String callerMethodName = "getFlow";
        log.trace("Trace - FlowStoreProxy: " + callerMethodName + "({});", id);
        Flow flow = null;
        try {
            flow = flowStoreServiceConnector.getFlow(id);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowModelMapper.toModel(flow);
    }

    /*
     * Flow Components
     */

    @Override
    public FlowComponentModel createFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException {

        final String callerMethodName = "createFlowComponent";
        log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", model.getName());
        FlowComponent flowComponent = null;
        try {
            FlowComponent createdFlowComponent = flowStoreServiceConnector.createFlowComponent(
                    FlowComponentModelMapper.toFlowComponentContent(model, fetchRequiredJavaScripts(model)));

            flowComponent = flowStoreServiceConnector.updateNext(
                    FlowComponentModelMapper.toNext(model, fetchRequiredJavaScriptsForNext(model)),
                    createdFlowComponent.getId(),
                    createdFlowComponent.getVersion());

        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowComponentModelMapper.toModel(flowComponent);
    }

    @Override
    public FlowComponentModel updateFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException {
        final String callerMethodName = "updateFlowComponent";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", model.getId(), model.getVersion());
        FlowComponent flowComponent = null;
        try {
            final FlowComponent updatedContentFlowComponent = flowStoreServiceConnector.updateFlowComponent(
                    FlowComponentModelMapper.toFlowComponentContent(model, fetchRequiredJavaScripts(model)),
                    model.getId(),
                    model.getVersion());

            flowComponent = flowStoreServiceConnector.updateNext(
                    FlowComponentModelMapper.toNext(model, fetchRequiredJavaScriptsForNext(model)),
                    updatedContentFlowComponent.getId(),
                    updatedContentFlowComponent.getVersion());

        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowComponentModelMapper.toModel(flowComponent);
    }

    @Override
    public FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException {
        final String callerMethodName = "refreshFlowComponents";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", id, version);
        Flow flow = null;
        try {
            flow = flowStoreServiceConnector.refreshFlowComponents(id, version);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowModelMapper.toModel(flow);
    }

    @Override
    public void deleteFlowComponent(long flowComponentId, long version) throws NullPointerException, ProxyException {
        final String callerMethodName = "deleteFlowComponent";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", flowComponentId, version);
        try {
            flowStoreServiceConnector.deleteFlowComponent(flowComponentId, version);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
    }

    @Override
    public List<FlowComponentModel> findAllFlowComponents() throws ProxyException {
        final String callerMethodName = "findAllFlowComponents";
        List<FlowComponentView> result = null;
        log.trace("FlowStoreProxy: {}()", callerMethodName);
        try {
            result = flowStoreServiceConnector.findAllFlowComponents();
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowComponentModelMapper.fromListOfFlowComponentViews(result);
    }

    @Override
    public FlowComponentModel getFlowComponent(Long id) throws ProxyException {
        final String callerMethodName = "getFlowComponent";
        FlowComponent result = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "({});", id);
        try {
            result = flowStoreServiceConnector.getFlowComponent(id);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowComponentModelMapper.toModel(result);
    }

    /*
     * Flows Binders
     */

    @Override
    public FlowBinderModel createFlowBinder(FlowBinderModel model) throws NullPointerException, ProxyException {
        final String callerMethodName = "createFlowBinder";
        FlowBinderModel flowBinderModel = null;
        FlowBinder flowBinder;
        log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", model.getName());
        try {
            flowBinder = flowStoreServiceConnector.createFlowBinder(FlowBinderModelMapper.toFlowBinderContent(model));
            flowBinderModel = FlowBinderModelMapper.toModel(
                    flowBinder,
                    getFlowModelLatestVersion(model.getFlowModel()),
                    getSubmitterModelsLatestVersion(model.getSubmitterModels()),
                    getSinkModelLatestVersion(model.getSinkModel()));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return flowBinderModel;
    }

    @Override
    public FlowBinderModel updateFlowBinder(FlowBinderModel model) throws NullPointerException, ProxyException {
        final String callerMethodName = "updateFlowBinder";
        FlowBinder flowBinder = null;
        List<SubmitterModel> submitterModels = null;
        FlowModel flowModel = null;
        SinkModel sinkModel = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", model.getId(), model.getVersion());
        try {
            flowBinder = flowStoreServiceConnector.updateFlowBinder(FlowBinderModelMapper.toFlowBinderContent(model), model.getId(), model.getVersion());
            submitterModels = new ArrayList<>(flowBinder.getContent().getSubmitterIds().size());
            for (Long submitterId : flowBinder.getContent().getSubmitterIds()) {
                submitterModels.add(SubmitterModelMapper.toModel(flowStoreServiceConnector.getSubmitter(submitterId)));
            }
            flowModel = FlowModelMapper.toModel(flowStoreServiceConnector.getFlow(flowBinder.getContent().getFlowId()));
            sinkModel = SinkModelMapper.toModel(flowStoreServiceConnector.getSink(flowBinder.getContent().getSinkId()));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowBinderModelMapper.toModel(flowBinder, flowModel, submitterModels, sinkModel);
    }

    @Override
    public void deleteFlowBinder(long flowBinderId, long version) throws NullPointerException, ProxyException {
        final String callerMethodName = "deleteFlowBinder";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", flowBinderId, version);
        try {
            flowStoreServiceConnector.deleteFlowBinder(flowBinderId, version);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
    }

    private Cache<Long, SubmitterModel> cachedSubmitterMap = CacheManager.createUnboundCache(SubmitterModel::getId, rethrowSupplier(this::findAllSubmitters));
    private Cache<Long, SinkModel> cachedSinkMap = CacheManager.createUnboundCache(SinkModel::getId, rethrowSupplier(this::findAllSinks));
    private Cache<Long, FlowModel> cachedFlowMap = CacheManager.createUnboundCache(FlowModel::getId, rethrowSupplier(this::findAllFlows));
    private Cache<Long, FlowBinderUsage> cachedFlowBinderUsage = CacheManager.createUnboundCache(FlowBinderUsage::getFlowBinderId, rethrowSupplier(this::getFlowBindersUsage));

    @Override
    public List<FlowBinderModel> queryFlowBinders(List<GwtQueryClause> clauses) throws ProxyException {
        final String callerMethodName = "queryFlowBinders";
        final List<FlowBinderModel> flowBinderModels = new ArrayList<>();
        cachedSubmitterMap.clear();
        cachedSinkMap.clear();
        cachedFlowMap.clear();
        try {
            for (GwtQueryClause gwtQueryClause : clauses) {
                if (gwtQueryClause instanceof GwtIntegerClause
                        && ((GwtIntegerClause) gwtQueryClause).getIdentifier()
                        .equals("flow_binders:content.submitterIds")) {
                    // Submitter filter uses submitter number as input,
                    // but submitter ID is needed to build a working query.
                    replaceSubmitterNumberWithIdInClause((GwtIntegerClause) gwtQueryClause);
                }
            }

            final List<FlowBinder> flowBinders = flowStoreServiceConnector.queryFlowBinders(
                    new GwtQueryBuilder()
                            .addAll(clauses)
                            .sortBy(new Ordering()
                                    .withIdentifier("flow_binders:content.name")
                                    .withOrder(Ordering.Order.ASC)
                                    .withSortCase(Ordering.SortCase.LOWER))
                            .build());
            for (FlowBinder flowBinder : flowBinders) {
                final List<SubmitterModel> submitterModels =
                        new ArrayList<>(flowBinder.getContent().getSubmitterIds().size());
                for (long submitterId : flowBinder.getContent().getSubmitterIds()) {
                    submitterModels.add(cachedSubmitterMap.get(submitterId));
                }
                flowBinderModels.add(
                        FlowBinderModelMapper.toModel(
                                flowBinder,
                                cachedFlowMap.get(flowBinder.getContent().getFlowId()),
                                submitterModels,
                                cachedSinkMap.get(flowBinder.getContent().getSinkId())
                        )
                );
            }
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return flowBinderModels;
    }

    private void replaceSubmitterNumberWithIdInClause(GwtIntegerClause gwtIntegerClause)
            throws FlowStoreServiceConnectorException {
        final Submitter submitter = flowStoreServiceConnector
                .getSubmitterBySubmitterNumber(gwtIntegerClause.getValue());
        if (submitter != null) {
            gwtIntegerClause.withValue(Math.toIntExact(submitter.getId()));
        }
    }

    @Override
    public List<FlowBinderModel> findAllFlowBinders() throws ProxyException {
        final String callerMethodName = "findAllFlowBinders";
        List<FlowBinder> flowBinders;
        final List<FlowBinderModel> flowBinderModels = new ArrayList<>();
        List<SubmitterModel> submitterModels;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        cachedSubmitterMap.clear();
        cachedSinkMap.clear();
        cachedFlowMap.clear();
        try {
            flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            for (FlowBinder flowBinder : flowBinders) {
                submitterModels = new ArrayList<>(flowBinder.getContent().getSubmitterIds().size());
                for (long submitterId : flowBinder.getContent().getSubmitterIds()) {
                    submitterModels.add(cachedSubmitterMap.get(submitterId));
                }
                flowBinderModels.add(
                        FlowBinderModelMapper.toModel(
                                flowBinder,
                                cachedFlowMap.get(flowBinder.getContent().getFlowId()),
                                submitterModels,
                                cachedSinkMap.get(flowBinder.getContent().getSinkId())
                        )
                );
            }
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return flowBinderModels;
    }

    @Override
    public List<FlowBinderUsage> getFlowBindersUsage() throws ProxyException {
        List<FlowBinderUsage> usages = new ArrayList<>();
        cachedFlowBinderUsage.clear();
        try {
            List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            for (FlowBinder flowBinder : flowBinders) {
                String format = String.format("{\"format\": \"%s\"}", flowBinder.getContent().getFormat());
                String charset = String.format("{\"charset\": \"%s\"}", flowBinder.getContent().getCharset());
                String packaging = String.format("{\"packaging\": \"%s\"}", flowBinder.getContent().getPackaging());
                String destination = String.format("{\"destination\": \"%s\"}", flowBinder.getContent().getDestination());
                long sinkId = flowBinder.getContent().getSinkId();
                List<JobInfoSnapshot> jobInfoSnapshots = jobStoreServiceConnector.listJobs(
                        new JobListCriteria().where(
                                new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, format)
                        ).and(
                                new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, charset)
                        ).and(
                                new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, packaging)
                        ).and(
                                new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, destination)
                        ).and(
                                new ListFilter<>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, sinkId)
                        ).orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC)).limit(1));
                FlowBinderUsage flowBinderUsage = new FlowBinderUsage()
                        .withName(flowBinder.getContent().getName())
                        .withFlowBinderId(flowBinder.getId());
                if (!jobInfoSnapshots.isEmpty()) {
                    flowBinderUsage.withLastUsed(dateFormat.format(jobInfoSnapshots.get(0).getTimeOfCreation()));
                } else {
                    flowBinderUsage.withLastUsed("-");
                }
                usages.add(flowBinderUsage);
                cachedFlowBinderUsage.put(flowBinderUsage.getFlowBinderId(), flowBinderUsage);
            }
            return usages;
        } catch (FlowStoreServiceConnectorException | JobStoreServiceConnectorException e) {
            log.error("getFlowBindersUsage:", e);
            handleExceptions(e, "getFlowBindersUsage");
        }
        return List.of();
    }

    @Override
    public List<FlowBinderUsage> getFlowBindersUsageCached() throws ProxyException {
        try {
            List<FlowBinder> flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            return flowBinders.stream().map(flowBinder -> cachedFlowBinderUsage.get(flowBinder.getId())).collect(Collectors.toList());

        } catch (FlowStoreServiceConnectorException e) {
            log.error("getFlowBindersUsageCached:", e);
            handleExceptions(e, "getFlowBindersUsageCached");
        }
        return List.of();
    }


    @Override
    public FlowBinderModel getFlowBinder(long id) throws ProxyException {
        final String callerMethodName = "getFlowBinder";
        FlowBinder flowBinder = null;
        FlowModel flowModel = null;
        final List<SubmitterModel> submitterModels = new ArrayList<>();
        SinkModel sinkModel = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "({});", id);
        try {
            flowBinder = flowStoreServiceConnector.getFlowBinder(id);
            flowModel = FlowModelMapper.toModel(flowStoreServiceConnector.getFlow(flowBinder.getContent().getFlowId()));
            for (long submitterId : flowBinder.getContent().getSubmitterIds()) {
                submitterModels.add(SubmitterModelMapper.toModel(flowStoreServiceConnector.getSubmitter(submitterId)));
            }
            sinkModel = SinkModelMapper.toModel(flowStoreServiceConnector.getSink(flowBinder.getContent().getSinkId()));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return FlowBinderModelMapper.toModel(flowBinder, flowModel, submitterModels, sinkModel);
    }

    /*
     * Submitters
     */

    @Override
    public SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException {
        Submitter submitter = null;
        final String callerMethodName = "createSubmitter";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", model.getId(), model.getVersion());
        try {
            submitter = flowStoreServiceConnector.createSubmitter(SubmitterModelMapper.toSubmitterContent(model));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return SubmitterModelMapper.toModel(submitter);
    }

    @Override
    public SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException {
        Submitter submitter = null;
        final String callerMethodName = "updateSubmitter";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", model.getId(), model.getVersion());
        try {
            submitter = flowStoreServiceConnector.updateSubmitter(SubmitterModelMapper.toSubmitterContent(model), model.getId(), model.getVersion());
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return SubmitterModelMapper.toModel(submitter);
    }

    @Override
    public void deleteSubmitter(long submitterId, long version) throws NullPointerException, ProxyException {
        final String callerMethodName = "deleteSubmitter";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", submitterId, version);
        try {
            flowStoreServiceConnector.deleteSubmitter(submitterId, version);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
    }

    @Override
    public List<SubmitterModel> querySubmitters(List<GwtQueryClause> clauses) throws ProxyException {
        final String callerMethodName = "querySubmitters";
        final List<SubmitterModel> submitterModels = new ArrayList<>();
        try {
            final List<Submitter> submitters = flowStoreServiceConnector.querySubmitters(
                    new GwtQueryBuilder()
                            .addAll(clauses)
                            .sortBy(new Ordering()
                                    .withIdentifier("submitters:content.number")
                                    .withOrder(Ordering.Order.ASC))
                            .build());
            for (Submitter submitter : submitters) {
                submitterModels.add(SubmitterModelMapper.toModel(submitter));
            }
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return submitterModels;
    }

    @Override
    public List<SubmitterModel> findAllSubmitters() throws ProxyException {
        List<Submitter> submitters = null;
        final String callerMethodName = "findAllSubmitters";
        log.trace("FlowStoreProxy: " + callerMethodName);
        try {
            submitters = flowStoreServiceConnector.findAllSubmitters();
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return SubmitterModelMapper.toListOfSubmitterModels(submitters);
    }

    @Override
    public SubmitterModel getSubmitter(Long submitterId) throws ProxyException {
        final String callerMethodName = "getSubmitter";
        Submitter submitter = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "({});", submitterId);
        try {
            submitter = flowStoreServiceConnector.getSubmitter(submitterId);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return SubmitterModelMapper.toModel(submitter);
    }

    @Override
    public List<FlowBinderIdent> getFlowBindersForSubmitter(long submitterId) throws ProxyException {
        final String callerMethodName = "getFlowBindersForSubmitter";
        List<FlowBinderIdent> flowBinderIdents = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "({});", submitterId);
        try {
            flowBinderIdents = flowStoreServiceConnector.getFlowBindersForSubmitter(submitterId);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return flowBinderIdents;
    }


    /*
     * Sinks
     */

    @Override
    public SinkModel createSink(SinkModel model) throws NullPointerException, ProxyException {
        final String callerMethodName = "createSink";
        Sink sink = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", model.getSinkName());
        try {
            sink = flowStoreServiceConnector.createSink(SinkModelMapper.toSinkContent(model));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return SinkModelMapper.toModel(sink);
    }

    @Override
    public SinkModel updateSink(SinkModel model) throws NullPointerException, ProxyException {
        final String callerMethodName = "updateSink";
        Sink sink = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", model.getId(), model.getVersion());
        try {
            sink = flowStoreServiceConnector.updateSink(SinkModelMapper.toSinkContent(model), model.getId(), model.getVersion());
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return SinkModelMapper.toModel(sink);
    }

    @Override
    public void deleteSink(long sinkId, long version) throws NullPointerException, ProxyException {
        final String callerMethodName = "deleteSink";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", sinkId, version);
        try {
            flowStoreServiceConnector.deleteSink(sinkId, version);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
    }

    @Override
    public List<SinkModel> findAllSinks() throws ProxyException {
        final String callerMethodName = "findAllSinks";
        List<Sink> sinks = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            sinks = flowStoreServiceConnector.findAllSinks();
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return SinkModelMapper.toListOfSinkModels(sinks);
    }

    @Override
    public SinkModel getSink(Long id) throws ProxyException {
        final String callerMethodName = "getSink";
        Sink sink = null;
        log.trace("FlowStoreProxy: \" + callerMethodName + \"({});", id);
        try {
            sink = flowStoreServiceConnector.getSink(id);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return SinkModelMapper.toModel(sink);
    }

    /*
     * Harvesters
     */

    // Generic harvesters
    @Override
    public HarvesterConfig updateHarvesterConfig(HarvesterConfig config) throws ProxyException {
        final String callerMethodName = "updateHarvesterConfig";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", config.getId(), config.getVersion());
        try {
            return flowStoreServiceConnector.updateHarvesterConfig(config);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
            return null;
        }
    }

    @Override
    public void deleteHarvesterConfig(long sinkId, long version) throws NullPointerException, ProxyException {
        final String callerMethodName = "deleteHarvesterConfig";
        log.trace("FlowStoreProxy: " + callerMethodName + "({}, {});", sinkId, version);
        try {
            flowStoreServiceConnector.deleteHarvesterConfig(sinkId, version);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
    }

    // RR harvester
    @Override
    public RRHarvesterConfig createRRHarvesterConfig(RRHarvesterConfig config) throws ProxyException {
        final String callerMethodName = "createRRHarvesterConfig";
        //log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", config.getKey()); // ja7
        try {
            return flowStoreServiceConnector.createHarvesterConfig(config.getContent(), RRHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
            return null;
        }
    }

    @Override
    public List<RRHarvesterConfig> findAllRRHarvesterConfigs() throws ProxyException {
        final String callerMethodName = "findAllRRHarvesterConfigs";
        List<RRHarvesterConfig> rrHarvesterConfigs = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            rrHarvesterConfigs = new ArrayList<>();
            rrHarvesterConfigs.addAll(flowStoreServiceConnector.findHarvesterConfigsByType(RRHarvesterConfig.class));
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return rrHarvesterConfigs;
    }

    @Override
    public RRHarvesterConfig getRRHarvesterConfig(long id) throws ProxyException {
        final String callerMethodName = "getRRHarvesterConfig";
        RRHarvesterConfig harvesterConfig = null;
        log.trace("FlowStoreProxy: \" + callerMethodName + \"({});", id);
        try {
            harvesterConfig = flowStoreServiceConnector.getHarvesterConfig(id, RRHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return harvesterConfig;
    }

    // Tickle Repo Harvesters
    @Override
    public TickleRepoHarvesterConfig createTickleRepoHarvesterConfig(TickleRepoHarvesterConfig config) throws ProxyException {
        final String callerMethodName = "createTickleRepoHarvesterConfig";
        log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", config.getId());
        try {
            return flowStoreServiceConnector.createHarvesterConfig(config.getContent(), TickleRepoHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
            return null;
        }
    }

    @Override
    public List<TickleRepoHarvesterConfig> findAllTickleRepoHarvesterConfigs() throws ProxyException {
        final String callerMethodName = "findAllTickleRepoHarvesterConfigs";
        List<TickleRepoHarvesterConfig> tickleRepoHarvesterConfigs = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            tickleRepoHarvesterConfigs = flowStoreServiceConnector.findHarvesterConfigsByType(TickleRepoHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return tickleRepoHarvesterConfigs;
    }

    @Override
    public TickleRepoHarvesterConfig getTickleRepoHarvesterConfig(long id) throws ProxyException {
        final String callerMethodName = "getTickleRepoHarvesterConfig";
        TickleRepoHarvesterConfig harvesterConfig = null;
        log.trace("FlowStoreProxy: \" + callerMethodName + \"({});", id);
        try {
            harvesterConfig = flowStoreServiceConnector.getHarvesterConfig(id, TickleRepoHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return harvesterConfig;
    }

    // CoRepo Harvesters
    @Override
    public CoRepoHarvesterConfig createCoRepoHarvesterConfig(CoRepoHarvesterConfig config) throws ProxyException {
        final String callerMethodName = "createCoRepoHarvesterConfig";
        log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", config.getId());
        try {
            return flowStoreServiceConnector.createHarvesterConfig(config.getContent(), CoRepoHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
            return null;
        }
    }

    @Override
    public List<CoRepoHarvesterConfig> findAllCoRepoHarvesterConfigs() throws ProxyException {
        final String callerMethodName = "findAllCoRepoHarvesterConfigs";
        List<CoRepoHarvesterConfig> CoRepoHarvesterConfigs = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            CoRepoHarvesterConfigs = flowStoreServiceConnector.findHarvesterConfigsByType(CoRepoHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return CoRepoHarvesterConfigs;
    }

    @Override
    public CoRepoHarvesterConfig getCoRepoHarvesterConfig(long id) throws ProxyException {
        final String callerMethodName = "getCoRepoHarvesterConfig";
        CoRepoHarvesterConfig harvesterConfig = null;
        log.trace("FlowStoreProxy: \" + callerMethodName + \"({});", id);
        try {
            harvesterConfig = flowStoreServiceConnector.getHarvesterConfig(id, CoRepoHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return harvesterConfig;
    }


    // Infomedia harvesters
    @Override
    public InfomediaHarvesterConfig createInfomediaHarvesterConfig(InfomediaHarvesterConfig config) throws ProxyException {
        final String callerMethodName = "createInfomediaHarvesterConfig";
        log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", config.getId());
        try {
            return flowStoreServiceConnector.createHarvesterConfig(config.getContent(), InfomediaHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
            return null;
        }
    }

    @Override
    public List<InfomediaHarvesterConfig> findAllInfomediaHarvesterConfigs() throws ProxyException {
        final String callerMethodName = "findAllInfoemdiaHarvesterConfigs";
        List<InfomediaHarvesterConfig> configs = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            configs = flowStoreServiceConnector.findHarvesterConfigsByType(InfomediaHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return configs;
    }

    @Override
    public InfomediaHarvesterConfig getInfomediaHarvesterConfig(long id) throws ProxyException {
        final String callerMethodName = "getInfomediaHarvesterConfig";
        InfomediaHarvesterConfig config = null;
        log.trace("FlowStoreProxy: \" + callerMethodName + \"({});", id);
        try {
            config = flowStoreServiceConnector.getHarvesterConfig(id, InfomediaHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return config;
    }

    // PeriodicJobs harvesters
    @Override
    public PeriodicJobsHarvesterConfig createPeriodicJobsHarvesterConfig(PeriodicJobsHarvesterConfig config) throws ProxyException {
        final String callerMethodName = "createPeriodicJobsHarvesterConfig";
        log.trace("FlowStoreProxy: " + callerMethodName + "(\"{}\");", config.getId());
        try {
            return flowStoreServiceConnector.createHarvesterConfig(config.getContent(), PeriodicJobsHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
            return null;
        }
    }

    @Override
    public List<PeriodicJobsHarvesterConfig> findAllPeriodicJobsHarvesterConfigs() throws ProxyException {
        final String callerMethodName = "findAllPeriodicJobsHarvesterConfigs";
        List<PeriodicJobsHarvesterConfig> configs = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            configs = flowStoreServiceConnector.findHarvesterConfigsByType(PeriodicJobsHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return configs;
    }

    @Override
    public PeriodicJobsHarvesterConfig getPeriodicJobsHarvesterConfig(long id) throws ProxyException {
        final String callerMethodName = "getPeriodicJobsHarvesterConfig";
        PeriodicJobsHarvesterConfig config = null;
        log.trace("FlowStoreProxy: \" + callerMethodName + \"({});", id);
        try {
            config = flowStoreServiceConnector.getHarvesterConfig(id, PeriodicJobsHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return config;
    }

    // Promat harvesters
    @Override
    public List<PromatHarvesterConfig> findAllPromatHarvesterConfigs() throws ProxyException {
        final String callerMethodName = "findAllPromatHarvesterConfigs";
        List<PromatHarvesterConfig> configs = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            configs = flowStoreServiceConnector.findHarvesterConfigsByType(PromatHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return configs;
    }

    @Override
    public PromatHarvesterConfig getPromatHarvesterConfig(long id) throws ProxyException {
        final String callerMethodName = "getPromatHarvesterConfig";
        PromatHarvesterConfig config = null;
        log.trace("FlowStoreProxy: \" + callerMethodName + \"({});", id);
        try {
            config = flowStoreServiceConnector.getHarvesterConfig(id, PromatHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return config;
    }

    // DMat harvesters
    @Override
    public List<DMatHarvesterConfig> findAllDMatHarvesterConfigs() throws ProxyException {
        final String callerMethodName = "findAllDMatHarvesterConfigs";
        List<DMatHarvesterConfig> configs = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            configs = flowStoreServiceConnector.findHarvesterConfigsByType(DMatHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return configs;
    }

    @Override
    public DMatHarvesterConfig getDMatHarvesterConfig(long id) throws ProxyException {
        final String callerMethodName = "getDMatHarvesterConfig";
        DMatHarvesterConfig config = null;
        log.trace("FlowStoreProxy: \" + callerMethodName + \"({});", id);
        try {
            config = flowStoreServiceConnector.getHarvesterConfig(id, DMatHarvesterConfig.class);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return config;
    }

    /*
     * Gatekeeper destinations
     */

    @Override
    public GatekeeperDestination createGatekeeperDestination(GatekeeperDestination gatekeeperDestination) throws NullPointerException, ProxyException {
        final String callerMethodName = "createGatekeeperDestination";
        GatekeeperDestination destination = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            destination = flowStoreServiceConnector.createGatekeeperDestination(gatekeeperDestination);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return destination;
    }

    @Override
    public List<GatekeeperDestination> findAllGatekeeperDestinations() throws ProxyException {
        final String callerMethodName = "findAllGatekeeperDestinations";
        List<GatekeeperDestination> destinations = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            destinations = flowStoreServiceConnector.findAllGatekeeperDestinations();
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return destinations;
    }

    @Override
    public void deleteGatekeeperDestination(long id) throws ProxyException {
        final String callerMethodName = "deleteGatekeeperDestination";
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            flowStoreServiceConnector.deleteGatekeeperDestination(id);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
    }

    @Override
    public GatekeeperDestination updateGatekeeperDestination(GatekeeperDestination gatekeeperDestination) throws ProxyException {
        final String callerMethodName = "updateGatekeeperDestination";
        GatekeeperDestination updatedGatekeeperDestination = null;
        log.trace("FlowStoreProxy: " + callerMethodName + "();");
        try {
            updatedGatekeeperDestination = flowStoreServiceConnector.updateGatekeeperDestination(gatekeeperDestination);
        } catch (Exception genericException) {
            handleExceptions(genericException, callerMethodName);
        }
        return updatedGatekeeperDestination;
    }


    /*
     * Other
     */

    public void close() {
        HttpClient.closeClient(client);
    }

    /*
     * Private methods
     */

    /**
     * Handle exceptions thrown by the FlowStoreServiceConnector and wrap them in ProxyExceptions
     *
     * @param exception        generic exception which in turn can be both Checked and Unchecked
     * @param callerMethodName calling method name for logging
     * @throws ProxyException       GUI exception
     * @throws NullPointerException Null pointer exception
     */
    private void handleExceptions(Exception exception, String callerMethodName) throws ProxyException, NullPointerException {
        if (exception instanceof FlowStoreServiceConnectorUnexpectedStatusCodeException) {
            FlowStoreServiceConnectorUnexpectedStatusCodeException fsscusce = (FlowStoreServiceConnectorUnexpectedStatusCodeException) exception;
            log.error("FlowStoreProxy: " + callerMethodName + " - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(fsscusce.getStatusCode()), fsscusce);
            throw new ProxyException(StatusCodeTranslator.toProxyError(fsscusce.getStatusCode()), fsscusce.getMessage());
        } else if (exception instanceof FlowStoreServiceConnectorException) {
            FlowStoreServiceConnectorException fssce = (FlowStoreServiceConnectorException) exception;
            log.error("FlowStoreProxy: " + callerMethodName + " - Service Not Found", fssce);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, fssce);
        } else if (exception instanceof IllegalArgumentException) {
            IllegalArgumentException iae = (IllegalArgumentException) exception;
            log.error("FlowStoreProxy: " + callerMethodName + " - Invalid Field Value Exception", iae);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, iae);
        } else if (exception instanceof JavaScriptProjectFetcherException) {
            JavaScriptProjectFetcherException jspfe = (JavaScriptProjectFetcherException) exception;
            log.error("FlowStoreProxy: " + callerMethodName + " - Subversion Lookup Failed Exception", jspfe);
            throw new ProxyException(ProxyError.SUBVERSION_LOOKUP_FAILED, jspfe);
        } else if (exception instanceof NullPointerException) {
            log.error("FlowStoreProxy: " + callerMethodName + " - Null Pointer Exception", exception);
            throw (NullPointerException) exception;
        } else {
            log.error("FlowStoreProxy: " + callerMethodName + " - Unexpected Exception", exception);
            throw new ProxyException(ProxyError.ERROR_UNKNOWN, exception);
        }
    }

    /**
     * Fetches the latest Submitter Models given as input
     *
     * @param submitterModels The current versions of the Submitter Models
     * @return The latest versions of the Submitter Models
     */
    private List<SubmitterModel> getSubmitterModelsLatestVersion(List<SubmitterModel> submitterModels) throws FlowStoreServiceConnectorException {
        List<SubmitterModel> models = new ArrayList<>();
        for (SubmitterModel model : submitterModels) {
            models.add(SubmitterModelMapper.toModel(flowStoreServiceConnector.getSubmitter(model.getId())));
        }
        return models;
    }

    /**
     * Fetches the latest version of the Flow Model given as input
     *
     * @param flowModel The current version of the Flow Model
     * @return The latest version of the Flow Model
     */
    private FlowModel getFlowModelLatestVersion(FlowModel flowModel) throws FlowStoreServiceConnectorException {
        return FlowModelMapper.toModel(flowStoreServiceConnector.getFlow(flowModel.getId()));
    }

    /**
     * Fetches the latest version of the Sink Model given as input
     *
     * @param sinkModel The current version of the Sink Model
     * @return The latest version of the Sink Model
     */
    private SinkModel getSinkModelLatestVersion(SinkModel sinkModel) throws FlowStoreServiceConnectorException {
        return SinkModelMapper.toModel(flowStoreServiceConnector.getSink(sinkModel.getId()));
    }

    /**
     * For each flow component model given as input, the method retrieves the latest version of a flow component
     * from the flow store
     *
     * @param flowComponentModels containing information regarding which flow components should be retrieved from the flow store
     * @return flowComponents, a list containing the retrieved flow components
     * @throws FlowStoreServiceConnectorException Flowstore Service Connector Exception
     */
    private List<FlowComponent> getFlowComponentsLatestVersion(List<FlowComponentModel> flowComponentModels) throws FlowStoreServiceConnectorException {
        List<FlowComponent> flowComponents = new ArrayList<>(flowComponentModels.size());
        for (FlowComponentModel flowComponentModel : flowComponentModels) {
            FlowComponent flowComponent = flowStoreServiceConnector.getFlowComponent(flowComponentModel.getId());
            flowComponents.add(flowComponent);
        }
        return flowComponents;
    }

    /**
     * This method loops through a list of flow component models and compares the id of each, with the id of the flow components nested
     * within the flow.
     * if the id is located, the method takes the flow component from the flow, and adds it to a list
     * if the id is not located, the flow component is taken directly from the flow store and adds it to the list
     * <p>
     * This is done for the following reasons:
     * The frontend object (flowComponentModel) does not contain the full data set needed by
     * the backend object (flowComponent).
     * <p>
     * The flow component is nested within a flow. Therefore the flow components of an existing flow cannot be replaced by
     * retrieving the latest version of the same flow component from the flow store.
     *
     * @param flow, the version of the flow that is currently saved in the underlying database
     * @param model containing the updated flow data
     * @return flowComponents, a list containing the flow components that should be used.
     * @throws FlowStoreServiceConnectorException Flowstore Service Connector Exception
     */
    private List<FlowComponent> getFlowComponents(Flow flow, List<FlowComponentModel> model) throws FlowStoreServiceConnectorException {
        List<FlowComponent> flowComponents = new ArrayList<>(model.size());
        for (FlowComponentModel flowComponentModel : model) {
            int counter = 0;
            boolean isNewFlowComponent = true;
            while (isNewFlowComponent && counter < flow.getContent().getComponents().size()) {
                if (flowComponentModel.getId() == flow.getContent().getComponents().get(counter).getId()) {
                    // The flow component lave been located within the existing flow. Re-use the flow component
                    flowComponents.add(flow.getContent().getComponents().get(counter));
                    // End loop.
                    isNewFlowComponent = false;
                }
                counter++;
            }
            if (isNewFlowComponent) {
                // Retrieve the latest version of the flow component from the flow store
                flowComponents.add(flowStoreServiceConnector.getFlowComponent(flowComponentModel.getId()));
            }
        }
        return flowComponents;
    }

    private fetchRequiredJavaScriptResult fetchRequiredJavaScripts(FlowComponentModel model) throws JavaScriptProjectFetcherException {
        try {
            final JavaScriptProject javaScriptProject = javaScriptSubversionProject.fetchRequiredJavaScript(
                    model.getSvnProject(),
                    Long.valueOf(model.getSvnRevision()),
                    model.getInvocationJavascript(),
                    model.getInvocationMethod());
            return new fetchRequiredJavaScriptResult(javaScriptProject.getJavaScripts(), javaScriptProject.getRequireCache());
        } catch (JavaScriptProjectException e) {
            throw JavaScriptProjectFetcherServlet.asJavaScriptProjectFetcherException(e);
        }
    }

    private fetchRequiredJavaScriptResult fetchRequiredJavaScriptsForNext(FlowComponentModel model) throws JavaScriptProjectFetcherException {
        try {
            final JavaScriptProject javaScriptProject = javaScriptSubversionProject.fetchRequiredJavaScript(
                    model.getSvnProject(),
                    Long.valueOf(model.getSvnNext()),
                    model.getInvocationJavascript(),
                    model.getInvocationMethod());
            return new fetchRequiredJavaScriptResult(javaScriptProject.getJavaScripts(), javaScriptProject.getRequireCache());
        } catch (JavaScriptProjectException e) {
            throw JavaScriptProjectFetcherServlet.asJavaScriptProjectFetcherException(e);
        }
    }


    // Additional stuff for the Cache class to facilitate usage of exception throwing Supplier classes

    @FunctionalInterface
    public interface Supplier_WithExceptions<T, E extends Exception> {
        T get() throws E;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E {
        throw (E) exception;
    }

    private static <T, E extends Exception> Supplier<T> rethrowSupplier(Supplier_WithExceptions<T, E> function) {
        return () -> {
            try {
                return function.get();
            } catch (Exception exception) {
                throwAsUnchecked(exception);
                return null;
            }
        };
    }


}
