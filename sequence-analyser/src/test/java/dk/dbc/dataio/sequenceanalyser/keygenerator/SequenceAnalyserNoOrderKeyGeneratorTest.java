package dk.dbc.dataio.sequenceanalyser.keygenerator;

import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SequenceAnalyserNoOrderKeyGeneratorTest {
    private final SequenceAnalyserKeyGenerator keyGenerator = new SequenceAnalyserNoOrderKeyGenerator();

    @Test
    public void generateKeys_returnsEmptyKeySet() {
        final Set<String> keys = keyGenerator.generateKeys(Arrays.asList("data1", "data2"));
        assertThat(keys, is(notNullValue()));
        assertThat(keys.size(), is(0));
    }
}