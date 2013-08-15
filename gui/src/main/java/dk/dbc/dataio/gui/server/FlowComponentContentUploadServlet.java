package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.tmpengine.EngineGUI;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowComponentContentUploadServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(FlowComponentContentUploadServlet.class);

    private transient FlowStoreProxy flowStoreProxy;

    @Override
    public void init() throws ServletException {
        super.init();

        String flowStoreServiceEndpoint = ServletUtil.getFlowStoreServiceEndpoint();
        flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceEndpoint);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {

        if (!ServletFileUpload.isMultipartContent(req)) {
            String errMsg = "Request did not have multipart content";
            log.error(errMsg);
            throw new ServletException(errMsg);
        } else {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            try {
                // Parse the request
                List<FileItem> items = upload.parseRequest(req);
                for (FileItem item : items) {
                    log.info(String.format("item: FieldName: [%s]", item.getFieldName()));
                }

            } catch (Exception e) {
                log.error("Exception caught", e);
                throw new ServletException(e);
            }
            
        }
    }
}
