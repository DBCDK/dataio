package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import java.nio.charset.Charset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ItemDataTest {

    @Test(expected = NullPointerException.class)
    public void constructor_dataArgIsNull_throws() {
        new ItemData(null, Charset.forName("UTF-8"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_emptyData_throws() {
        new ItemData("", Charset.forName("UTF-8"));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_encodingArgIdNull_throws() {
        new ItemData("test", null);
    }

    @Test
    public void setItemData_inputIsValid_ItemDataCreated() {
        final String DATA = "this is some test data";
        ItemData itemData = new ItemData(DATA, Charset.forName("UTF-8"));
        assertThat("itemData.data", itemData.getData(), is(DATA));
        assertThat("itemData.encoding", itemData.getEncoding(), is(Charset.forName("UTF-8")));
    }
}
