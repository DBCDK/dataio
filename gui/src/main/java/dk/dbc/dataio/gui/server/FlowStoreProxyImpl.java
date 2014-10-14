package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.FlowBinderModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.server.ModelMappers.FlowBinderModelMapper;
import dk.dbc.dataio.gui.server.ModelMappers.FlowComponentModelMapper;
import dk.dbc.dataio.gui.server.ModelMappers.FlowModelMapper;
import dk.dbc.dataio.gui.server.ModelMappers.SinkModelMapper;
import dk.dbc.dataio.gui.server.ModelMappers.SubmitterModelMapper;
import org.glassfish.jersey.client.ClientConfig;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class FlowStoreProxyImpl implements FlowStoreProxy {
    final Client client;
    final String baseUrl;
    FlowStoreServiceConnector flowStoreServiceConnector;

    public FlowStoreProxyImpl() throws NamingException{
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
        flowStoreServiceConnector = new FlowStoreServiceConnector(client, baseUrl);
    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    FlowStoreProxyImpl(FlowStoreServiceConnector flowStoreServiceConnector) throws NamingException{
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        this.flowStoreServiceConnector = flowStoreServiceConnector;
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getFlowStoreServiceEndpoint();
    }

    /*
     * Flows
     */
    @Override
    public FlowModel createFlow(FlowModel model) throws NullPointerException, ProxyException {
        Flow flow;
        List<FlowComponent> flowComponents;
        try {
            flowComponents = getFlowComponentsLatestVersion(model.getFlowComponents());
            flow = flowStoreServiceConnector.createFlow(FlowModelMapper.toFlowContent(model, flowComponents));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e){
            throw new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, e);
    }
        return FlowModelMapper.toModel(flow);
    }

    public FlowModel updateFlow(FlowModel model) throws NullPointerException, ProxyException {
        Flow flow;
        List<FlowComponent> flowComponents;
        try {
            // Retrieve the currently saved version of the flow
            Flow flowOpenedForUpdate = flowStoreServiceConnector.getFlow(model.getId());

            // If the flow has been updated by another user: Throw proxyException - conflict error
            if(model.getVersion() != flowOpenedForUpdate.getVersion()) {
                throw new ProxyException(ProxyError.CONFLICT_ERROR, "Concurrent Update Error");
            }
            else {
                flowComponents = getFlowComponents(flowOpenedForUpdate, model.getFlowComponents());
                flow = flowStoreServiceConnector.updateFlow(FlowModelMapper.toFlowContent(model, flowComponents), model.getId(), model.getVersion());
            }
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e){
            throw new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, e);
        }
        return FlowModelMapper.toModel(flow);
    }

    @Override
    public List<Flow> findAllFlows() throws ProxyException {
        final List<Flow> result;
        try {
            result = flowStoreServiceConnector.findAllFlows();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }

    @Override
    public FlowModel getFlow(Long id) throws ProxyException {
        final Flow flow;
        try {
            flow = flowStoreServiceConnector.getFlow(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return FlowModelMapper.toModel(flow);
    }


    /*
     * Flow Components
     */

    @Override
    public FlowComponent createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProxyException {
        FlowComponent flowComponent;
        try {
            flowComponent = flowStoreServiceConnector.createFlowComponent(flowComponentContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return flowComponent;
    }

    @Override
    public FlowComponent updateFlowComponent(FlowComponentContent flowComponentContent, Long id, Long version) throws NullPointerException, ProxyException {
        FlowComponent flowComponent;
        try {
            flowComponent = flowStoreServiceConnector.updateFlowComponent(flowComponentContent, id, version);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return flowComponent;
    }


    @Override
    public Flow refreshFlowComponentsOld(Long id, Long version) throws NullPointerException, ProxyException {
        Flow flow;
        try {
            flow = flowStoreServiceConnector.refreshFlowComponents(id, version);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return flow;
    }

    @Override
    public FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException {
        Flow flow;
        try {
            flow = flowStoreServiceConnector.refreshFlowComponents(id, version);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return FlowModelMapper.toModel(flow);
    }

    @Override
    public List<FlowComponent> findAllFlowComponentsOld() throws ProxyException {
        final List<FlowComponent> result;
        try {
            result = flowStoreServiceConnector.findAllFlowComponents();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }

        return result;
    }

    @Override
    public List<FlowComponentModel> findAllFlowComponents() throws ProxyException {
        final List<FlowComponent> result;
        try {
            result = flowStoreServiceConnector.findAllFlowComponents();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return FlowComponentModelMapper.toListOfFlowComponentModels(result);
    }

    @Override
    public FlowComponent getFlowComponent(Long id) throws ProxyException {
        final FlowComponent result;
        try {
            result = flowStoreServiceConnector.getFlowComponent(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }


    /*
     * Flows Binders
     */

    @Override
    public FlowBinder createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProxyException {
        FlowBinder flowBinder;
        try {
            flowBinder = flowStoreServiceConnector.createFlowBinder(flowBinderContent);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return flowBinder;
    }

    @Override
    public List<FlowBinder> findAllFlowBinders() throws ProxyException {
        final List<FlowBinder> result;
        try {
            result = flowStoreServiceConnector.findAllFlowBinders();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }

    @Override
    public FlowBinderModel getFlowBinder(long id) throws ProxyException {
        final FlowBinder flowBinder;
        final FlowModel flowModel;
        final List<SubmitterModel> submitterModels = new ArrayList<SubmitterModel>();
        final SinkModel sinkModel;
        try {
            flowBinder = flowStoreServiceConnector.getFlowBinder(id);
            flowModel = FlowModelMapper.toModel(flowStoreServiceConnector.getFlow(flowBinder.getContent().getFlowId()));
            for (long submitterId: flowBinder.getContent().getSubmitterIds()) {
                submitterModels.add(SubmitterModelMapper.toModel(flowStoreServiceConnector.getSubmitter(submitterId)));
            }
            sinkModel = SinkModelMapper.toModel(flowStoreServiceConnector.getSink(flowBinder.getContent().getSinkId()));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e) {
            throw new ProxyException(translateToProxyError(e.getStatusCode()), e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return FlowBinderModelMapper.toModel(flowBinder, flowModel, submitterModels, sinkModel);
    }


    /*
     * Submitters
     */

    @Override
    public SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException {
        Submitter submitter;
        try {
            submitter = flowStoreServiceConnector.createSubmitter(SubmitterModelMapper.toSubmitterContent(model));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e){
            throw new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, e);
        }
        return SubmitterModelMapper.toModel(submitter);
    }

    @Override
    public SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException, IllegalArgumentException {
        Submitter submitter;
        try {
            submitter = flowStoreServiceConnector.updateSubmitter(SubmitterModelMapper.toSubmitterContent(model), model.getId(), model.getVersion());
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e){
            throw new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, e);
        }
        return SubmitterModelMapper.toModel(submitter);
    }

    @Override
    public List<Submitter> findAllSubmitters() throws ProxyException {
        final List<Submitter> result;
        try {
            result = flowStoreServiceConnector.findAllSubmitters();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }

    @Override
    public SubmitterModel getSubmitter(Long id) throws ProxyException {
        final Submitter submitter;
        try {
            submitter = flowStoreServiceConnector.getSubmitter(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return SubmitterModelMapper.toModel(submitter);
    }


    /*
     * Sinks
     */

    @Override
    public SinkModel createSink(SinkModel model) throws NullPointerException, ProxyException {
        Sink sink;
        try {
            sink = flowStoreServiceConnector.createSink(SinkModelMapper.toSinkContent(model));
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e){
            throw new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, e);
        }
        return SinkModelMapper.toModel(sink);
    }

    @Override
    public SinkModel updateSink(SinkModel model) throws NullPointerException, ProxyException {
        Sink sink;
        try {
            sink = flowStoreServiceConnector.updateSink(SinkModelMapper.toSinkContent(model), model.getId(), model.getVersion());
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        } catch (IllegalArgumentException e){
            throw new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, e);
        }
        return SinkModelMapper.toModel(sink);
    }

    @Override
    public List<Sink> findAllSinks() throws ProxyException {
        final List<Sink> result;
        try {
            result = flowStoreServiceConnector.findAllSinks();
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return result;
    }

    @Override
    public SinkModel getSink(Long id) throws ProxyException {
        final Sink sink;
        try {
            sink = flowStoreServiceConnector.getSink(id);
        } catch (FlowStoreServiceConnectorUnexpectedStatusCodeException e){
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (FlowStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
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
            while(isNewFlowComponent && counter < flow.getContent().getComponents().size()){
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


    private ProxyError translateToProxyError(int statusCode)throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(statusCode);

        final ProxyError errorCode;
        switch (status){
            case NOT_FOUND: errorCode = ProxyError.ENTITY_NOT_FOUND;
                break;
            case CONFLICT: errorCode = ProxyError.CONFLICT_ERROR;
                break;
            case NOT_ACCEPTABLE: errorCode = ProxyError.NOT_ACCEPTABLE;
                break;
            case PRECONDITION_FAILED: errorCode = ProxyError.PRECONDITION_FAILED;
                break;
            case BAD_REQUEST: errorCode = ProxyError.BAD_REQUEST;
                break;
            default:
                errorCode = ProxyError.INTERNAL_SERVER_ERROR;
        }
        return errorCode;
    }

}
