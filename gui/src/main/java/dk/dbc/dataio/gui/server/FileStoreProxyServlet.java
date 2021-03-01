package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FileStoreProxy;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class FileStoreProxyServlet extends HttpServlet implements FileStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(FileStoreProxyServlet.class);
    private transient FileStoreProxyImpl fileStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        fileStoreProxy = new FileStoreProxyImpl();
    }

    @Override
    public void removeFile(String fileId) throws ProxyException {
        fileStoreProxy.removeFile(fileId);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final ServletFileUpload upload = new ServletFileUpload();

        try {
            final FileItemIterator iter = upload.getItemIterator(request);
            log.info("iter.hasNext? {}", iter.hasNext());
            while (iter.hasNext()) {
                final FileItemStream item = iter.next();

                if (!item.isFormField()) {
                    try (InputStream is = item.openStream()) {
                        final String fileId = fileStoreProxy.addFile(is);
                        response.setStatus(200);
                        response.getWriter().write(fileId);
                    }
                }
            }
        } catch (FileUploadException | ProxyException e) {
            throw new ServletException("Got FileUploadException when trying to upload file to filestore", e);
        }
    }

    @Override
    public void close() {
        if (fileStoreProxy != null) {
            fileStoreProxy.close();
            fileStoreProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }
}
