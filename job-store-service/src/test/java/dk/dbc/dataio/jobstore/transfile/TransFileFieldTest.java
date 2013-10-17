package dk.dbc.dataio.jobstore.transfile;

import dk.dbc.dataio.jobstore.transfile.TransFileField.TransFileFieldId;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

/**
 *
 * @author slf
 */
public class TransFileFieldTest {
    
    // Illegal input values
    
    @Test (expected = NullPointerException.class)
    public void nullInput_generateException () {
        TransFileField field = new TransFileField(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void invalidInput_generateException () {
        TransFileField field = new TransFileField("");
    }

    @Test (expected = IllegalArgumentException.class)
    public void noEqualSignInField_generateException () {
        TransFileField field = new TransFileField("illegalField");
    }

    @Test (expected = IllegalArgumentException.class)
    public void blanksInNameInField_generateException () {
        TransFileField field = new TransFileField(" i=content");
    }

    @Test (expected = IllegalArgumentException.class)
    public void blanksInContentInField_generateException () {
        TransFileField field = new TransFileField("i=con tent");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fieldNameMissing_generateException () {
        TransFileField field = new TransFileField("=content");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fieldNameContainsMultipleChars_generateException () {
        TransFileField field = new TransFileField("is=content");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fieldNameIsUnknown_generateException () {
        TransFileField field = new TransFileField("x=content");
    }

    
    // Field: b
    
    @Test (expected = IllegalArgumentException.class)
    public void bFieldEqualsXxx_generateException () {
        TransFileField field = new TransFileField("b=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void bFieldEqualsDatabroendpr2WithCapitalLetter_generateException () {
        TransFileField field = new TransFileField("b=Databroendpr2");
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
        TransFileField field = new TransFileField("f=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldLibraryNumberNotNumeric_generateException () {
        TransFileField field = new TransFileField("f=12345V.1234567890.file-name_X.001.xml");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldTooShortLibraryNumber_generateException () {
        TransFileField field = new TransFileField("f=12345.1234567890.file-name_X.001.xml");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldTooLongLibraryNumber_generateException () {
        TransFileField field = new TransFileField("f=1234567.1234567890.file-name_X.001.xml");
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
        TransFileField field = new TransFileField("f=123456.1234567890.file-name_X:4.001.xml");
    }

    @Test (expected = IllegalArgumentException.class)
    public void fFieldDanishCharsInFileName_generateException () {
        TransFileField field = new TransFileField("f=123456.1234567890.file-næme_X.001.xml");
    }

    
    // Field: t
    
    @Test (expected = IllegalArgumentException.class)
    public void tFieldEqualsXxx_generateException () {
        TransFileField field = new TransFileField("t=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void tFieldEqualsXmlWithCapitalLetter_generateException () {
        TransFileField field = new TransFileField("t=XML");
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
        TransFileField field = new TransFileField("o=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void oFieldEqualsNmalbumWithCapitalLetter_generateException () {
        TransFileField field = new TransFileField("o=Nmalbum");
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
        TransFileField field = new TransFileField("c=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void cFieldEqualsUtf8WithCapitalLetter_generateException () {
        TransFileField field = new TransFileField("c=UTF8");
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
        TransFileField field = new TransFileField("m=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void mFieldEmailAddressWithoutAt_generateException () {
        TransFileField field = new TransFileField("m=mail.com");
    }

    @Test (expected = IllegalArgumentException.class)
    public void mFieldEmailAddressWithoutSubdomain_generateException () {
        TransFileField field = new TransFileField("m=mail@.com");
    }

    @Test (expected = IllegalArgumentException.class)
    public void mFieldEmailAddressWithoutDomain_generateException () {
        TransFileField field = new TransFileField("m=mail@");
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
        TransFileField field = new TransFileField("M=xxx");
    }

    @Test (expected = IllegalArgumentException.class)
    public void MFieldEmailAddressWithoutAt_generateException () {
        TransFileField field = new TransFileField("M=mail.com");
    }

    @Test (expected = IllegalArgumentException.class)
    public void MFieldEmailAddressWithoutSubdomain_generateException () {
        TransFileField field = new TransFileField("M=mail@.com");
    }

    @Test (expected = IllegalArgumentException.class)
    public void MFieldEmailAddressWithoutDomain_generateException () {
        TransFileField field = new TransFileField("M=mail@");
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
        TransFileField field = new TransFileField("i=Æble");
    }

    @Test (expected = IllegalArgumentException.class)
    public void iFieldContainsColon_generateException () {
        TransFileField field = new TransFileField("i=Apple:Banana");
    }

    @Test (expected = IllegalArgumentException.class)
    public void iFieldContainsDash_generateException () {
        TransFileField field = new TransFileField("i=Apple-Banana");
    }

    @Test (expected = IllegalArgumentException.class)
    public void iFieldContainsUnderscore_generateException () {
        TransFileField field = new TransFileField("i=Apple_Banana");
    }

    @Test
    public void iFieldValidInitials_validTransFileField () {
        TransFileField field = new TransFileField("i=AppleBanana");
        assertThat(field.getKey(), is(TransFileFieldId.INITIALS));
        assertThat(field.getContent(), is("AppleBanana"));
    }
    

}