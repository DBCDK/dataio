/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.commons.utils;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        try (final JunitXmlStreamWriter junitXmlStreamWriter = new JunitXmlStreamWriter(baos)) {
            try (final JunitXmlTestSuite testSuite = new JunitXmlTestSuite("MyClassTest", junitXmlStreamWriter)) {
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