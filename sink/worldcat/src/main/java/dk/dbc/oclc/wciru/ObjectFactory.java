
package dk.dbc.oclc.wciru;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the dk.dbc.oclc.wciru package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _UpdateRequest_QNAME = new QName("http://www.loc.gov/zing/srw/update/", "updateRequest");
    private final static QName _ExplainRequest_QNAME = new QName("http://www.loc.gov/zing/srw/update/", "explainRequest");
    private final static QName _ExplainResponse_QNAME = new QName("http://www.loc.gov/zing/srw/update/", "explainResponse");
    private final static QName _UpdateResponse_QNAME = new QName("http://www.loc.gov/zing/srw/update/", "updateResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: dk.dbc.oclc.wciru
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Diagnostic }
     * 
     */
    public Diagnostic createDiagnostic() {
        return new Diagnostic();
    }

    /**
     * Create an instance of {@link UpdateRequestType }
     * 
     */
    public UpdateRequestType createUpdateRequestType() {
        return new UpdateRequestType();
    }

    /**
     * Create an instance of {@link ExplainResponseType }
     * 
     */
    public ExplainResponseType createExplainResponseType() {
        return new ExplainResponseType();
    }

    /**
     * Create an instance of {@link ExplainRequestType }
     * 
     */
    public ExplainRequestType createExplainRequestType() {
        return new ExplainRequestType();
    }

    /**
     * Create an instance of {@link UpdateResponseType }
     * 
     */
    public UpdateResponseType createUpdateResponseType() {
        return new UpdateResponseType();
    }

    /**
     * Create an instance of {@link RecordVersionType }
     * 
     */
    public RecordVersionType createRecordVersionType() {
        return new RecordVersionType();
    }

    /**
     * Create an instance of {@link ExtraRequestDataType }
     * 
     */
    public ExtraRequestDataType createExtraRequestDataType() {
        return new ExtraRequestDataType();
    }

    /**
     * Create an instance of {@link EditReplaceType }
     * 
     */
    public EditReplaceType createEditReplaceType() {
        return new EditReplaceType();
    }

    /**
     * Create an instance of {@link ArrayOfTns1RecordVersionType }
     * 
     */
    public ArrayOfTns1RecordVersionType createArrayOfTns1RecordVersionType() {
        return new ArrayOfTns1RecordVersionType();
    }

    /**
     * Create an instance of {@link ResponseType }
     * 
     */
    public ResponseType createResponseType() {
        return new ResponseType();
    }

    /**
     * Create an instance of {@link ExtraDataType }
     * 
     */
    public ExtraDataType createExtraDataType() {
        return new ExtraDataType();
    }

    /**
     * Create an instance of {@link RequestType }
     * 
     */
    public RequestType createRequestType() {
        return new RequestType();
    }

    /**
     * Create an instance of {@link RecordType }
     * 
     */
    public RecordType createRecordType() {
        return new RecordType();
    }

    /**
     * Create an instance of {@link StringOrXmlFragment }
     * 
     */
    public StringOrXmlFragment createStringOrXmlFragment() {
        return new StringOrXmlFragment();
    }

    /**
     * Create an instance of {@link DiagnosticsType }
     * 
     */
    public DiagnosticsType createDiagnosticsType() {
        return new DiagnosticsType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.loc.gov/zing/srw/update/", name = "updateRequest")
    public JAXBElement<UpdateRequestType> createUpdateRequest(UpdateRequestType value) {
        return new JAXBElement<UpdateRequestType>(_UpdateRequest_QNAME, UpdateRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExplainRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.loc.gov/zing/srw/update/", name = "explainRequest")
    public JAXBElement<ExplainRequestType> createExplainRequest(ExplainRequestType value) {
        return new JAXBElement<ExplainRequestType>(_ExplainRequest_QNAME, ExplainRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExplainResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.loc.gov/zing/srw/update/", name = "explainResponse")
    public JAXBElement<ExplainResponseType> createExplainResponse(ExplainResponseType value) {
        return new JAXBElement<ExplainResponseType>(_ExplainResponse_QNAME, ExplainResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.loc.gov/zing/srw/update/", name = "updateResponse")
    public JAXBElement<UpdateResponseType> createUpdateResponse(UpdateResponseType value) {
        return new JAXBElement<UpdateResponseType>(_UpdateResponse_QNAME, UpdateResponseType.class, null, value);
    }

}
