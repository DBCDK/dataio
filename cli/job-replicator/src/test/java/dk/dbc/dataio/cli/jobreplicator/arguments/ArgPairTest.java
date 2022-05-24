package dk.dbc.dataio.cli.jobreplicator.arguments;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ArgPairTest {
    @Test
    public void fromString() throws ArgParseException {
        String input = "key=value";
        ArgPair argPair = ArgPair.fromString(input);
        assertThat("key", argPair.getKey(), is("key"));
        assertThat("value", argPair.getValue(), is("value"));
    }

    @Test(expected = ArgParseException.class)
    public void fromString_tooManyValues() throws ArgParseException{
        String input = "key=value=what";
        ArgPair.fromString(input);
    }

    @Test(expected = ArgParseException.class)
    public void fromString_tooFewValues() throws ArgParseException {
        String input = "key";
        ArgPair.fromString(input);
    }
}
