package dk.dbc.dataio.commons.utils.httpclient;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class HttpClientTest {
    private final String path = "{id1}/test/{id2}";
    private final Map<String, String> pathValues = new HashMap<>();
    {
        pathValues.put("id1", "val1");
        pathValues.put("id2", "val2");
    }

    @Test
    public void interpolatePathValues_pathArgIsNull_returnsNull() {
        assertThat(HttpClient.interpolatePathValues(null, pathValues), is(nullValue()));
    }

    @Test
    public void interpolatePathValues_valuesArgIsNull_returnsPathUnchanged() {
        assertThat(HttpClient.interpolatePathValues(path, null), is(path));
    }

    @Test
    public void interpolatePathValues_whenValuesMatchesPathElements_returnsInterpolatedPath() {
        final String expectedPath = "val1/test/val2";
        assertThat(HttpClient.interpolatePathValues(path, pathValues), is(expectedPath));
    }

    @Test
    public void interpolatePathValues_whenValueMatchesPathElementMultipleTimes_returnsInterpolatedPath() {
        final String path = "{id1}/test/{id2}/{id1}/test/{id2}";
        final String expectedPath = "val1/test/val2/val1/test/val2";
        assertThat(HttpClient.interpolatePathValues(path, pathValues), is(expectedPath));
    }

    @Test
    public void interpolatePathValues_whenNoValuesMatchesPathElements_returnsPathUnchanged() {
        assertThat(HttpClient.interpolatePathValues(path, new HashMap<String, String>()), is(path));
    }
}
