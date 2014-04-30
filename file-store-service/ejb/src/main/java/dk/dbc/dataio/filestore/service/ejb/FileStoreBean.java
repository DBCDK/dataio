package dk.dbc.dataio.filestore.service.ejb;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@Stateless
public class FileStoreBean {
    @EJB
    BinaryFileStoreBean binaryFileStore;

    public String addFile(InputStream dataSource) {
        final Path path = Paths.get("file.dat");
        final BinaryFile binaryFile = binaryFileStore.getBinaryFile(path);
        binaryFile.write(dataSource);
        return path.toString();
    }


    public void getFile(String fileId, OutputStream dataDestination) {
        final Path path = Paths.get(fileId);
        final BinaryFile binaryFile = binaryFileStore.getBinaryFile(path);
        binaryFile.read(dataDestination);
    }
}
