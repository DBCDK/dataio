package dk.dbc.dataio.commons.utils.httpclient;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class HttpClientTest {
    private final String path = "{id1}/test/{id2}";
    private final Map<String, String> pathVariables = new HashMap<>();
    {
        pathVariables.put("id1", "val1");
        pathVariables.put("id2", "val2");
    }

    @Test
    public void interpolatePathVariables_pathArgIsNull_returnsNull() {
        assertThat(HttpClient.interpolatePathVariables(null, pathVariables), is(nullValue()));
    }

    @Test
    public void interpolatePathVariables_valuesArgIsNull_returnsPathUnchanged() {
        assertThat(HttpClient.interpolatePathVariables(path, null), is(path));
    }

    @Test
    public void interpolatePathVariables_whenValuesMatchesPathElements_returnsInterpolatedPath() {
        final String expectedPath = "val1/test/val2";
        assertThat(HttpClient.interpolatePathVariables(path, pathVariables), is(expectedPath));
    }

    @Test
    public void interpolatePathVariables_whenValueMatchesPathElementMultipleTimes_returnsInterpolatedPath() {
        final String path = "{id1}/test/{id2}/{id1}/test/{id2}";
        final String expectedPath = "val1/test/val2/val1/test/val2";
        assertThat(HttpClient.interpolatePathVariables(path, pathVariables), is(expectedPath));
    }

    @Test
    public void interpolatePathVariables_whenNoValuesMatchesPathElements_returnsPathUnchanged() {
        assertThat(HttpClient.interpolatePathVariables(path, new HashMap<String, String>()), is(path));
    }
}
