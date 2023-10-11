package dk.dbc.oclc.wciru;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

    private final static QName _UpdateRequest_QNAME = new QName("http://www.loc.gov/zing/srw/update/", "updateRequest");
    private final static QName _ExplainRequest_QNAME = new QName("http://www.loc.gov/zing/srw/update/", "explainRequest");
    private final static QName _ExplainResponse_QNAME = new QName("http://www.loc.gov/zing/srw/update/", "explainResponse");
    private final static QName _UpdateResponse_QNAME = new QName("http://www.loc.gov/zing/srw/update/", "updateResponse");

    public ObjectFactory() {
    }

    public Diagnostic createDiagnostic() {
        return new Diagnostic();
    }

    public UpdateRequestType createUpdateRequestType() {
        return new UpdateRequestType();
    }

    public ExplainResponseType createExplainResponseType() {
        return new ExplainResponseType();
    }

    public ExplainRequestType createExplainRequestType() {
        return new ExplainRequestType();
    }

    public UpdateResponseType createUpdateResponseType() {
        return new UpdateResponseType();
    }

    public RecordVersionType createRecordVersionType() {
        return new RecordVersionType();
    }

    public ExtraRequestDataType createExtraRequestDataType() {
        return new ExtraRequestDataType();
    }

    public EditReplaceType createEditReplaceType() {
        return new EditReplaceType();
    }

    public ArrayOfTns1RecordVersionType createArrayOfTns1RecordVersionType() {
        return new ArrayOfTns1RecordVersionType();
    }

    public ResponseType createResponseType() {
        return new ResponseType();
    }

    public ExtraDataType createExtraDataType() {
        return new ExtraDataType();
    }

    public RequestType createRequestType() {
        return new RequestType();
    }

    public RecordType createRecordType() {
        return new RecordType();
    }

    public StringOrXmlFragment createStringOrXmlFragment() {
        return new StringOrXmlFragment();
    }

    public DiagnosticsType createDiagnosticsType() {
        return new DiagnosticsType();
    }

    @XmlElementDecl(namespace = "http://www.loc.gov/zing/srw/update/", name = "updateRequest")
    public JAXBElement<UpdateRequestType> createUpdateRequest(UpdateRequestType value) {
        return new JAXBElement<UpdateRequestType>(_UpdateRequest_QNAME, UpdateRequestType.class, null, value);
    }

    @XmlElementDecl(namespace = "http://www.loc.gov/zing/srw/update/", name = "explainRequest")
    public JAXBElement<ExplainRequestType> createExplainRequest(ExplainRequestType value) {
        return new JAXBElement<ExplainRequestType>(_ExplainRequest_QNAME, ExplainRequestType.class, null, value);
    }

    @XmlElementDecl(namespace = "http://www.loc.gov/zing/srw/update/", name = "explainResponse")
    public JAXBElement<ExplainResponseType> createExplainResponse(ExplainResponseType value) {
        return new JAXBElement<ExplainResponseType>(_ExplainResponse_QNAME, ExplainResponseType.class, null, value);
    }

    @XmlElementDecl(namespace = "http://www.loc.gov/zing/srw/update/", name = "updateResponse")
    public JAXBElement<UpdateResponseType> createUpdateResponse(UpdateResponseType value) {
        return new JAXBElement<UpdateResponseType>(_UpdateResponse_QNAME, UpdateResponseType.class, null, value);
    }
}
