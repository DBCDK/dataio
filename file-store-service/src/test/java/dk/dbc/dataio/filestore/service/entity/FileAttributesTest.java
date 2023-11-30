package dk.dbc.dataio.filestore.service.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileAttributesTest {
    private final Date creationTime = new Date();
    private final Path path = Paths.get("path/to/file");

    @Test
    public void constructor_creationTimeArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FileAttributes(null, path));
    }

    @Test
    public void constructor_pathArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FileAttributes(creationTime, null));
    }

    @Test
    public void constructor_pathArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FileAttributes(creationTime, Paths.get("")));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        assertThat(fileAttributes, is(notNullValue()));
        assertThat(fileAttributes.getCreationTime(), is(creationTime));
        assertThat(fileAttributes.getLocation(), is(path));
        assertThat(fileAttributes.getByteSize(), is(0L));
    }

    @Test
    public void constructor_creationTime_isDefensivelyCopied() {
        Date dateToBeModified = new Date();
        FileAttributes fileAttributes = new FileAttributes(dateToBeModified, path);
        dateToBeModified.setTime(42);
        assertThat(fileAttributes.getCreationTime(), is(not(dateToBeModified)));
    }

    @Test
    public void getCreationTime_returnValue_isDefensivelyCopied() {
        FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        Date creationTimeToBeModified = fileAttributes.getCreationTime();
        creationTimeToBeModified.setTime(42);
        assertThat(fileAttributes.getCreationTime(), is(not(creationTimeToBeModified)));
        assertThat(fileAttributes.getCreationTime(), is(creationTime));
    }

    @Test
    public void setBytesRead_bytesReadIsSet_bytesReadIsReturnedWithUpdatedValue() {
        FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        assertThat(fileAttributes.getByteSize(), is(0L));
        fileAttributes.setByteSize(42);
        assertThat(fileAttributes.getByteSize(), is(42L));
    }
}
