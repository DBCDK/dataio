
package dk.dbc.dataio.gui.client.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.gui.client.util.Format.inBracketsPairString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Format unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class FormatTest {

    @Test(expected = NullPointerException.class)
    public void commaSeparate_nullInput_nullPointerException() {
        Format.commaSeparate(null);
    }

    @Test
    public void commaSeparate_emptyList_emptyResultString() {
        List<String> empty = new ArrayList<String>();
        String result = Format.commaSeparate(empty);
        assertThat(result, is(""));
    }

    @Test
    public void commaSeparate_validList_validResultString() {
        List<String> animals = Arrays.asList("Monkey", "Elephant", "Bird");
        String result = Format.commaSeparate(animals);
        assertThat(result, is("Monkey, Elephant, Bird"));
    }

    @Test
    public void submitterPairString_idNull_validPairString() {
        String result = inBracketsPairString(null, "name");
        assertThat(result, is("null (name)"));
    }

    @Test
    public void submitterPairString_nameNull_validPairString() {
        String result = inBracketsPairString("27", null);
        assertThat(result, is("27 (null)"));
    }

    @Test
    public void submitterPairString_validInput_validPairString() {
        String result = inBracketsPairString("27", "name");
        assertThat(result, is("27 (name)"));
    }

}
