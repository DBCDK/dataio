package dk.dbc.dataio.sink.openupdate.mapping;

import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.oss.ns.catalogingupdate.ValidateEntry;
import dk.dbc.oss.ns.catalogingupdate.ValidateInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This mapper is responsible for mapping between a UpdateRecordResult received from the OpenUpdate
 * web service and the JSON OpenUpdateResonseDTO.
 * The OpenUpdateResponseDTO can generate JSON by calling asJson method and this is persistable in the data-part of an ExternalChunk.
 *
 * @param <OpenUpdateWebServiceResponse>    UpdateRecordResult received from the web service.
 */
public class UpdateRecordResponseMapper<OpenUpdateWebServiceResponse extends UpdateRecordResult> {

    private OpenUpdateWebServiceResponse openUpdateWebServiceResponse;

    /**
     * @param openUpdateWebServiceResponse  UpdateRecordResult received from the OpenUpdate web service
     */
    public UpdateRecordResponseMapper(OpenUpdateWebServiceResponse openUpdateWebServiceResponse) {
        this.openUpdateWebServiceResponse = openUpdateWebServiceResponse;
    }

    /**
     * map is responsible for mapping the web service response to the DTO
     * @param trackingId trackingId to trace a record to an error response in OpenUpdate webservice
     * @return OpenUpdateRosponseDTO which can generate JSON
     */
    public OpenUpdateResponseDTO mapToDto(UUID trackingId) {

        OpenUpdateResponseDTO dto = null;

        if(openUpdateWebServiceResponse != null) {

            dto = new OpenUpdateResponseDTO(trackingId);
            this.mapStatus(openUpdateWebServiceResponse.getUpdateStatus(), dto);

            if(dto.getStatus() != OpenUpdateResponseDTO.Status.OK) {
                mapErrorData(openUpdateWebServiceResponse.getValidateInstance(), dto);
            }
        }

        return dto;
    }

    private void mapStatus(UpdateStatusEnum updateStatus, OpenUpdateResponseDTO dto) {

        OpenUpdateResponseDTO.Status status;
        switch (updateStatus) {

            case OK :                               status = OpenUpdateResponseDTO.Status.OK;                                     break;
            case VALIDATION_ERROR :                 status = OpenUpdateResponseDTO.Status.VALIDATION_ERROR;                       break;
            case FAILED_INVALID_AGENCY :            status = OpenUpdateResponseDTO.Status.FAILED_INVALID_AGENCY;                  break;
            case FAILED_INVALID_SCHEMA :            status = OpenUpdateResponseDTO.Status.FAILED_INVALID_SCHEMA;                  break;
            case FAILED_INVALID_OPTION :            status = OpenUpdateResponseDTO.Status.FAILED_INVALID_OPTION;                  break;
            case FAILED_VALIDATION_INTERNAL_ERROR : status = OpenUpdateResponseDTO.Status.FAILED_VALIDATION_INTERNAL_ERROR;       break;
            case FAILED_UPDATE_INTERNAL_ERROR :     status = OpenUpdateResponseDTO.Status.FAILED_UPDATE_INTERNAL_ERROR;           break;
            default:                                status = OpenUpdateResponseDTO.Status.UNKNOWN_ERROR;
        }

        dto.setStatus(status);
    }

    private void mapErrorData(ValidateInstance validateInstance, OpenUpdateResponseDTO dto) throws IllegalStateException {

        // An error/warning is received from the OpenUpdate webservice, but no error data exists
        if(validateInstance == null || validateInstance.getValidateEntry() == null || validateInstance.getValidateEntry().isEmpty()) {
            throw new IllegalStateException("An error is received form the OpenUpdate webservice, but no error data exists!");
        } else {

            List<OpenUpdateErrorMessageDTO> errorMessages = new ArrayList<>();

            for (ValidateEntry validateEntry : validateInstance.getValidateEntry()) {
                errorMessages.add(new ValidateEntryMapper(validateEntry).map());
            }

            dto.setErrorMessages(errorMessages);
        }
    }
}