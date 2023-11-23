package dk.dbc.dataio.commons.utils;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JunitXmlStreamWriterTest {
    @Test
    public void output() throws Exception {
        final String expectedOutput =
                "<testsuites>" +
                        "<testsuite name=\"MyClassTest\">" +
                        "<testcase classname=\"dk.dbc.dataio.MyClass\" name=\"test1\" time=\"10\"></testcase>" +
                        "<testcase classname=\"dk.dbc.dataio.MyClass\" name=\"test2\">" +
                        "<failure message=\"test failed\">" +
                        "output of failed test" +
                        "</failure>" +
                        "</testcase>" +
                        "<testcase classname=\"dk.dbc.dataio.MyClass\" name=\"test3\">" +
                        "<error message=\"test erred\">" +
                        "output of erred test -&gt; fatal error" +
                        "</error>" +
                        "</testcase>" +
                        "<testcase classname=\"dk.dbc.dataio.MyClass\" name=\"test4\">" +
                        "<skipped message=\"test skipped\">" +
                        "output of skipped test" +
                        "</skipped>" +
                        "</testcase>" +
                        "<testcase classname=\"dk.dbc.dataio.MyClass\" name=\"test5\">" +
                        "<system-out>" +
                        "system STDOUT" +
                        "</system-out>" +
                        "<system-err>" +
                        "system STDERR" +
                        "</system-err>" +
                        "</testcase>" +
                        "</testsuite>" +
                        "</testsuites>";

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JunitXmlStreamWriter junitXmlStreamWriter = new JunitXmlStreamWriter(baos)) {
            try (JunitXmlTestSuite testSuite = new JunitXmlTestSuite("MyClassTest", junitXmlStreamWriter)) {
                testSuite.addTestCase(JunitXmlTestCase.passed("test1", "dk.dbc.dataio.MyClass")
                        .withTime(10));
                testSuite.addTestCase(JunitXmlTestCase.failed("test2", "dk.dbc.dataio.MyClass", "test failed", "output of failed test"));
                testSuite.addTestCase(JunitXmlTestCase.erred("test3", "dk.dbc.dataio.MyClass", "test erred", "output of erred test -> fatal error"));
                testSuite.addTestCase(JunitXmlTestCase.skipped("test4", "dk.dbc.dataio.MyClass", "test skipped", "output of skipped test"));
                testSuite.addTestCase(JunitXmlTestCase.passed("test5", "dk.dbc.dataio.MyClass")
                        .withStdout("system STDOUT")
                        .withStderr("system STDERR"));
            }
        }

        assertThat(new String(baos.toByteArray(), StandardCharsets.UTF_8), is(expectedOutput));
    }
}
