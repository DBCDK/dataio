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

    /**
     * Getter: name The name of the TransFile Field
     * @return 
     */
    public String getName() {
        return name;
    }

    /**
     * Getter: content The content of the TransFile Field
     * @return 
     */
    public String getContent() {
        return content;
    }
    
    // Private methods
    
    /**
     * Checks if the fieldContent is valid. If not, an exception (IllegalArgumentException) is thrown.
     * @param fieldIdentifier
     * @param fieldContent
     * @throws IllegalArgumentException 
     */
    private void checkValidFieldContentOrThrow(String fieldIdentifier, String fieldContent) throws IllegalArgumentException {
        switch (fieldIdentifier) {
            case "b":
                checkValidEnumeratedFieldContentValueOrThrow(VALID_TRANSFILE_b_CONTENT_VALUES, fieldContent);
                break;
            case "f":
                checkValidFileNameFieldOrThrow(fieldContent);
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
                checkValidInitialsOrThrow(fieldContent);
                break;
            default:
                throw new IllegalArgumentException("Field identifier: '" + fieldIdentifier + "' is invalid");
        }
    }
    
    /**
     * Checks if the format for the Field Identifier is valid
     * @param fieldIdentifier
     * @throws IllegalArgumentException 
     */
    private void checkValidFieldIdentifierOrThrow(String fieldIdentifier) throws IllegalArgumentException {
        if (fieldIdentifier.length() != 1) {
            throw new IllegalArgumentException("Field identifier: '" + fieldIdentifier + "' shall contain only one character");
        }
    }

    /**
     * Checks if the supplied fieldContent is in the valid range (given by the validFieldContent parameter)
     * @param validFieldContent The valid range
     * @param fieldContent
     * @throws IllegalArgumentException 
     */
    private void checkValidEnumeratedFieldContentValueOrThrow(final List<String> validFieldContent, String fieldContent) throws IllegalArgumentException {
        if (!validFieldContent.contains(fieldContent)) {
            throw new IllegalArgumentException("Field content: '" + fieldContent + "' is not valid in this context");
        }
    }
    
    /**
     * Checks if the supplied email address is legal. Throws an IllegalArgumentException if not
     * @param email
     * @throws IllegalArgumentException 
     */
    private void checkValidEmailAddressOrThrow(String email) throws IllegalArgumentException {
        try {
            new InternetAddress(email, true);
        } catch (AddressException ex) {
            throw new IllegalArgumentException("Field content: '" + email + "' is not a valid email address");
        }
    }

    /**
     * Checks if the supplied filename is a valid filename reference in the TransFile
     * @param fieldContent
     * @throws IllegalArgumentException 
     */
    private void checkValidFileNameFieldOrThrow(String fieldContent) throws IllegalArgumentException {
        if (!fieldContent.matches("[0-9]{6}\\.[0-9a-zA-Z-_.]*")) {
            throw new IllegalArgumentException("File name is not valid: '" + fieldContent + "'");
        }
    }

    /**
     * Checks if the supplied parameter is a valid Initials field
     * @param fieldContent
     * @throws IllegalArgumentException 
     */
    private void checkValidInitialsOrThrow(String fieldContent) throws IllegalArgumentException {
        if (!fieldContent.matches("[0-9a-zA-Z]*")) {
            throw new IllegalArgumentException("File name is not valid: '" + fieldContent + "'");
        }
    }
    
}
