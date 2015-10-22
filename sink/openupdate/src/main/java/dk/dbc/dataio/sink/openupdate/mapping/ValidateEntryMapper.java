package dk.dbc.dataio.sink.openupdate.mapping;

import dk.dbc.oss.ns.catalogingupdate.ValidateEntry;
import dk.dbc.oss.ns.catalogingupdate.ValidateWarningOrErrorEnum;

import java.math.BigInteger;

/**
 * In case the OpenUpdate webservice response contains error messages this ValidateEntryMapper
 * can do the mapping between 1 webservice error message to the OpenUpdateErrorMessageDTO.
 *
 * @param <OpenUpdateWebServiceErrorMessage>    ValidateEntry received from the Open Update web service in case of errors
 */
public class ValidateEntryMapper<OpenUpdateWebServiceErrorMessage extends ValidateEntry> {

    private OpenUpdateWebServiceErrorMessage openUpdateWebServiceErrorMessage;

    /**
     * @param openUpdateWebServiceErrorMessage  ValidateEntry received from the OpenUpdate web service
     */
    public ValidateEntryMapper(OpenUpdateWebServiceErrorMessage openUpdateWebServiceErrorMessage) {
        this.openUpdateWebServiceErrorMessage = openUpdateWebServiceErrorMessage;
    }

    /**
     * map is responsible for mapping the web service response to the DTO
     * @return  OpenUpdateErrorMessagesDTO which can generate JSON
     */
    public OpenUpdateErrorMessageDTO map() {

        OpenUpdateErrorMessageDTO dto = null;
        if(openUpdateWebServiceErrorMessage != null) {

            dto = new OpenUpdateErrorMessageDTO();

            dto.setType(mapType(openUpdateWebServiceErrorMessage.getWarningOrError()));
            dto.setOrdinalPositionOfField(mapBigIntegerToLong(openUpdateWebServiceErrorMessage.getOrdinalPositionOfField()));
            dto.setOrdinalPositionOfSubField(mapBigIntegerToLong(openUpdateWebServiceErrorMessage.getOrdinalPositionOfSubField()));
            dto.setErrorMessage(openUpdateWebServiceErrorMessage.getMessage());

        }

        return dto;
    }

    private OpenUpdateErrorMessageDTO.ErrorType mapType(ValidateWarningOrErrorEnum warningOrError) {

        if(warningOrError != null) {
            switch (warningOrError) {
                case ERROR      : return OpenUpdateErrorMessageDTO.ErrorType.ERROR;
                case WARNING    : return OpenUpdateErrorMessageDTO.ErrorType.WARNING;
            }
        }

        return null;
    }

    private Long mapBigIntegerToLong(BigInteger valueToMap) {
        return valueToMap != null ? valueToMap.longValueExact() : null;
    }
}