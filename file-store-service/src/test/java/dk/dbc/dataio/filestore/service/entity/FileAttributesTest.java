package dk.dbc.dataio.filestore.service.entity;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileAttributesTest {
    private final Date creationTime = new Date();
    private final Path path = Paths.get("path/to/file");

    @Test(expected = NullPointerException.class)
    public void constructor_creationTimeArgIsNull_throws() {
        new FileAttributes(null, path);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_pathArgIsNull_throws() {
        new FileAttributes(creationTime, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_pathArgIsEmpty_throws() {
        new FileAttributes(creationTime, Paths.get(""));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        assertThat(fileAttributes, is(notNullValue()));
        assertThat(fileAttributes.getCreationTime(), is(creationTime));
        assertThat(fileAttributes.getLocation(), is(path));
        assertThat(fileAttributes.getByteSize(), is(0L));
    }

    @Test
    public void constructor_creationTime_isDefensivelyCopied() {
        Date dateToBeModified = new Date();
        final FileAttributes fileAttributes = new FileAttributes(dateToBeModified, path);
        dateToBeModified.setTime(42);
        assertThat(fileAttributes.getCreationTime(), is(not(dateToBeModified)));
    }

    @Test
    public void getCreationTime_returnValue_isDefensivelyCopied() {
        final FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        final Date creationTimeToBeModified = fileAttributes.getCreationTime();
        creationTimeToBeModified.setTime(42);
        assertThat(fileAttributes.getCreationTime(), is(not(creationTimeToBeModified)));
        assertThat(fileAttributes.getCreationTime(), is(creationTime));
    }

    @Test
    public void setBytesRead_bytesReadIsSet_bytesReadIsReturnedWithUpdatedValue() {
        final FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        assertThat(fileAttributes.getByteSize(), is(0L));
        fileAttributes.setByteSize(42);
        assertThat(fileAttributes.getByteSize(), is(42L));
    }
}
