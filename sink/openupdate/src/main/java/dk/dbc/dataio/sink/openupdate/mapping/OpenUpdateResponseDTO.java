package dk.dbc.dataio.sink.openupdate.mapping;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.util.List;

/**
 * This is a DTO class for marshelling and unmarshelling with JAXB hence all the getters and setters are required.
 */
@XmlRootElement
public class OpenUpdateResponseDTO {

    public enum Status {OK, UNKNOWN_ERROR, VALIDATION_ERROR, FAILED_INVALID_AGENCY, FAILED_INVALID_SCHEMA, FAILED_INVALID_OPTION, FAILED_VALIDATION_INTERNAL_ERROR, FAILED_UPDATE_INTERNAL_ERROR}
    private JSONBContext jsonbContext = new JSONBContext();
    private Status status;

    List<OpenUpdateErrorMessageDTO> errorMessages = null;

    public String asJson() throws JSONBException {
        return jsonbContext.marshall(this);
    }

    /**
     *
     * @return                  this DTO and sub DTO's as an XML String
     * @throws JAXBException    if marshelling fails
     */
    public String asXml() throws JAXBException {

        StringWriter stringWriter = new StringWriter();
        final Marshaller marshaller = JAXBContext.newInstance(OpenUpdateResponseDTO.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(this, stringWriter);

        return stringWriter.toString();
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public List<OpenUpdateErrorMessageDTO> getErrorMessages() {
        return errorMessages;
    }
    public void setErrorMessages(List<OpenUpdateErrorMessageDTO> errorMessages) {
        this.errorMessages = errorMessages;
    }
}