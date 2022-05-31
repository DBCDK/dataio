package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * FlowComponentContent unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class FlowComponentContentTest {
    private static final String NAME = "name";
    private static final String SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT = "svnProjectForInvocationJavascript";
    private static final long SVN_REVISION = 1L;
    private static final String JAVA_SCRIPT_NAME = "invocationJavascriptName";
    private static final String INVOCATION_METHOD = "method";
    private static final List<JavaScript> JAVASCRIPTS = Collections.singletonList(JavaScriptTest.newJavaScriptInstance());
    private static final String REQUIRE_CACHE = "";
    private static final String DESCRIPTION = "description";
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test(expected = NullPointerException.class)
    public void constructor_nameArgIsNull_throws() {
        new FlowComponentContent(null, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, REQUIRE_CACHE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nameArgIsEmpty_throws() {
        new FlowComponentContent("", SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, REQUIRE_CACHE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_svnProjectArgIsNull_throws() {
        new FlowComponentContent(NAME, null, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, REQUIRE_CACHE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_svnProjectArgIsEmpty_throws() {
        new FlowComponentContent(NAME, "", SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, REQUIRE_CACHE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_svnRevisionArgIsZero_throws() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, 0, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, REQUIRE_CACHE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_invocationJavascriptNameArgIsNull_throws() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, null, JAVASCRIPTS, INVOCATION_METHOD, REQUIRE_CACHE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_invocationJavascriptNameArgIsEmpty_throws() {
        new FlowComponentContent("", SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, "", JAVASCRIPTS, INVOCATION_METHOD, REQUIRE_CACHE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_javascriptsArgIsNull_throws() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, null, INVOCATION_METHOD, REQUIRE_CACHE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_invocationMethodArgIsNull_throws() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, null, REQUIRE_CACHE);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowComponentContent instance = new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, DESCRIPTION, REQUIRE_CACHE);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_invocationMethodArgIsEmpty_returnsNewInstance() {
        final FlowComponentContent instance = new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, "", DESCRIPTION, REQUIRE_CACHE);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void constructor_javascriptsArgIsEmpty_returnsNewInstance() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, new ArrayList<JavaScript>(0), INVOCATION_METHOD, DESCRIPTION, REQUIRE_CACHE);
    }

    @Test
    public void constructor_requireCacheArgIsEmpty_returnsNewInstance() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, DESCRIPTION, "");
    }

    @Test
    public void constructor_requireCacheArgIsNull_returnsNewInstance() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, DESCRIPTION, null);
    }

    @Test
    public void constructor_descriptionArgIsEmpty_returnsNewInstance() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, "", REQUIRE_CACHE);
    }

    @Test
    public void constructor_descriptionArgIsNull_returnsNewInstance() {
        new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, null, REQUIRE_CACHE);
    }

    @Test
    public void verify_defensiveCopyingOfJavascriptsList() {
        final List<JavaScript> javaScripts = new ArrayList<>();
        javaScripts.add(JavaScriptTest.newJavaScriptInstance());
        final FlowComponentContent instance = new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, javaScripts, INVOCATION_METHOD, DESCRIPTION, REQUIRE_CACHE);
        assertThat(instance.getJavascripts().size(), is(1));
        javaScripts.add(null);
        final List<JavaScript> returnedJavascripts = instance.getJavascripts();
        assertThat(returnedJavascripts.size(), is(1));
        returnedJavascripts.add(null);
        assertThat(instance.getJavascripts().size(), is(1));
    }

    @Test
    public void testJsonUnmarchallOld() throws Exception {
        String data = "{ \"invocationJavascriptName\": \"trunk/tracerBulletXmlDom.js\",\n" +
                "        \"invocationMethod\": \"tracerbullet_xmldom\",\n" +
                "        \"javascripts\": [\n" +
                "            {\n" +
                "                \"javascript\": \"dXNlKCAiTG9nIiApOwp1c2UoICJYTUxET00iKTsKCgpmdW5jdGlvbiB0cmFjZXJidWxsZXRfeG1sZG9tKCByZWNvcmQsIHN1Ym1pdHRlcl9mb3JtYXQgKSB7CiAgICBMb2cudHJhY2UoICJFbnRlcmluZzogdHJhY2VyYnVsbGV0X3htbGRvbSBmdW5jdGlvbiIgKTsKICAgIExvZy5pbmZvICggIlRoZSByZWNvcmQgICBpcz0gIiwgcmVjb3JkICk7CiAgICBMb2cuaW5mbyAoICJUaGUgc3VibWl0ZXIgaXM9ICIsIHN1Ym1pdHRlcl9mb3JtYXQpOwoKICAgIHZhciBkb21QYXJzZXI9bmV3IFhNTERPTS5ET01QYXJzZXIoKTsKCgogICAgdmFyIGRvYyA9IGRvbVBhcnNlci5wYXJzZUZyb21TdHJpbmcoIHJlY29yZCApOwogICAgdmFyIHJlY29yZEVsZW1lbnQ9ZG9jLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdyZWNvcmQnKVswXTsKICAgIHZhciB2YWx1ZT1yZWNvcmRFbGVtZW50LmNoaWxkTm9kZXNbMF0ubm9kZVZhbHVlOwoKICAgIExvZy50cmFjZSggIkxlYXZpbmc6IHRyYWNlcmJ1bGxldF94bWxkb20gZnVuY3Rpb24iICk7CiAgICByZXR1cm4gIkRvbmUgd2l0aCByZWNvcmQ6ICIgKyB2YWx1ZTsKfQ==\",\n" +
                "                \"moduleName\": \"\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"javascript\": \"dXNlKCAiTG9nIiApOwp1c2UoICJYTUxET00iKTsKCgpmdW5jdGlvbiB0cmFjZXJidWxsZXRfeG1sZG9tKCByZWNvcmQsIHN1Ym1pdHRlcl9mb3JtYXQgKSB7CiAgICBMb2cudHJhY2UoICJFbnRlcmluZzogdHJhY2VyYnVsbGV0X3htbGRvbSBmdW5jdGlvbiIgKTsKICAgIExvZy5pbmZvICggIlRoZSByZWNvcmQgICBpcz0gIiwgcmVjb3JkICk7CiAgICBMb2cuaW5mbyAoICJUaGUgc3VibWl0ZXIgaXM9ICIsIHN1Ym1pdHRlcl9mb3JtYXQpOwoKICAgIHZhciBkb21QYXJzZXI9bmV3IFhNTERPTS5ET01QYXJzZXIoKTsKCgogICAgdmFyIGRvYyA9IGRvbVBhcnNlci5wYXJzZUZyb21TdHJpbmcoIHJlY29yZCApOwogICAgdmFyIHJlY29yZEVsZW1lbnQ9ZG9jLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdyZWNvcmQnKVswXTsKICAgIHZhciB2YWx1ZT1yZWNvcmRFbGVtZW50LmNoaWxkTm9kZXNbMF0ubm9kZVZhbHVlOwoKICAgIExvZy50cmFjZSggIkxlYXZpbmc6IHRyYWNlcmJ1bGxldF94bWxkb20gZnVuY3Rpb24iICk7CiAgICByZXR1cm4gIkRvbmUgd2l0aCByZWNvcmQ6ICIgKyB2YWx1ZTsKfQ==\",\n" +
                "                \"moduleName\": \"moduleName2\"\n" +
                "            }]," +
                "        \"name\": \"test\",\n" +
                "        \"svnProjectForInvocationJavascript\": \"dataio-js-test-projects\",\n" +
                "        \"svnRevision\": 83597" +
                "}";

        final FlowComponentContent flowComponentContent = jsonbContext.unmarshall(data, FlowComponentContent.class);
        assertThat("fisk", flowComponentContent.getName(), is("test"));
        assertThat(flowComponentContent.getRequireCache(), is(nullValue()));
        assertThat(flowComponentContent.getDescription(), is(nullValue()));
    }

    @Test
    public void testJsonUnmarchall() throws Exception {

        String data = "{ \"invocationJavascriptName\": \"trunk/tracerBulletXmlDom.js\",\n" +
                "        \"invocationMethod\": \"tracerbullet_xmldom\",\n" +
                "        \"javascripts\": [\n" +
                "            {\n" +
                "                \"javascript\": \"dXNlKCAiTG9nIiApOwp1c2UoICJYTUxET00iKTsKCgpmdW5jdGlvbiB0cmFjZXJidWxsZXRfeG1sZG9tKCByZWNvcmQsIHN1Ym1pdHRlcl9mb3JtYXQgKSB7CiAgICBMb2cudHJhY2UoICJFbnRlcmluZzogdHJhY2VyYnVsbGV0X3htbGRvbSBmdW5jdGlvbiIgKTsKICAgIExvZy5pbmZvICggIlRoZSByZWNvcmQgICBpcz0gIiwgcmVjb3JkICk7CiAgICBMb2cuaW5mbyAoICJUaGUgc3VibWl0ZXIgaXM9ICIsIHN1Ym1pdHRlcl9mb3JtYXQpOwoKICAgIHZhciBkb21QYXJzZXI9bmV3IFhNTERPTS5ET01QYXJzZXIoKTsKCgogICAgdmFyIGRvYyA9IGRvbVBhcnNlci5wYXJzZUZyb21TdHJpbmcoIHJlY29yZCApOwogICAgdmFyIHJlY29yZEVsZW1lbnQ9ZG9jLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdyZWNvcmQnKVswXTsKICAgIHZhciB2YWx1ZT1yZWNvcmRFbGVtZW50LmNoaWxkTm9kZXNbMF0ubm9kZVZhbHVlOwoKICAgIExvZy50cmFjZSggIkxlYXZpbmc6IHRyYWNlcmJ1bGxldF94bWxkb20gZnVuY3Rpb24iICk7CiAgICByZXR1cm4gIkRvbmUgd2l0aCByZWNvcmQ6ICIgKyB2YWx1ZTsKfQ==\",\n" +
                "                \"moduleName\": \"\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"javascript\": \"dXNlKCAiTG9nIiApOwp1c2UoICJYTUxET00iKTsKCgpmdW5jdGlvbiB0cmFjZXJidWxsZXRfeG1sZG9tKCByZWNvcmQsIHN1Ym1pdHRlcl9mb3JtYXQgKSB7CiAgICBMb2cudHJhY2UoICJFbnRlcmluZzogdHJhY2VyYnVsbGV0X3htbGRvbSBmdW5jdGlvbiIgKTsKICAgIExvZy5pbmZvICggIlRoZSByZWNvcmQgICBpcz0gIiwgcmVjb3JkICk7CiAgICBMb2cuaW5mbyAoICJUaGUgc3VibWl0ZXIgaXM9ICIsIHN1Ym1pdHRlcl9mb3JtYXQpOwoKICAgIHZhciBkb21QYXJzZXI9bmV3IFhNTERPTS5ET01QYXJzZXIoKTsKCgogICAgdmFyIGRvYyA9IGRvbVBhcnNlci5wYXJzZUZyb21TdHJpbmcoIHJlY29yZCApOwogICAgdmFyIHJlY29yZEVsZW1lbnQ9ZG9jLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdyZWNvcmQnKVswXTsKICAgIHZhciB2YWx1ZT1yZWNvcmRFbGVtZW50LmNoaWxkTm9kZXNbMF0ubm9kZVZhbHVlOwoKICAgIExvZy50cmFjZSggIkxlYXZpbmc6IHRyYWNlcmJ1bGxldF94bWxkb20gZnVuY3Rpb24iICk7CiAgICByZXR1cm4gIkRvbmUgd2l0aCByZWNvcmQ6ICIgKyB2YWx1ZTsKfQ==\",\n" +
                "                \"moduleName\": \"moduleName2\"\n" +
                "            }]," +
                "        \"name\": \"test\",\n" +
                "        \"description\": \"description\",\n" +
                "        \"svnProjectForInvocationJavascript\": \"dataio-js-test-projects\",\n" +
                "        \"svnRevision\": 83597,\n" +
                "        \"requireCache\": \"RequireCacheString\"" +
                "}";

        final FlowComponentContent flowComponentContent = jsonbContext.unmarshall(data, FlowComponentContent.class);
        assertThat(flowComponentContent.getName(), is("test"));
        assertThat(flowComponentContent.getRequireCache(), is("RequireCacheString"));
        assertThat(flowComponentContent.getDescription(), is("description"));
    }

    public static FlowComponentContent newFlowComponentContentInstance() {
        return new FlowComponentContent(NAME, SVN_PROJECT_FOR_INVOCATION_JAVASCRIPT, SVN_REVISION, JAVA_SCRIPT_NAME, JAVASCRIPTS, INVOCATION_METHOD, DESCRIPTION, REQUIRE_CACHE);
    }
}
