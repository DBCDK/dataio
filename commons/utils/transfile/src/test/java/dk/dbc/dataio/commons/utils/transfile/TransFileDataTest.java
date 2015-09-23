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

import org.hamcrest.core.IsNull;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TransFileDataTest {
    @Test (expected = NullPointerException.class)
    public void nullInput_generateException () {
        new TransFileData(null);
    }

    @Test
    public void emptyInputLine_Ok() {
        TransFileData line = new TransFileData("");
        assertThat(line.getBaseName(), is(IsNull.nullValue()));
        assertThat(line.getFileName(), is(IsNull.nullValue()));
        assertThat(line.getTechnicalProtocol(), is(IsNull.nullValue()));
        assertThat(line.getLibraryFormat(), is(IsNull.nullValue()));
        assertThat(line.getCharacterSet(), is(IsNull.nullValue()));
        assertThat(line.getPrimaryEmailAddress(), is(IsNull.nullValue()));
        assertThat(line.getSecondaryEmailAddress(), is(IsNull.nullValue()));
        assertThat(line.getInitials(), is(IsNull.nullValue()));
    }

    @Test (expected = IllegalArgumentException.class)
    public void oneSpaceLine_generateException() {
        new TransFileData(" ");
    }

    @Test (expected = IllegalArgumentException.class)
    public void oneCommaLine_generateException() {
        new TransFileData(",");
    }

    @Test
    public void validFullExample_Ok() {
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
        assertThat(line.getBaseName(), is("databroendpr2"));
        assertThat(line.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(line.getSubmitterNumber(), is(150014L));
        assertThat(line.getTechnicalProtocol(), is("xml"));
        assertThat(line.getLibraryFormat(), is("nmalbum"));
        assertThat(line.getCharacterSet(), is("utf8"));
        assertThat(line.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(line.getSecondaryEmailAddress(), is("secondary@dbc.dk"));
        assertThat(line.getInitials(), is("initialstext"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void leadingEmptyField_generateException() {
        new TransFileData(",b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }

    @Test (expected = IllegalArgumentException.class)
    public void emptyFieldInTheMiddle_generateException() {
        new TransFileData("b=databroendpr2,,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }

    @Test
    public void trailingEmptyField_Ok() {
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
        assertThat(line.getBaseName(), is("databroendpr2"));
        assertThat(line.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(line.getSubmitterNumber(), is(150014L));
        assertThat(line.getTechnicalProtocol(), is("xml"));
        assertThat(line.getLibraryFormat(), is("nmalbum"));
        assertThat(line.getCharacterSet(), is("utf8"));
        assertThat(line.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(line.getSecondaryEmailAddress(), is("secondary@dbc.dk"));
        assertThat(line.getInitials(), is("initialstext"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void missingBaseName_generateException() {
        new TransFileData("f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }

    @Test (expected = IllegalArgumentException.class)
    public void missingFileName_generateException() {
        new TransFileData("b=databroendpr2,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }

    @Test
    public void missingProtocol_Ok() {
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
        assertThat(line.getBaseName(), is("databroendpr2"));
        assertThat(line.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(line.getSubmitterNumber(), is(150014L));
        assertThat(line.getTechnicalProtocol(), is(IsNull.nullValue()));
        assertThat(line.getLibraryFormat(), is("nmalbum"));
        assertThat(line.getCharacterSet(), is("utf8"));
        assertThat(line.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(line.getSecondaryEmailAddress(), is("secondary@dbc.dk"));
        assertThat(line.getInitials(), is("initialstext"));
    }

    @Test
    public void missingCharacterSet_Ok() {
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
        assertThat(line.getBaseName(), is("databroendpr2"));
        assertThat(line.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(line.getSubmitterNumber(), is(150014L));
        assertThat(line.getTechnicalProtocol(), is("xml"));
        assertThat(line.getLibraryFormat(), is("nmalbum"));
        assertThat(line.getCharacterSet(), is(IsNull.nullValue()));
        assertThat(line.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(line.getSecondaryEmailAddress(), is("secondary@dbc.dk"));
        assertThat(line.getInitials(), is("initialstext"));
    }

    @Test
    public void missingLibraryFormat_Ok() {
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
        assertThat(line.getBaseName(), is("databroendpr2"));
        assertThat(line.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(line.getSubmitterNumber(), is(150014L));
        assertThat(line.getTechnicalProtocol(), is("xml"));
        assertThat(line.getLibraryFormat(), is(IsNull.nullValue()));
        assertThat(line.getCharacterSet(), is("utf8"));
        assertThat(line.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(line.getSecondaryEmailAddress(), is("secondary@dbc.dk"));
        assertThat(line.getInitials(), is("initialstext"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void missingPrimaryEmailAddress_generateException() {
        new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,M=secondary@dbc.dk,i=initialstext");
    }

    @Test
    public void missingSecondaryEmailAddress_Ok() {
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,i=initialstext");
        assertThat(line.getBaseName(), is("databroendpr2"));
        assertThat(line.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(line.getSubmitterNumber(), is(150014L));
        assertThat(line.getTechnicalProtocol(), is("xml"));
        assertThat(line.getLibraryFormat(), is("nmalbum"));
        assertThat(line.getCharacterSet(), is("utf8"));
        assertThat(line.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(line.getSecondaryEmailAddress(), is(IsNull.nullValue()));
        assertThat(line.getInitials(), is("initialstext"));
    }

    @Test
    public void missingInitials_Ok() {
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk");
        assertThat(line.getBaseName(), is("databroendpr2"));
        assertThat(line.getFileName(), is("150014.201305272202.albumTOTAL.001.xml"));
        assertThat(line.getSubmitterNumber(), is(150014L));
        assertThat(line.getTechnicalProtocol(), is("xml"));
        assertThat(line.getLibraryFormat(), is("nmalbum"));
        assertThat(line.getCharacterSet(), is("utf8"));
        assertThat(line.getPrimaryEmailAddress(), is("kildepost@dbc.dk"));
        assertThat(line.getSecondaryEmailAddress(), is("secondary@dbc.dk"));
        assertThat(line.getInitials(), is(IsNull.nullValue()));
    }

    @Test (expected = IllegalArgumentException.class)
    public void duplicateFields_generateException() {
        new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }

    @Test (expected = IllegalArgumentException.class)
    public void basenameNotFirstField_generateException() {
        new TransFileData("f=150014.201305272202.albumTOTAL.001.xml,b=databroendpr2,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }


}