package dk.dbc.dataio.harvester.utils.datafileverifier;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

public abstract class JSonExpectation extends Expectation {
    private static final ObjectMapper mapper = new ObjectMapper();
    public abstract void verify(String json);

    @Override
    public void verify(byte[] data) {
        verify(new String(data, StandardCharsets.UTF_8));
    }
}
