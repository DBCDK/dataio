package dk.dbc.dataio.commons.conversion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Marc8Charset;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConversionParamTest {
    @Test
    public void getEncoding() {
        final ConversionParam param = new ConversionParam();
        assertThat("null", param.getEncoding().orElse(null),
                is(nullValue()));
        assertThat("danmarc2", param.withEncoding("danmarc2").getEncoding().orElse(null),
                is(new DanMarc2Charset()));
        assertThat("latin1", param.withEncoding("latin1").getEncoding().orElse(null),
                is(new DanMarc2Charset()));
        assertThat("marc8", param.withEncoding("marc8").getEncoding().orElse(null),
                is(new Marc8Charset()));
        assertThat("utf8", param.withEncoding("utf8").getEncoding().orElse(null),
                is(StandardCharsets.UTF_8));
    }

    @Test
    public void getEncoding_unknownEncoding() {
        final ConversionParam param = new ConversionParam();
        assertThat(() -> param.withEncoding("unknown").getEncoding(), isThrowing(ConversionException.class));
    }

    @Test
    public void unmarshalling() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        final String json = "{\"encoding\": \"utf8\", \"magicNumber\": 42, \"submitter\": 123456}";
        final ConversionParam param = mapper.readValue(json, ConversionParam.class);
        assertThat("encoding", param.getEncoding().orElse(null), is(StandardCharsets.UTF_8));
        assertThat("submitter", param.getSubmitter().orElse(null), is(123456));
    }
}
