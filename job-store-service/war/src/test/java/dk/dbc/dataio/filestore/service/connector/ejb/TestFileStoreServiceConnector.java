
package dk.dbc.dataio.filestore.service.connector.ejb;


import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;

import javax.ws.rs.ProcessingException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ja7 on 1/3/17.
 */
public class TestFileStoreServiceConnector extends dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector {


    static Map<String, String> files = new HashMap();


    public static void updateFileContent( String fileId, String fileContent ) {
        files.put(fileId, fileContent);
    }


    public TestFileStoreServiceConnector() throws NullPointerException, IllegalArgumentException {
        super(HttpClient.newClient(), "baseUrl");
        
    }

    @Override
    public String addFile(InputStream is) throws NullPointerException, ProcessingException, FileStoreServiceConnectorException {
        throw new ProcessingException("Test Connector unable to add file ");
    }

    @Override
    public InputStream getFile(String fileId) throws NullPointerException, IllegalArgumentException, ProcessingException, FileStoreServiceConnectorException {

        ByteArrayInputStream  res=null;
        try {
            res=new ByteArrayInputStream(files.get(fileId).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return res;
    }

    @Override
    public void deleteFile(String fileId) throws NullPointerException, IllegalArgumentException, FileStoreServiceConnectorException {
        throw new ProcessingException("Test connector unable to delete file");
    }

    @Override
    public long getByteSize(String fileId) throws NullPointerException, IllegalArgumentException, FileStoreServiceConnectorException {
        try {
            return files.get(fileId).getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
