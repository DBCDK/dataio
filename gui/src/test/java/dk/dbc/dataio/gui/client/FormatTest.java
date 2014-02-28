
package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.gui.client.util.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

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
        List<String> empty = Arrays.asList("Monkey", "Elephant", "Bird");
        String result = Format.commaSeparate(empty);
        assertThat(result, is("Monkey, Elephant, Bird"));
    }

}
