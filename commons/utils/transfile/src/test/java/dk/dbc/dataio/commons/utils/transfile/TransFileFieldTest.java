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

import dk.dbc.dataio.commons.utils.transfile.TransFileField.TransFileFieldId;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

public class TransFileFieldTest {
    
    // Illegal input values
    
    @Test (expected = NullPointerException.class)
    public void nullInput_generateException () {
        new TransFileField(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void invalidInput_generateException () {
        new TransFileField("");
    }

    @Test (expected = IllegalArgumentException.class)
    public void noEqualSignInField_generateException () {
        new TransFileField("illegalField");
    }

    @Test (expected = IllegalArgumentException.class)
    public void blanksInNameInField_generateException () {
        new TransFileField(" i=content");
    }

    @Test (expected = IllegalArgumentException.class)
    public void blanksInContentInField_generateException () {
        new TransFileField("i=con tent");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fieldNameMissing_generateException () {
        new TransFileField("=content");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fieldNameContainsMultipleChars_generateException () {
        new TransFileField("is=content");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fieldNameIsUnknown_generateException () {
        new TransFileField("x=content");
    }

    
    // Field: b
    
    @Test (expected = IllegalArgumentException.class)
    public void bFieldEqualsXxx_generateException () {
        new TransFileField("b=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void bFieldEqualsDatabroendpr2WithCapitalLetter_generateException () {
        new TransFileField("b=Databroendpr2");
    }

    @Test
    public void bFieldEqualsDatabroendpr2_validTransFileField () {
        TransFileField field = new TransFileField("b=databroendpr2");
        assertThat(field.getKey(), is(TransFileFieldId.BASE_NAME));
        assertThat(field.getContent(), is("databroendpr2"));
    }


    // Field: f

    @Test (expected = IllegalArgumentException.class)
    public void fFieldEqualsXxx_generateException () {
        new TransFileField("f=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldLibraryNumberNotNumeric_generateException () {
        new TransFileField("f=12345V.1234567890.file-name_X.001.xml");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldTooShortLibraryNumber_generateException () {
        new TransFileField("f=12345.1234567890.file-name_X.001.xml");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldTooLongLibraryNumber_generateException () {
        new TransFileField("f=1234567.1234567890.file-name_X.001.xml");
    }

    @Test
    public void fFieldValidLibraryNumber_validTransFileField () {
        TransFileField field = new TransFileField("f=123456.1234567890.file-name_X.001.xml");
        assertThat(field.getKey(), is(TransFileFieldId.FILE_NAME));
        assertThat(field.getContent(), is("123456.1234567890.file-name_X.001.xml"));
    }

    @Test
    public void fFieldValidLibraryNumberButEmptyRemainder_validTransFileField () {
        TransFileField field = new TransFileField("f=123456.");
        assertThat(field.getKey(), is(TransFileFieldId.FILE_NAME));
        assertThat(field.getContent(), is("123456."));
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldInvalidCharsInFileName_generateException () {
        new TransFileField("f=123456.1234567890.file-name_X:4.001.xml");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldDanishCharsInFileName_generateException () {
        new TransFileField("f=123456.1234567890.file-næme_X.001.xml");
    }

    
    // Field: t
    
    @Test (expected = IllegalArgumentException.class)
    public void tFieldEqualsXxx_generateException () {
        new TransFileField("t=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void tFieldEqualsXmlWithCapitalLetter_generateException () {
        new TransFileField("t=XML");
    }

    @Test
    public void tFieldEqualsXml_validTransFileField () {
        TransFileField field = new TransFileField("t=xml");
        assertThat(field.getKey(), is(TransFileFieldId.TECHNICAL_PROTOCOL));
        assertThat(field.getContent(), is("xml"));
    }


    // Field: o
    
    @Test (expected = IllegalArgumentException.class)
    public void oFieldEqualsXxx_generateException () {
        new TransFileField("o=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void oFieldEqualsNmalbumWithCapitalLetter_generateException () {
        new TransFileField("o=Nmalbum");
    }

    @Test
    public void oFieldEqualsNmalbum_validTransFileField () {
        TransFileField field = new TransFileField("o=nmalbum");
        assertThat(field.getKey(), is(TransFileFieldId.LIBRARY_FORMAT));
        assertThat(field.getContent(), is("nmalbum"));
    }

    @Test
    public void oFieldEqualsNmtrack_validTransFileField () {
        TransFileField field = new TransFileField("o=nmtrack");
        assertThat(field.getKey(), is(TransFileFieldId.LIBRARY_FORMAT));
        assertThat(field.getContent(), is("nmtrack"));
    }


    // Field: c
    
    @Test (expected = IllegalArgumentException.class)
    public void cFieldEqualsXxx_generateException () {
        new TransFileField("c=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void cFieldEqualsUtf8WithCapitalLetter_generateException () {
        new TransFileField("c=UTF8");
    }

    @Test
    public void cFieldEqualsUtf8_validTransFileField () {
        TransFileField field = new TransFileField("c=utf8");
        assertThat(field.getKey(), is(TransFileFieldId.CHARACTER_SET));
        assertThat(field.getContent(), is("utf8"));
    }


    // Field: m
    
    @Test (expected = IllegalArgumentException.class)
    public void mFieldEqualsXxx_generateException () {
        new TransFileField("m=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void mFieldEmailAddressWithoutAt_generateException () {
        new TransFileField("m=mail.com");
    }

    @Test (expected = IllegalArgumentException.class)
    public void mFieldEmailAddressWithoutSubdomain_generateException () {
        new TransFileField("m=mail@.com");
    }

    @Test (expected = IllegalArgumentException.class)
    public void mFieldEmailAddressWithoutDomain_generateException () {
        new TransFileField("m=mail@");
    }

    @Test
    public void mFieldEmailAddressOnlyWithToplevelDomain_validTransFileField () {
        TransFileField field = new TransFileField("m=mail@com");
        assertThat(field.getKey(), is(TransFileFieldId.PRIMARY_EMAIL_ADDRESS));
        assertThat(field.getContent(), is("mail@com"));
    }

    @Test
    public void mFieldValidEmail_validTransFileField () {
        TransFileField field = new TransFileField("m=mail@dbc.dk");
        assertThat(field.getKey(), is(TransFileFieldId.PRIMARY_EMAIL_ADDRESS));
        assertThat(field.getContent(), is("mail@dbc.dk"));
    }

    @Test
    public void mFieldValidComplexEmail_validTransFileField () {
        TransFileField field = new TransFileField("m=e_mail.box@d-b-c.3.dk");
        assertThat(field.getKey(), is(TransFileFieldId.PRIMARY_EMAIL_ADDRESS));
        assertThat(field.getContent(), is("e_mail.box@d-b-c.3.dk"));
    }
    

    // Field: M
    
    @Test (expected = IllegalArgumentException.class)
    public void MFieldEqualsXxx_generateException () {
        new TransFileField("M=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void MFieldEmailAddressWithoutAt_generateException () {
        new TransFileField("M=mail.com");
    }

    @Test (expected = IllegalArgumentException.class)
    public void MFieldEmailAddressWithoutSubdomain_generateException () {
        new TransFileField("M=mail@.com");
    }

    @Test (expected = IllegalArgumentException.class)
    public void MFieldEmailAddressWithoutDomain_generateException () {
        new TransFileField("M=mail@");
    }

    @Test
    public void MFieldEmailAddressOnlyWithToplevelDomain_validTransFileField () {
        TransFileField field = new TransFileField("M=mail@com");
        assertThat(field.getKey(), is(TransFileFieldId.SECONDARY_EMAIL_ADDRESS));
        assertThat(field.getContent(), is("mail@com"));
    }

    @Test
    public void MFieldValidEmail_validTransFileField () {
        TransFileField field = new TransFileField("M=mail@dbc.dk");
        assertThat(field.getKey(), is(TransFileFieldId.SECONDARY_EMAIL_ADDRESS));
        assertThat(field.getContent(), is("mail@dbc.dk"));
    }

    @Test
    public void MFieldValidComplexEmail_validTransFileField () {
        TransFileField field = new TransFileField("M=e_mail.box@d-b-c.3.dk");
        assertThat(field.getKey(), is(TransFileFieldId.SECONDARY_EMAIL_ADDRESS));
        assertThat(field.getContent(), is("e_mail.box@d-b-c.3.dk"));
    }
    

    // Field: i
    
    @Test (expected = IllegalArgumentException.class)
    public void iFieldContainsDanishChars_generateException () {
        new TransFileField("i=Æble");
    }

    @Test (expected = IllegalArgumentException.class)
    public void iFieldContainsColon_generateException () {
        new TransFileField("i=Apple:Banana");
    }

    @Test (expected = IllegalArgumentException.class)
    public void iFieldContainsDash_generateException () {
        new TransFileField("i=Apple-Banana");
    }

    @Test (expected = IllegalArgumentException.class)
    public void iFieldContainsUnderscore_generateException () {
        new TransFileField("i=Apple_Banana");
    }

    @Test
    public void iFieldValidInitials_validTransFileField () {
        TransFileField field = new TransFileField("i=AppleBanana");
        assertThat(field.getKey(), is(TransFileFieldId.INITIALS));
        assertThat(field.getContent(), is("AppleBanana"));
    }
    
    public @Test void iFieldIsEmpty_validTransFileField() {
        TransFileField field = new TransFileField("i=");
        assertThat(field.getKey(), is(TransFileFieldId.INITIALS));
        assertThat(field.getContent(), is(""));
    }

}