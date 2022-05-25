package dk.dbc.dataio.gatekeeper.transfile;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class TransFileLineTest {
    @Test(expected = NullPointerException.class)
    public void constructor_lineArgIsNull_throws() {
        new TransFile.Line((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void copyConstructor_lineArgIsNull_throws() {
        new TransFile.Line((TransFile.Line) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_lineArgIsEmpty_throws() {
        new TransFile.Line("   ");
    }

    @Test
    public void constructor_lineContainsNoKeyValuePairs_noFieldsExtracted() {
        final String lineValue = "notKeyValuePair";
        final TransFile.Line line = new TransFile.Line(lineValue);
        assertThat("Line value", line.getLine(), is(lineValue));
        assertThat("Number of fields found", line.getFieldNames().size(), is(0));
    }

    @Test
    public void constructor_lineContainsKeyValuePairs_fieldsExtracted() {
        final String lineValue = "b=base1,f=file1,c=latin-1,b=base2";
        final TransFile.Line line = new TransFile.Line(lineValue);
        assertThat("Line value", line.getLine(), is(lineValue));
        assertThat("Fields found", line.getFieldNames(), containsInAnyOrder("b", "f", "c"));
    }

    @Test
    public void getField_whenFieldExists_returnsValue() {
        final String lineValue = "b=base1, f=file1,\tc = latin-1";
        final TransFile.Line line = new TransFile.Line(lineValue);
        assertThat("Field b", line.getField("b"), is("base1"));
        assertThat("Field f", line.getField("f"), is("file1"));
        assertThat("Field c", line.getField("c"), is("latin-1"));
    }

    @Test
    public void getField_whenFieldNotExists_returnsNull() {
        final TransFile.Line line = new TransFile.Line("b=base");
        assertThat("Non existing field", line.getField("f"), is(nullValue()));
    }

    @Test
    public void getLine_whenLineIsModified_returnsModifiedLine() {
        final String content = "f=file1,b=base1,m=mail@company.com";
        final TransFile.Line line = new TransFile.Line(content);
        line.setField("b", "foo");
        line.setField("m", "");
        assertThat(line.getLine(), is("b=foo,f=file1,m="));
    }
}
