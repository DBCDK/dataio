package dk.dbc.dataio.gui.client.exceptions;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum ProxyError implements IsSerializable {
    SERVICE_NOT_FOUND,
    BAD_REQUEST,                // invalid data content
    NOT_ACCEPTABLE,             // violation of unique key contraints
    ENTITY_NOT_FOUND,
    CONFLICT_ERROR,             // Concurrent Update Error
    INTERNAL_SERVER_ERROR,
    MODEL_MAPPER_INVALID_FIELD_VALUE,
    PRECONDITION_FAILED,        // Referenced objects could not be located
    SUBVERSION_LOOKUP_FAILED,   // Error retrieving java scripts
    ERROR_UNKNOWN,              // If the connector throw an unexpected exception
    NO_CONTENT,                 // Successful transaction, but no content results
    FORBIDDEN_SINK_TYPE_TICKLE, // Sink type is invalid for rerun only failed items
    NAMING_ERROR,               // Naming error
    FTP_CONNECTION_ERROR        // Ftp connection error
}
