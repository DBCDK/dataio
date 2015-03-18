package dk.dbc.dataio.filestore.service.ejb;

import com.sun.media.sound.InvalidDataException;
import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.filestore.service.entity.FileAttributes;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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

    @Test(expected = NullPointerException.class)
    public void addFile_dataSourceArgIsNull_throws() {
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.addFile(null);
    }

    @Test
    public void addFile_fileIsAdded_returnsFileId() {
        when(binaryFileStoreBean.getBinaryFile(any(Path.class))).thenReturn(binaryFile);
        when(entityManager.merge(any(FileAttributes.class))).thenReturn(fileAttributes);
        when(fileAttributes.getId()).thenReturn(Long.valueOf(fileId));

        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();

        assertThat(fileStoreBean.addFile(inputStream), is(fileId));
        verify(fileAttributes).setByteSize(anyLong());
    }

    @Test(expected = NullPointerException.class)
    public void getFile_fileIdArgIsNull_throws() {
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.getFile(null, outputStream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getFile_fileIdArgIsEmpty_throws() {
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.getFile("", outputStream);
    }

    @Test(expected = NullPointerException.class)
    public void getFile_dataDestinationArgIsNull_throws() {
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.getFile(fileId, null);
    }

    @Test(expected = EJBException.class)
    public void getFile_fileAttributesCanNotBeFound_throws() {
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(null);
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.getFile(fileId, outputStream);
    }

    @Test(expected = NullPointerException.class)
    public void fileExists_fileIdArgIsNull_throws() {
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.fileExists(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fileExists_fileIdArgIsEmpty_throws() {
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.fileExists("");
    }

    @Test
    public void fileExists_fileIdArgIsNaN_returnsFalse() {
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.fileExists("not-a-number"), is(false));
    }

    @Test
    public void fileExists_fileDoesNotExist_returnsFalse() {
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(null);
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.fileExists(fileId), is(false));
    }

    @Test
    public void fileExists_fileDoesExist_returnsTrue() {
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(fileAttributes);
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.fileExists(fileId), is(true));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getByteSize_fileIdIsEmpty_throws() throws InvalidDataException{
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.getByteSize("");
        fail("getByteSize: Invalid file ID was not detected");
    }

    @Test (expected = NullPointerException.class)
    public void getByteSize_fileIdIsNull_throws() throws InvalidDataException{
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.getByteSize(null);
        fail("getByteSize: Invalid file ID was not detected");
    }

    @Test
    public void getByteSize_fileIdNotANumber_throws() {
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        try {
            fileStoreBean.getByteSize("notANumber");
            fail("getByteSize: Invalid file ID was not detected");
        } catch(InvalidDataException e) {}
    }

    @Test (expected = EJBException.class)
    public void getByteSize_filAttributesNotFound_throws() throws InvalidDataException{
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(null);
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        fileStoreBean.getByteSize(fileId);
    }

    @Test
    public void getByteSize_fileAttributesExist_returnsByteSize() throws InvalidDataException{
        FileAttributes fileAttributes = new FileAttributes(new Date(), path);
        when(entityManager.find(eq(FileAttributes.class), eq(Long.valueOf(fileId)))).thenReturn(fileAttributes);
        final FileStoreBean fileStoreBean = newFileStoreBeanInstance();
        assertThat(fileStoreBean.getByteSize(fileId), is(0L));
    }

    private FileStoreBean newFileStoreBeanInstance() {
        final FileStoreBean fileStoreBean = new FileStoreBean();
        fileStoreBean.entityManager = entityManager;
        fileStoreBean.binaryFileStore = binaryFileStoreBean;
        return fileStoreBean;
    }

}