package dk.dbc.dataio.filestore.service.ejb;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.filestore.service.entity.FileAttributes;
import jakarta.ejb.EJBException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileStoreBeanTest {
    private final EntityManager entityManager = mock(EntityManager.class);
    private final BinaryFileStoreBean binaryFileStoreBean = mock(BinaryFileStoreBean.class);
    private final BinaryFile binaryFile = mock(BinaryFile.class);
    private final FileAttributes fileAttributes = mock(FileAttributes.class);
    private final InputStream inputStream = mock(InputStream.class);
    private final OutputStream outputStream = mock(OutputStream.class);
    private final Path path = mock(Path.class);
    private final String fileId = "42";

    @Test
    public void addFile_dataSourceArgIsNull_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(NullPointerException.class, () -> fileStoreBean.addFile(null));
    }

    @Test
    public void addFile_fileIsAdded_returnsFileId() {
        when(binaryFileStoreBean.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(entityManager.merge(any(FileAttributes.class))).thenReturn(fileAttributes);
        when(fileAttributes.getId()).thenReturn(Long.valueOf(fileId));
        when(binaryFile.exists()).thenReturn(false);

        FileStoreBean fileStoreBean = newFileStoreBeanInstance();

        assertThat(fileStoreBean.addFile(inputStream), is(fileId));
        verify(fileAttributes).setByteSize(anyLong());
        verify(binaryFile, times(0)).delete();
    }

    @Test
    public void addFile_danglingFileExists_overwritesFile() {
        when(binaryFileStoreBean.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(entityManager.merge(any(FileAttributes.class))).thenReturn(fileAttributes);
        when(fileAttributes.getId()).thenReturn(Long.valueOf(fileId));
        when(binaryFile.exists()).thenReturn(true);

        FileStoreBean fileStoreBean = newFileStoreBeanInstance();

        assertThat(fileStoreBean.addFile(inputStream), is(fileId));
        verify(fileAttributes).setByteSize(anyLong());
        verify(binaryFile, times(1)).delete();
    }

    @Test
    public void addMetadata() {
        FileAttributes fileAttributes = new FileAttributes(new Date(),
                Paths.get("path"));
        when(entityManager.find(eq(FileAttributes.class), anyLong()))
                .thenReturn(fileAttributes);
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.addMetaData("123456", "{\"meta\": \"data\"}");
        assertThat("added metadata", fileAttributes.getMetadata(),
                is("{\"meta\": \"data\"}"));
    }

    @Test
    public void addMetadata_argumentIdIsNull_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(NullPointerException.class, () -> fileStoreBean.addMetaData(null, "metadata"));
    }

    @Test
    public void getFile_fileIdArgIsNull_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(NullPointerException.class, () -> fileStoreBean.getFile(null, outputStream, false));
    }

    @Test
    public void getFile_fileIdArgIsEmpty_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(IllegalArgumentException.class, () -> fileStoreBean.getFile("", outputStream, false));
    }

    @Test
    public void getFile_dataDestinationArgIsNull_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(NullPointerException.class, () -> fileStoreBean.getFile(fileId, null, false));
    }

    @Test
    public void getFile_fileAttributesCanNotBeFound_throws() {
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(null);
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(EJBException.class, () -> fileStoreBean.getFile(fileId, outputStream, false));
    }

    @Test
    public void deleteFile_fileIdArgIsNull_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(NullPointerException.class, () -> fileStoreBean.deleteFile(null));
    }

    @Test
    public void deleteFile_fileIdArgIsEmpty_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(IllegalArgumentException.class, () -> fileStoreBean.deleteFile(""));
    }

    @Test
    public void deleteFile_fileAttributesCanNotBeFound_throws() {
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(null);
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(EJBException.class, () -> fileStoreBean.deleteFile(fileId));
    }

    @Test
    public void deleteFile() {
        when(binaryFileStoreBean.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(entityManager.find(eq(FileAttributes.class), any(Long.class))).thenReturn(fileAttributes);
        when(fileAttributes.getId()).thenReturn(Long.valueOf(fileId));
        when(fileAttributes.getLocation()).thenReturn(path);
        when(path.resolve(fileId)).thenReturn(path);

        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.deleteFile(fileId);
        verify(binaryFile).delete();
        verify(entityManager).remove(fileAttributes);
    }

    @Test
    public void fileExists_fileIdArgIsNull_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(NullPointerException.class, () -> fileStoreBean.fileExists(null));
    }

    @Test
    public void fileExists_fileIdArgIsEmpty_returnsFalse() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.fileExists(""), is(false));
    }

    @Test
    public void fileExists_fileIdArgIsNaN_returnsFalse() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.fileExists("not-a-number"), is(false));
    }

    @Test
    public void fileExists_fileDoesNotExist_returnsFalse() {
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(null);
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.fileExists(fileId), is(false));
    }

    @Test
    public void fileExists_fileDoesExist_returnsTrue() {
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(fileAttributes);
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.fileExists(fileId), is(true));
    }

    @Test
    public void getByteSize_fileIdIsEmpty_throws() throws IllegalArgumentException {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(IllegalArgumentException.class, () -> fileStoreBean.getByteSize("", true), "getByteSize: Invalid file ID was not detected");
    }

    @Test
    public void getByteSize_fileIdIsNull_throws() throws IllegalArgumentException {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(NullPointerException.class, () -> fileStoreBean.getByteSize(null, true), "getByteSize: Invalid file ID was not detected");
    }

    @Test
    public void getByteSize_fileIdNotANumber_throws() {
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(IllegalArgumentException.class, () -> fileStoreBean.getByteSize("notANumber", true), "getByteSize: Invalid file ID was not detected");
    }

    @Test
    public void getByteSize_filAttributesNotFound_throws() throws IllegalArgumentException {
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(null);
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThrows(EJBException.class, () -> fileStoreBean.getByteSize(fileId, true));
    }

    @Test
    public void getByteSize_fileAttributesExist_returnsByteSize() throws IllegalArgumentException {
        FileAttributes fileAttributes = new FileAttributes(new Date(), path);
        when(entityManager.find(FileAttributes.class, Long.parseLong(fileId))).thenReturn(fileAttributes);
        FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.getByteSize(fileId, false), is(0L));
    }

    private FileStoreBean newFileStoreBeanInstance() {
        FileStoreBean fileStoreBean = new FileStoreBean();
        fileStoreBean.entityManager = entityManager;
        fileStoreBean.binaryFileStore = binaryFileStoreBean;
        return fileStoreBean;
    }
}
