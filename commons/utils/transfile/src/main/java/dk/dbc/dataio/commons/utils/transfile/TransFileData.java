package dk.dbc.dataio.commons.utils.transfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Trans File Data element, which is one line in the Trans File,
 * each line containing multiple Trans File Fields
 */
public class TransFileData {

    private final Map<TransFileField.TransFileFieldId, String> data = new HashMap<>();
    
    /**
     * Accumulates single Trans File Fields in the class from the input string
     * 
     * @param transFileData 
     * @throws IllegalArgumentException
     */
    public TransFileData(String transFileData) throws IllegalArgumentException {
        TransFileField field;

        if (transFileData.isEmpty()) {
            return;  // An empty input line is legal
        }
        String[] split = transFileData.split(",");
        if (split.length < 1) {
            throw new IllegalArgumentException("Empty input is not allowed");
        }
        checkCorrectTransFileFieldIdOrThrow(split[0], TransFileField.TransFileFieldId.BASE_NAME);
        for (String element: split) {
            field = new TransFileField(element);
            if (data.containsKey(field.getKey())) {
                throw new IllegalArgumentException("Duplicate fields are not allowed");
            }
            data.put(field.getKey(), field.getContent());
        }
        checkMandatoryFieldOrThrow(TransFileField.TransFileFieldId.BASE_NAME);
        checkMandatoryFieldOrThrow(TransFileField.TransFileFieldId.FILE_NAME);
        checkMandatoryFieldOrThrow(TransFileField.TransFileFieldId.PRIMARY_EMAIL_ADDRESS);
    }
    
    /**
     * Gets the Basename for the TransFile data element
     * @return The basename
     */
    public String getBaseName() {
        return data.get(TransFileField.TransFileFieldId.BASE_NAME);
    }
    
    /**
     * Gets the Filename for the TransFile data element
     * @return The filename
     */
    public String getFileName() {
        return data.get(TransFileField.TransFileFieldId.FILE_NAME);
    }

    /**
     * Gets the submitter number for the TransFile data element
     * @return submitter number
     */
    public long getSubmitterNumber() {
        final String filename = data.get(TransFileField.TransFileFieldId.FILE_NAME);
        return Long.valueOf(filename.substring(0, filename.indexOf(".")));
    }
    
    /**
     * Gets the Technical Protocol for the TransFile data element
     * @return The Technical Protocol
     */
    public String getTechnicalProtocol() {
        return data.get(TransFileField.TransFileFieldId.TECHNICAL_PROTOCOL);
    }
    
    /**
     * Gets the Character Set for the TransFile data element
     * @return The Character Set
     */
    public String getCharacterSet() {
        return data.get(TransFileField.TransFileFieldId.CHARACTER_SET);
    }
    
    /**
     * Gets the Library Format for the TransFile data element
     * @return The Library Format
     */
    public String getLibraryFormat() {
        return data.get(TransFileField.TransFileFieldId.LIBRARY_FORMAT);
    }
    
    /**
     * Gets the Primary Email Address for the TransFile data element
     * @return The Primary Email Address
     */
    public String getPrimaryEmailAddress() {
        return data.get(TransFileField.TransFileFieldId.PRIMARY_EMAIL_ADDRESS);
    }
    
    /**
     * Gets the Secondary Email Address for the TransFile data element
     * @return The Secondary Email Address
     */
    public String getSecondaryEmailAddress() {
        return data.get(TransFileField.TransFileFieldId.SECONDARY_EMAIL_ADDRESS);
    }
    
    /**
     * Gets the Initials for the TransFile data element
     * @return The Initials
     */
    public String getInitials() {
        return data.get(TransFileField.TransFileFieldId.INITIALS);
    }
    
    
    // Private methods

    /**
     * Checks if the accumulated data contains a mandatory field. If not, an exception is thrown
     * @param mandatoryFieldId 
     * @throws IllegalArgumentException
     */
    private void checkMandatoryFieldOrThrow(TransFileField.TransFileFieldId mandatoryFieldId) throws IllegalArgumentException {
        if (!data.containsKey(mandatoryFieldId)) {
            throw new IllegalArgumentException("A mandatory field '" + mandatoryFieldId + "' is missing");
        }
    }

    /**
     * Checks that the supplied string contains a key as supplied 
     * @param fieldString 
     * @throws IllegalArgumentException
     */
    private void checkCorrectTransFileFieldIdOrThrow(String fieldString, TransFileField.TransFileFieldId matchKey) throws IllegalArgumentException {
        TransFileField field = new TransFileField(fieldString);
        if (field.getKey() != matchKey) {
            throw new IllegalArgumentException("Field 'b' must be the first field in the line");
        }
    }
    
}
