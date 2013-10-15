package dk.dbc.dataio.jobstore.transfile;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * 
 * @author slf
 */
public class TransFileField {
    private final static List<String> VALID_TRANSFILE_b_CONTENT_VALUES = Arrays.asList("databroendpr2");
    private final static List<String> VALID_TRANSFILE_t_CONTENT_VALUES = Arrays.asList("xml");
    private final static List<String> VALID_TRANSFILE_o_CONTENT_VALUES = Arrays.asList("nmalbum", "nmtrack");
    private final static List<String> VALID_TRANSFILE_c_CONTENT_VALUES = Arrays.asList("utf8");

    private final String name;
    private final String content;

    /**
     * Constructor: Construct the TransFileField object
     * @param field A string, containing Field Identifier and Field content in the form: fieldId=fieldContent
     * @throws IllegalArgumentException 
     */
    public TransFileField(final String field) throws IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(field, "field");
        if (field.contains(" ")) {
            throw new IllegalArgumentException("Field must not contain a blank character");
        }
        if (!field.contains("=")) {
            throw new IllegalArgumentException("Field does not contain a Field seperator: '='");
        }
        String parts[] = field.split("=", 2);
        name = parts[0];
        checkValidFieldIdentifierOrThrow(name);
        content = parts[1];
        checkValidFieldContentOrThrow(name, content);
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
    
    private void checkValidFieldIdentifierOrThrow(String fieldIdentifier) {
        if (fieldIdentifier.length() != 1) {
            throw new IllegalArgumentException("Field identifier: '" + fieldIdentifier + "' shall contain only one character");
        }
    }

    private void checkValidEnumeratedFieldContentValueOrThrow(final List<String> validFieldContent, String fieldContent) {
        if (!validFieldContent.contains(fieldContent)) {
            throw new IllegalArgumentException("Field content: '" + fieldContent + "' is not valid in this context");
        }
    }
    
    private void checkValidFieldContentOrThrow(String fieldIdentifier, String fieldContent) {
        switch (fieldIdentifier) {
            case "b":
                checkValidEnumeratedFieldContentValueOrThrow(VALID_TRANSFILE_b_CONTENT_VALUES, fieldContent);
                break;
            case "f":
                // Not yet implemented
                break;
            case "t":
                checkValidEnumeratedFieldContentValueOrThrow(VALID_TRANSFILE_t_CONTENT_VALUES, fieldContent);
                break;
            case "c":
                checkValidEnumeratedFieldContentValueOrThrow(VALID_TRANSFILE_c_CONTENT_VALUES, fieldContent);
                break;
            case "o":
                checkValidEnumeratedFieldContentValueOrThrow(VALID_TRANSFILE_o_CONTENT_VALUES, fieldContent);
                break;
            case "m":
            case "M":
                checkValidEmailAddressOrThrow(fieldContent);
                break;
            case "i":
                // Not yet implemented
                break;
            default:
                throw new IllegalArgumentException("Field identifier: '" + fieldIdentifier + "' is invalid");
        }
    }
    
    private void checkValidEmailAddressOrThrow(String email) {
        try {
            new InternetAddress(email, true);
        } catch (AddressException ex) {
            throw new IllegalArgumentException("Field content: '" + email + "' is not a valid email address");
        }
    }
    
}
