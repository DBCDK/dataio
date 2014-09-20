package dk.dbc.dataio.commons.utils.httpclient;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PathBuilderTest {
    private static final String PATH_TEMPLATE = "{id1}/test/{id2}/{id1}/test/{id2}";

    @Test(expected = NullPointerException.class)
    public void constructor_pathTemplateArgIsNull_throws() {
        new PathBuilder(null);
    }

    @Test
    public void pathBuilder_noValuesBound_returnsPathTemplateUnchanged() {
        final PathBuilder pathBuilder = new PathBuilder(PATH_TEMPLATE);
        assertThat(pathBuilder.build(), is(PATH_TEMPLATE.split(PathBuilder.PATH_SEPARATOR)));
    }

    @Test
    public void pathBuilder_whenValuesMatchPathVariables_returnsInterpolatedPath() {
        final String expectedPath = "val1/test/val2/val1/test/val2";
        final PathBuilder pathBuilder = new PathBuilder(PATH_TEMPLATE);
        pathBuilder.bind("id1", "val1");
        pathBuilder.bind("id2", "val2");
        assertThat(pathBuilder.build(), is(expectedPath.split(PathBuilder.PATH_SEPARATOR)));
    }
}