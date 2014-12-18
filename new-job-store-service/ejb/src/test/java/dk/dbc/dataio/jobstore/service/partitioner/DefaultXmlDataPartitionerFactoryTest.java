package dk.dbc.dataio.jobstore.service.partitioner;

import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class DefaultXmlDataPartitionerFactoryTest {
    private static final InputStream INPUT_STREAM = mock(InputStream.class);
    private static final String ENCODING = "UTF-8";
    
    @Test
    public void constructor_returnsNewInstance() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        assertThat(factory, is(notNullValue()));
    }

    @Test
    public void createDataPartitioner_inputStreamArgIsNull_throws() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        try {
            factory.createDataPartitioner(null, ENCODING);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void createDataPartitioner_encodingArgIsNull_throws() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        try {
            factory.createDataPartitioner(INPUT_STREAM, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void createDataPartitioner_encodingArgIsEmpty_throws() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        try {
            factory.createDataPartitioner(INPUT_STREAM, "");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void createDataPartitioner_allArgsAreValid_returnsNewDataPartitioner() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        assertThat(factory.createDataPartitioner(INPUT_STREAM, ENCODING), is(notNullValue()));
    }
}