package dk.dbc.dataio.gui.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import dk.dbc.dataio.engine.Engine;
import dk.dbc.dataio.engine.FlowComponentContent;
import dk.dbc.dataio.engine.JavaScript;
import dk.dbc.dataio.gui.client.views.FlowComponentCreateViewImpl;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowComponentContentUploadServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(FlowComponentContentUploadServlet.class);
    private WebResource webResource;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String flowStoreServiceEndpoint = ServletUtil.getFlowStoreServiceEndpoint();
            webResource = setupWebResource(flowStoreServiceEndpoint);
        } catch (Exception ex) {
            log.error("Exception caught while initializing: ", ex);
        }
    }

    private WebResource setupWebResource(String flowStoreServiceEndpoint) {
        final ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        // force client to use Jackson JAX-RS provider (one in org.codehaus.jackson.jaxrs)
        clientConfig.getClasses().add(JacksonJsonProvider.class);
        final Client httpClient = Client.create(clientConfig);

        return httpClient.resource(flowStoreServiceEndpoint);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        if (!ServletFileUpload.isMultipartContent(req)) {
            String errMsg = "Request did not have multipart content";
            log.error(errMsg);
            throw new ServletException(errMsg);
        } else {
            FileItemFactory factory = new DiskFileItemFactory(); // Note: MemoryFileItemUpload???
            ServletFileUpload upload = new ServletFileUpload(factory); // Factory???

            String componentName = null;
            String javascriptString = null;
            String invocationMethod = null;
            try {
                List<FileItem> items = upload.parseRequest(req);
                for (FileItem item : items) {
                    String fieldName = item.getFieldName();
                    if (fieldName.equals(FlowComponentCreateViewImpl.FORM_FIELD_COMPONENT_NAME)) {
                        componentName = getItemAsCharacterEncodedString(item);
                        log.info("Found ComponentName field: " + componentName);
                    } else if (fieldName.equals(FlowComponentCreateViewImpl.FORM_FIELD_INVOCATION_METHOD)) {
                        invocationMethod = getItemAsCharacterEncodedString(item);
                        log.info("Found InvocationMethod field: " + invocationMethod);
                    } else if (fieldName.equals(FlowComponentCreateViewImpl.FORM_FIELD_JAVASCRIPT_FILE_UPLOAD)) {
                        javascriptString = getItemAsCharacterEncodedString(item);
                        log.info("Found Javascript field (content as string): " + javascriptString);
                    } else {
                        log.warn("Unknown field: [" + fieldName + "]");
                    }
                }
            } catch (Exception e) {
                log.error("Exception caught", e);
                throw new ServletException(e);
            }

            JavaScript javascript = new JavaScript(Engine.base64encode(javascriptString), "");
            FlowComponentContent flowComponentContent = new FlowComponentContent(componentName, Arrays.asList(javascript), invocationMethod);
            createFlowComponent(flowComponentContent);
        }
    }

    private String getItemAsCharacterEncodedString(FileItem item) throws UnsupportedEncodingException {
        return item.getString("UTF-8");
    }

    public void createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, IllegalStateException {
        final ClientResponse response = webResource.path("components").type(MediaType.APPLICATION_JSON).post(ClientResponse.class, flowComponentContent);
        if (response.getClientResponseStatus() != ClientResponse.Status.CREATED) {
            throw new IllegalStateException(response.getEntity(String.class));
        }
    }
}
