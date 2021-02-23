package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FileStoreProxy;

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
        BufferedReader bufferedReader = null;
        try {
            try (InputStream inputStream = request.getInputStream()) {
                if (inputStream != null) {
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    char[] charBuffer = new char[128];
                    int bytesRead = -1;
                    while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                        stringBuilder.append(charBuffer, 0, bytesRead);
                    }
                }
            }
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
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
        } else {
            response.setStatus(403);
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
