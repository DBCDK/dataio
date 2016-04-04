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

package dk.dbc.dataio.commons.utils.transfile;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TransFileTest {
    
    private final static String validTransFileDataLine1  = "b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext";
    private final static String validTransFileDataLine2  = "b=databroendpr2,f=150014.201305272202.albumTOTAL.002.xml,t=xml,o=nmtrack,c=utf8,m=elephant@dbc.dk,M=dinosaur@dbc.dk,i=";
    private final static String invalidTransFileDataLine = "b=databroendpr2,x=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext";
    private final static String emptyLine = "";
    private final static String endMark = "slut";
    
    @Test (expected = TransFile.UnexpectedEndOfFileException.class)
    public void emptyInputWithoutEndmark_generateException () throws UnsupportedEncodingException {
        InputStream stream = constructStream(emptyLine);
        TransFile.process(stream);
    }

    @Test
    public void onlyEndMark_Ok () throws UnsupportedEncodingException {
        InputStream stream = constructStream(endMark);
        List<TransFileData> data = TransFile.process(stream);
        assertThat(data.isEmpty(), is(true));
    }

    @Test
    public void emptyLinesInputWithEndMark_Ok () throws UnsupportedEncodingException {
        InputStream stream = constructStream(emptyLine, emptyLine, endMark);
        List<TransFileData> data = TransFile.process(stream);
        assertThat(data.isEmpty(), is(true));
    }

    @Test
    public void oneValidInputLineWithEndMark_Ok () throws UnsupportedEncodingException {
        InputStream stream = constructStream(validTransFileDataLine1, endMark);
        List<TransFileData> data = TransFile.process(stream);
        assertThat(data.size(), is(1));
        TransFileData tfd = data.get(0);
        assertThat(tfd.getBaseName(), is("databroendpr2"));
        assertThat(tfd.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(tfd.getTechnicalProtocol(), is("xml"));
        assertThat(tfd.getLibraryFormat(), is("nmalbum"));
        assertThat(tfd.getCharacterSet(), is("utf8"));
        assertThat(tfd.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(tfd.getSecondaryEmailAddress(), is("secondary@dbc.dk"));
        assertThat(tfd.getInitials(), is("initialstext"));
    }

    @Test (expected = TransFile.UnexpectedEndOfFileException.class)
    public void oneValidInputLineWithoutEndMark_generateException () throws UnsupportedEncodingException {
        InputStream stream = constructStream(validTransFileDataLine1);
        TransFile.process(stream);
    }

    @Test
    public void twoValidInputLineWithEndMark_Ok () throws UnsupportedEncodingException {
        InputStream stream = constructStream(validTransFileDataLine1, validTransFileDataLine2, endMark);
        List<TransFileData> data = TransFile.process(stream);
        assertThat(data.size(), is(2));
        TransFileData tfd = data.get(0);
        assertThat(tfd.getBaseName(), is("databroendpr2"));
        assertThat(tfd.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(tfd.getTechnicalProtocol(), is("xml"));
        assertThat(tfd.getLibraryFormat(), is("nmalbum"));
        assertThat(tfd.getCharacterSet(), is("utf8"));
        assertThat(tfd.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(tfd.getSecondaryEmailAddress(), is("secondary@dbc.dk"));
        assertThat(tfd.getInitials(), is("initialstext"));
        tfd = data.get(1);
        assertThat(tfd.getBaseName(), is("databroendpr2"));
        assertThat(tfd.getFileName(), is("150014.201305272202.albumTOTAL.002.xml"));
        assertThat(tfd.getTechnicalProtocol(), is("xml"));
        assertThat(tfd.getLibraryFormat(), is("nmtrack"));
        assertThat(tfd.getCharacterSet(), is("utf8"));
        assertThat(tfd.getPrimaryEmailAddress(), is("elephant@dbc.dk"));
        assertThat(tfd.getSecondaryEmailAddress(), is("dinosaur@dbc.dk"));
        assertThat(tfd.getInitials(), is(""));
    }

    @Test (expected = IllegalArgumentException.class)
    public void oneInvalidInputLineWithEndMark_generateException () throws UnsupportedEncodingException {
        InputStream stream = constructStream(invalidTransFileDataLine, endMark);
        TransFile.process(stream);
    }

    @Test (expected = IllegalArgumentException.class)
    public void oneInvalidInputLineWithoutEndMark_generateException () throws UnsupportedEncodingException {
        InputStream stream = constructStream(invalidTransFileDataLine);
        TransFile.process(stream);
    }

    @Test (expected = IllegalArgumentException.class)
    public void oneValidAndOneInvalidInputLineWithEndMark_generateException () throws UnsupportedEncodingException {
        InputStream stream = constructStream(validTransFileDataLine1, invalidTransFileDataLine, endMark);
        TransFile.process(stream);
    }

    @Test (expected = IllegalArgumentException.class)
    public void oneValidAndOneInvalidInputLineWithoutEndMark_generateException () throws UnsupportedEncodingException {
        InputStream stream = constructStream(validTransFileDataLine1, invalidTransFileDataLine);
        TransFile.process(stream);
    }



    // Private utility methods
    
    /**
     * Joins a list of strings together into one, delimited by newlines ("\n")
     * 
     * @param texts The text strings to join
     * @return The joined text string
     */
    private String join(String... texts) {
        String acc = "";
        for (String s: texts) {
            if (!acc.isEmpty()) {
                acc += "\n";
            }
            acc += s;
        }
        return acc;
    }

    /**
     * Constructs an InputStream, containing the supplied input strings
     * 
     * @param content The array of strings, that should go into the stream
     * @return The InputStream, containing the strings
     * @throws UnsupportedEncodingException 
     */
    private InputStream constructStream(String... content) throws UnsupportedEncodingException {
        String contentString = join(content);
        return new ByteArrayInputStream(contentString.getBytes("ISO-8859-1"));
    }
}