
package dk.dbc.dataio.filestore.service.connector.ejb;


import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;

import javax.ws.rs.ProcessingException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ja7 on 1/3/17.
 */
public class TestFileStoreServiceConnector extends FileStoreServiceConnector {


   static Map<String, String> files = new HashMap();
   static Map<String, Long> fileLengthOverWrite = new HashMap<>();


    public static void updateFileContent( String fileId, String fileContent ) {
        files.put(fileId, fileContent);
    }
    public static void updateFileContentLength( String fileId, Long newSize ) {
        fileLengthOverWrite.put(fileId, newSize);
    }


    public TestFileStoreServiceConnector() throws NullPointerException, IllegalArgumentException {
        super(HttpClient.newClient(), "baseUrl");
        
    }

    public static void resetTestData() {
        files= new HashMap<>();
        fileLengthOverWrite = new HashMap<>();
    }

    @Override
    public String addFile(InputStream is) throws NullPointerException, ProcessingException, FileStoreServiceConnectorException {
        throw new ProcessingException("Test Connector unable to add file ");
    }

    @Override
    public InputStream getFile(String fileId) throws NullPointerException, IllegalArgumentException, ProcessingException, FileStoreServiceConnectorException {
        if ("404".equals(fileId)) {
            throw new FileStoreServiceConnectorUnexpectedStatusCodeException("not found", 404);
        }

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
            if( fileLengthOverWrite.containsKey( fileId)) return fileLengthOverWrite.get(fileId );
            
            return files.get(fileId).getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
