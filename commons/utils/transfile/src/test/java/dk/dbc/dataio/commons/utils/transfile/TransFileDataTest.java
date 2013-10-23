package dk.dbc.dataio.commons.utils.transfile;

import org.hamcrest.core.IsNull;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TransFileDataTest {
    @Test (expected = NullPointerException.class)
    public void nullInput_generateException () {
        TransFileData line = new TransFileData(null);
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
        TransFileData line = new TransFileData(" ");
    }

    @Test (expected = IllegalArgumentException.class)
    public void oneCommaLine_generateException() {
        TransFileData line = new TransFileData(",");
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
        TransFileData line = new TransFileData(",b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }

    @Test (expected = IllegalArgumentException.class)
    public void emptyFieldInTheMiddle_generateException() {
        TransFileData line = new TransFileData("b=databroendpr2,,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
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
        TransFileData line = new TransFileData("f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }

    @Test (expected = IllegalArgumentException.class)
    public void missingFileName_generateException() {
        TransFileData line = new TransFileData("b=databroendpr2,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
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
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,o=nmalbum,c=utf8,M=secondary@dbc.dk,i=initialstext");
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
        TransFileData line = new TransFileData("b=databroendpr2,f=150014.201305272202.albumTOTAL.001.xml,t=xml,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }

    @Test (expected = IllegalArgumentException.class)
    public void basenameNotFirstField_generateException() {
        TransFileData line = new TransFileData("f=150014.201305272202.albumTOTAL.001.xml,b=databroendpr2,t=xml,o=nmalbum,c=utf8,m=kildepost@dbc.dk,M=secondary@dbc.dk,i=initialstext");
    }


}