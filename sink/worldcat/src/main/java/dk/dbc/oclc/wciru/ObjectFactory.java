/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.oclc.wciru;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
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
