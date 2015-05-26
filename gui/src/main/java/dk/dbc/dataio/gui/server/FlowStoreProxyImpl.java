package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.StatusCodeTranslator;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher.fetchRequiredJavaScriptResult;
import dk.dbc.dataio.gui.server.modelmappers.FlowBinderModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.FlowComponentModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.FlowModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.SinkModelMapper;
import dk.dbc.dataio.gui.server.modelmappers.SubmitterModelMapper;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.List;

public class FlowStoreProxyImpl implements FlowStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyImpl.class);
    final Client client;
    final String baseUrl;
    final String subversionScmEndpoint;
    FlowStoreServiceConnector flowStoreServiceConnector;
    JavaScriptProjectFetcher javaScriptProjectFetcher;

    public FlowStoreProxyImpl() throws NamingException{
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
        log.info("FlowStoreProxy: Using Base URL {}", baseUrl);
        subversionScmEndpoint = ServiceUtil.getSubversionScmEndpoint();
        flowStoreServiceConnector = new FlowStoreServiceConnector(client, baseUrl);
        javaScriptProjectFetcher = new JavaScriptProjectFetcherImpl(subversionScmEndpoint);
    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    FlowStoreProxyImpl(FlowStoreServiceConnector flowStoreServiceConnector) throws NamingException{
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        subversionScmEndpoint = null;
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
        log.info("FlowStoreProxy: Using Base URL {}", baseUrl);
    }
    //This constructor is intended for test purpose only with reference to dependency injection.
    FlowStoreProxyImpl(FlowStoreServiceConnector flowStoreServiceConnector, JavaScriptProjectFetcherImpl javaScriptProjectFetcher) throws NamingException{
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        this.javaScriptProjectFetcher = javaScriptProjectFetcher;
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
        log.info("FlowStoreProxy: Using Base URL {}", baseUrl);
        this.subversionScmEndpoint = ServiceUtil.getSubversionScmEndpoint();
    }

    /*
     * Flows
     */
    @Override
    public FlowModel createFlow(FlowModel model) throws NullPointerException, ProxyException {
        Flow flow;
        List<FlowComponent> flowComponents;
        log.trace("FlowStoreProxy: createFlow(\"{}\");", model.getFlowName());
        final StopWatch stopWatch = new StopWatch();
        try {
            flowComponents = getFlowComponentsLatestVersion(model.getFlowComponents());
            flow = flowStoreServiceConnector.createFlow(FlowModelMapper.toFlowContent(model, flowComponents));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: createFlow - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: createFlow - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: createFlow - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("FlowStoreProxy: createFlow took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowModelMapper.toModel(flow);
    }

    public FlowModel updateFlow(FlowModel model) throws NullPointerException, ProxyException {
        Flow flow;
        List<FlowComponent> flowComponents;
        log.trace("FlowStoreProxy: updateFlow({});", model.getId(), model.getVersion());
        final StopWatch stopWatch = new StopWatch();
        try {
            // Retrieve the currently saved version of the flow
            Flow flowOpenedForUpdate = flowStoreServiceConnector.getFlow(model.getId());

            // If the flow has been updated by another user: Throw proxyException - conflict error
            if (model.getVersion() != flowOpenedForUpdate.getVersion()) {
                log.error("FlowStoreProxy: updateFlow - Concurrent Update Error");
                throw new ProxyException(ProxyError.CONFLICT_ERROR, "Concurrent Update Error");
            }
            else {
                flowComponents = getFlowComponents(flowOpenedForUpdate, model.getFlowComponents());
                flow = flowStoreServiceConnector.updateFlow(FlowModelMapper.toFlowContent(model, flowComponents), model.getId(), model.getVersion());
            }
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: updateFlow - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: updateFlow - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: updateFlow - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("FlowStoreProxy: updateFlow took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowModelMapper.toModel(flow);
    }

    @Override
    public List<FlowModel> findAllFlows() throws ProxyException {
        final List<Flow> flows;
        log.trace("FlowStoreProxy: findAllFlows();");
        final StopWatch stopWatch = new StopWatch();
        try {
            flows = flowStoreServiceConnector.findAllFlows();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: findAllFlows - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: findAllFlows - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: findAllFlows took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowModelMapper.toListOfFlowModels(flows);
    }

    @Override
    public FlowModel getFlow(Long id) throws ProxyException {
        final Flow flow;
        log.trace("Trace - FlowStoreProxy: getFlow({});", id);
        final StopWatch stopWatch = new StopWatch();
        try {
            flow = flowStoreServiceConnector.getFlow(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: getFlow - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: getFlow - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: getFlow took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowModelMapper.toModel(flow);
    }


    /*
     * Flow Components
     */

    @Override
    public FlowComponentModel createFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException {
        FlowComponent flowComponent;
        log.trace("FlowStoreProxy: createFlowComponent(\"{}\");", model.getName());
        final StopWatch stopWatch = new StopWatch();
        try {
            fetchRequiredJavaScriptResult fetchRequiredJavaScriptResult = fetchRequiredJavaScripts(model);
            flowComponent = flowStoreServiceConnector.createFlowComponent(FlowComponentModelMapper.toFlowComponentContent(model, fetchRequiredJavaScriptResult));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: createFlowComponent - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: createFlowComponent - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: createFlowComponent - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } catch (JavaScriptProjectFetcherException e) {
            log.error("FlowStoreProxy: createFlowComponent - Subversion Lookup Failed Exception", e);
            throw new ProxyException(ProxyError.SUBVERSION_LOOKUP_FAILED, e);
        } finally {
            log.debug("FlowStoreProxy: createFlowComponent took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowComponentModelMapper.toModel(flowComponent);
    }

    @Override
    public FlowComponentModel updateFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException {
        FlowComponent flowComponent;
        log.trace("FlowStoreProxy: updateFlowComponent({}, {});", model.getId(), model.getVersion());
        final StopWatch stopWatch = new StopWatch();
        try {
            fetchRequiredJavaScriptResult fetchRequiredJavaScriptResult = fetchRequiredJavaScripts(model);
            flowComponent = flowStoreServiceConnector.updateFlowComponent(
                    FlowComponentModelMapper.toFlowComponentContent(model, fetchRequiredJavaScriptResult), model.getId(), model.getVersion());

        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: updateFlowComponent - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: updateFlowComponent - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: updateFlowComponent - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } catch (JavaScriptProjectFetcherException e) {
            log.error("FlowStoreProxy: updateFlowComponent - Subversion Lookup Failed Exception", e);
            throw new ProxyException(ProxyError.SUBVERSION_LOOKUP_FAILED, e);
        } finally {
            log.debug("FlowStoreProxy: updateFlowComponent took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowComponentModelMapper.toModel(flowComponent);
    }

    @Override
    public FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException {
        Flow flow;
        final StopWatch stopWatch = new StopWatch();
        log.trace("FlowStoreProxy: refreshFlowComponent({}, {});", id, version);
        try {
            flow = flowStoreServiceConnector.refreshFlowComponents(id, version);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: refreshFlowComponent - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: refreshFlowComponent - Service Not Found Exception", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: refreshFlowComponents took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowModelMapper.toModel(flow);
    }

    @Override
    public List<FlowComponentModel> findAllFlowComponents() throws ProxyException {
        final List<FlowComponent> result;
        log.trace("FlowStoreProxy: findAllFlowComponents();");
        final StopWatch stopWatch = new StopWatch();
        try {
            result = flowStoreServiceConnector.findAllFlowComponents();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: findAllFlowComponents - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: findAllFlowComponents - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: findAllFlowComponents took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowComponentModelMapper.toListOfFlowComponentModels(result);
    }

    @Override
    public FlowComponentModel getFlowComponent(Long id) throws ProxyException {
        final FlowComponent result;
        log.trace("FlowStoreProxy: getFlowComponent({});", id);
        final StopWatch stopWatch = new StopWatch();
        try {
            result = flowStoreServiceConnector.getFlowComponent(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: getFlowComponent - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: getFlowComponent - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: getFlowComponent took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowComponentModelMapper.toModel(result);
    }


    /*
     * Flows Binders
     */

    @Override
    public FlowBinderModel createFlowBinder(FlowBinderModel model) throws NullPointerException, ProxyException {
        FlowBinderModel flowBinderModel;
        FlowBinder flowBinder;
        log.trace("FlowStoreProxy: createFlowBinder(\"{}\");", model.getName());
        final StopWatch stopWatch = new StopWatch();
        try {
            flowBinder = flowStoreServiceConnector.createFlowBinder(FlowBinderModelMapper.toFlowBinderContent(model));
            flowBinderModel = FlowBinderModelMapper.toModel(flowBinder,
                    getFlowModelLatestVersion(model.getFlowModel()),
                    getSubmitterModelsLatestVersion(model.getSubmitterModels()),
                    getSinkModelLatestVersion(model.getSinkModel()));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: createFlowBinder - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: createFlowBinder - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: createFlowBinder took {} milliseconds", stopWatch.getElapsedTime());
        }
        return flowBinderModel;
    }

    @Override
    public FlowBinderModel updateFlowBinder(FlowBinderModel model) throws NullPointerException, ProxyException {
        FlowBinder flowBinder;
        List<SubmitterModel> submitterModels;
        FlowModel flowModel;
        SinkModel sinkModel;
        log.trace("FlowStoreProxy: updateFlowBinder({}, {});", model.getId(), model.getVersion());
        final StopWatch stopWatch = new StopWatch();
        try {
            flowBinder = flowStoreServiceConnector.updateFlowBinder(FlowBinderModelMapper.toFlowBinderContent(model), model.getId(), model.getVersion());
            submitterModels = new ArrayList<SubmitterModel>(flowBinder.getContent().getSubmitterIds().size());
            for(Long submitterId : flowBinder.getContent().getSubmitterIds()) {
                submitterModels.add(SubmitterModelMapper.toModel(flowStoreServiceConnector.getSubmitter(submitterId)));
            }
            flowModel = FlowModelMapper.toModel(flowStoreServiceConnector.getFlow(flowBinder.getContent().getFlowId()));
            sinkModel = SinkModelMapper.toModel(flowStoreServiceConnector.getSink(flowBinder.getContent().getSinkId()));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: updateFlowBinder - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: updateFlowBinder - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: updateFlowBinder - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("FlowStoreProxy: updateFlowBinder took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowBinderModelMapper.toModel(flowBinder, flowModel, submitterModels, sinkModel);
    }

    @Override
    public List<FlowBinderModel> findAllFlowBinders() throws ProxyException {
        final List<FlowBinder> flowBinders;
        final List<FlowBinderModel> flowBinderModels = new ArrayList<FlowBinderModel>();
        List<SubmitterModel> submitterModels;
        log.trace("FlowStoreProxy: findAllFlowBinders();");
        final StopWatch stopWatch = new StopWatch();
        try {
            flowBinders = flowStoreServiceConnector.findAllFlowBinders();
            for (FlowBinder flowBinder: flowBinders) {
                submitterModels = new ArrayList<SubmitterModel>(flowBinder.getContent().getSubmitterIds().size());
                for (long submitterId: flowBinder.getContent().getSubmitterIds()) {
                    submitterModels.add(getSubmitter(submitterId));
                }
                flowBinderModels.add(
                        FlowBinderModelMapper.toModel(
                                flowBinder,
                                getFlow(flowBinder.getContent().getFlowId()),
                                submitterModels,
                                getSink(flowBinder.getContent().getSinkId())
                                )
                );
            }
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: findAllFlowBinders - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: findAllFlowBinders - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: findAllFlowBinders took {} milliseconds", stopWatch.getElapsedTime());
        }
        return flowBinderModels;
    }

    @Override
    public FlowBinderModel getFlowBinder(long id) throws ProxyException {
        final FlowBinder flowBinder;
        final FlowModel flowModel;
        final List<SubmitterModel> submitterModels = new ArrayList<SubmitterModel>();
        final SinkModel sinkModel;
        log.trace("FlowStoreProxy: getFlowBinder({});", id);
        final StopWatch stopWatch = new StopWatch();
        try {
            flowBinder = flowStoreServiceConnector.getFlowBinder(id);
            flowModel = FlowModelMapper.toModel(flowStoreServiceConnector.getFlow(flowBinder.getContent().getFlowId()));
            for (long submitterId: flowBinder.getContent().getSubmitterIds()) {
                submitterModels.add(SubmitterModelMapper.toModel(flowStoreServiceConnector.getSubmitter(submitterId)));
            }
            sinkModel = SinkModelMapper.toModel(flowStoreServiceConnector.getSink(flowBinder.getContent().getSinkId()));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: getFlowBinder - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: getFlowBinder - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: getFlowBinder took {} milliseconds", stopWatch.getElapsedTime());
        }
        return FlowBinderModelMapper.toModel(flowBinder, flowModel, submitterModels, sinkModel);
    }

    /*
     * Submitters
     */


    @Override
    public SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException {
        Submitter submitter;
        log.trace("FlowStoreProxy: createSubmitter({}, \"{}\");", model.getNumber(), model.getName());
        final StopWatch stopWatch = new StopWatch();
        try {
            submitter = flowStoreServiceConnector.createSubmitter(SubmitterModelMapper.toSubmitterContent(model));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: createSubmitter - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: createSubmitter - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: createSubmitter - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("FlowStoreProxy: createSubmitter took {} milliseconds", stopWatch.getElapsedTime());
        }
        return SubmitterModelMapper.toModel(submitter);
    }

    @Override
    public SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException, IllegalArgumentException {
        Submitter submitter;
        log.trace("FlowStoreProxy: updateSubmitter({}, {});", model.getId(), model.getVersion());
        final StopWatch stopWatch = new StopWatch();
        try {
            submitter = flowStoreServiceConnector.updateSubmitter(SubmitterModelMapper.toSubmitterContent(model), model.getId(), model.getVersion());
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: updateSubmitter - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: updateSubmitter - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: updateSubmitter - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("FlowStoreProxy: updateSubmitter took {} milliseconds", stopWatch.getElapsedTime());
        }
        return SubmitterModelMapper.toModel(submitter);
    }

    @Override
    public List<SubmitterModel> findAllSubmitters() throws ProxyException {
        final List<Submitter> submitters;
        log.trace("FlowStoreProxy: findAllSubmitters();");
        final StopWatch stopWatch = new StopWatch();
        try {
            submitters = flowStoreServiceConnector.findAllSubmitters();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: findAllSubmitters - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: findAllSubmitters - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: findAllSubmitters took {} milliseconds", stopWatch.getElapsedTime());
        }
        return SubmitterModelMapper.toListOfSubmitterModels(submitters);
    }

    @Override
    public SubmitterModel getSubmitter(Long id) throws ProxyException {
        final Submitter submitter;
        log.trace("FlowStoreProxy: getSubmitter({});", id);
        final StopWatch stopWatch = new StopWatch();
        try {
            submitter = flowStoreServiceConnector.getSubmitter(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: getSubmitter - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: getSubmitter - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: getSubmitter took {} milliseconds", stopWatch.getElapsedTime());
        }
        return SubmitterModelMapper.toModel(submitter);
    }

    /*
     * Sinks
     */


    @Override
    public SinkModel createSink(SinkModel model) throws NullPointerException, ProxyException {
        Sink sink;
        log.trace("FlowStoreProxy: createSink(\"{}\");", model.getSinkName());
        final StopWatch stopWatch = new StopWatch();
        try {
            sink = flowStoreServiceConnector.createSink(SinkModelMapper.toSinkContent(model));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: createSink - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: createSink - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: createSink - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("FlowStoreProxy: createSink took {} milliseconds", stopWatch.getElapsedTime());
        }
        return SinkModelMapper.toModel(sink);
    }

    @Override
    public SinkModel updateSink(SinkModel model) throws NullPointerException, ProxyException {
        Sink sink;
        log.trace("FlowStoreProxy: updateSink({}, {});", model.getId(), model.getVersion());
        final StopWatch stopWatch = new StopWatch();
        try {
            sink = flowStoreServiceConnector.updateSink(SinkModelMapper.toSinkContent(model), model.getId(), model.getVersion());
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: updateSink - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: updateSink - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            log.error("FlowStoreProxy: updateSink - Invalid Field Value Exception", e);
            throw new ProxyException(ProxyError.MODEL_MAPPER_INVALID_FIELD_VALUE, e);
        } finally {
            log.debug("FlowStoreProxy: updateSink took {} milliseconds", stopWatch.getElapsedTime());
        }
        return SinkModelMapper.toModel(sink);
    }

    @Override
    public List<SinkModel> findAllSinks() throws ProxyException {
        final List<Sink> sinks;
        log.trace("FlowStoreProxy: findAllSinks();");
        final StopWatch stopWatch = new StopWatch();
        try {
            sinks = flowStoreServiceConnector.findAllSinks();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: findAllSinks - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: findAllSinks - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: findAllSinks took {} milliseconds", stopWatch.getElapsedTime());
        }
        return SinkModelMapper.toListOfSinkModels(sinks);
    }

    @Override
    public SinkModel getSink(Long id) throws ProxyException {
        final Sink sink;
        log.trace("FlowStoreProxy: getSink({});", id);
        final StopWatch stopWatch = new StopWatch();
        try {
            sink = flowStoreServiceConnector.getSink(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            log.error("FlowStoreProxy: getSink - Unexpected Status Code Exception({})", StatusCodeTranslator.toProxyError(e.getStatusCode()), e);
            throw new ProxyException(StatusCodeTranslator.toProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            log.error("FlowStoreProxy: getSink - Service Not Found", e);
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } finally {
            log.debug("FlowStoreProxy: getSink took {} milliseconds", stopWatch.getElapsedTime());
        }
        return SinkModelMapper.toModel(sink);
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
     * Fetches the latest Submitter Models given as input
     * @param submitterModels The current versions of the Submitter Models
     * @return The latest versions of the Submitter Models
     */
    private List<SubmitterModel> getSubmitterModelsLatestVersion(List<SubmitterModel> submitterModels) throws FlowStoreServiceConnectorException {
        List<SubmitterModel> models = new ArrayList<SubmitterModel>();
        for (SubmitterModel model: submitterModels) {
            models.add(SubmitterModelMapper.toModel(flowStoreServiceConnector.getSubmitter(model.getId())));
        }
        return models;
    }

    /**
     * Fetches the latest version of the Flow Model given as input
     * @param flowModel The current version of the Flow Model
     * @return The latest version of the Flow Model
     */
    private FlowModel getFlowModelLatestVersion(FlowModel flowModel) throws FlowStoreServiceConnectorException {
        return FlowModelMapper.toModel(flowStoreServiceConnector.getFlow(flowModel.getId()));
    }

    /**
     * Fetches the latest version of the Sink Model given as input
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
     *
     * @throws FlowStoreServiceConnectorException
     */
    private List<FlowComponent> getFlowComponentsLatestVersion (List<FlowComponentModel> flowComponentModels) throws FlowStoreServiceConnectorException {
        List<FlowComponent> flowComponents = new ArrayList<FlowComponent>(flowComponentModels.size());
        for(FlowComponentModel flowComponentModel : flowComponentModels) {
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
     *
     * This is done for the following reasons:
     *      The frontend object (flowComponentModel) does not contain the full data set needed by
     *      the backend object (flowComponent).
     *
     *      The flow component is nested within a flow. Therefore the flow components of an existing flow cannot be replaced by
     *      retrieving the latest version of the same flow component from the flow store.
     *
     * @param flow, the version of the flow that is currently saved in the underlying database
     * @param model containing the updated flow data
     *
     * @return flowComponents, a list containing the flow components that should be used.
     * @throws FlowStoreServiceConnectorException
     */
    private List<FlowComponent> getFlowComponents (Flow flow, List<FlowComponentModel> model) throws FlowStoreServiceConnectorException {
        List<FlowComponent> flowComponents = new ArrayList<FlowComponent>(model.size());
        for (FlowComponentModel flowComponentModel : model) {
            int counter = 0;
            boolean isNewFlowComponent = true;
            while(isNewFlowComponent && counter < flow.getContent().getComponents().size()) {
                if (flowComponentModel.getId() == flow.getContent().getComponents().get(counter).getId()) {
                    // The flow component lave been located within the existing flow. Re-use the flow component
                    flowComponents.add(flow.getContent().getComponents().get(counter));
                    // End loop.
                    isNewFlowComponent = false;
                }
                counter ++;
            }
            if (isNewFlowComponent) {
                // Retrieve the latest version of the flow component from the flow store
                flowComponents.add(flowStoreServiceConnector.getFlowComponent(flowComponentModel.getId()));
            }
        }
        return flowComponents;
    }

    private fetchRequiredJavaScriptResult fetchRequiredJavaScripts(FlowComponentModel model) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchRequiredJavaScript(
                model.getSvnProject(),
                Long.valueOf(model.getSvnRevision()),
                model.getInvocationJavascript(),
                model.getInvocationMethod());
    }

}
