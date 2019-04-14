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

package dk.dbc.dataio.sink.diff;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

@Ignore("Since the tests are not run in a docker container" +
        " where we ca be sure that the binaries called by " +
        "the external tool exist.")
@RunWith(Parameterized.class)
public class XmlDiffGeneratorParameterizedTest extends AbstractDiffGeneratorTest {

    private static final String CURRENT_XML = "-current.xml";
    private static final String NEXT_XML = "-next.xml";

    @Parameters(name = "xmldiff current {0} - next {1}")
    public static Collection<String[]> testData() throws URISyntaxException, IOException {
        final java.net.URL url = XmlDiffGeneratorParameterizedTest.class.getResource("/");
        final java.nio.file.Path resPath;
        resPath = java.nio.file.Paths.get(url.toURI());

        final List<String[]> result = new LinkedList<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(resPath)) {
            for (final Path fn : ds) {
                if (fn.getFileName().toString().endsWith(CURRENT_XML)) {
                    String[] fileEntry = new String[2];
                    fileEntry[0] = fn.getFileName().toString();
                    fileEntry[1] = fn.getFileName().toString().replace(CURRENT_XML,NEXT_XML);
                    result.add(fileEntry);
                }
            }
        }

        return result;
    }

    @Parameterized.Parameter( value=0)
    public String currentFileName;
    @Parameterized.Parameter( value=1)
    public String nextFileName;


    @Test
    public void testName() throws Exception {
        final ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
         final String diff = xmlDiffGenerator.getDiff(
                 ExternalToolDiffGenerator.Kind.XML,
                 XmlDiffGeneratorTest.readTestRecord( currentFileName ),
                 XmlDiffGeneratorTest.readTestRecord( nextFileName )
         );
         assertThat(diff, not(""));
    }
}
