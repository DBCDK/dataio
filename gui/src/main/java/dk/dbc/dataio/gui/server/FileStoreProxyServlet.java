package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FileStoreProxy;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileStoreProxyServlet extends HttpServlet implements FileStoreProxy {
    private transient FileStoreProxy fileStoreProxy = null;

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
    public String addFile(byte[] dataSource) throws ProxyException {
        return fileStoreProxy.addFile(dataSource);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        final ServletFileUpload upload = new ServletFileUpload();

        try {
            final FileItemIterator iter = upload.getItemIterator(request);
            while (iter.hasNext()) {
                final FileItemStream item = iter.next();

                if (!item.isFormField()) {
                    try (InputStream is = item.openStream();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }

                        final String payload = stringBuilder.toString();

                        if (!payload.isEmpty()) {
                            final String fileId;
                            try {
                                fileId = fileStoreProxy.addFile(payload.getBytes(StandardCharsets.UTF_8));
                                response.setStatus(200);
                                response.getWriter().write(fileId);
                            } catch (ProxyException e) {
                                throw new ServletException("Exception when calling FileStoreProxy", e);
                            }
                        }

                        break;
                    }
                }
            }
        } catch (FileUploadException e) {
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
