package dk.dbc.dataio.sink.openupdate.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.oss.ns.catalogingupdate.ValidateEntry;
import dk.dbc.oss.ns.catalogingupdate.ValidateInstance;
import dk.dbc.oss.ns.catalogingupdate.ValidateWarningOrErrorEnum;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ValidateEntryMapperTest {

    // the response received from the OpenUpdate web service
    private UpdateRecordResult dummyUpdateRecordResult_ok;
    private UpdateRecordResult dummyUpdateRecordResult_validation;

    JSONBContext jsonbContext = new JSONBContext();

    @Before
    public void setUp() throws Exception {

        dummyUpdateRecordResult_ok = new UpdateRecordResult();
        dummyUpdateRecordResult_ok.setUpdateStatus(UpdateStatusEnum.OK);

        dummyUpdateRecordResult_validation = new UpdateRecordResult();
        dummyUpdateRecordResult_validation.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);

        ValidateInstance validateInstance = new ValidateInstance();
        validateInstance.getValidateEntry().add(buildValidateEntry(ValidateWarningOrErrorEnum.ERROR, 1l, 1l, "Værdien '191919' er ikke end del af de valide værdier: '870970'"));
        validateInstance.getValidateEntry().add(buildValidateEntry(ValidateWarningOrErrorEnum.ERROR, 1l, 1l, "Delfelt 'a' mangler i felt '001'"));
        validateInstance.getValidateEntry().add(buildValidateEntry(ValidateWarningOrErrorEnum.ERROR, 1l, 1l, "Delfelt 'b' er gentaget '2' gange i feltet"));
        dummyUpdateRecordResult_validation.setValidateInstance(validateInstance);
    }

    private ValidateEntry buildValidateEntry(ValidateWarningOrErrorEnum validateWarningOrErrorEnum, long ordinalPositionOfField, long ordinalPositionOfSubField, String message) {
        ValidateEntry validateEntry = new ValidateEntry();
        validateEntry.setWarningOrError(validateWarningOrErrorEnum);
        validateEntry.setOrdinalPositionOfField(new BigInteger(String.valueOf(ordinalPositionOfField)));
        validateEntry.setOrdinalPositionOfSubField(new BigInteger(String.valueOf(ordinalPositionOfSubField)));
        validateEntry.setMessage(message);
        validateEntry.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);

        return validateEntry;
    }

    @Test
    public void testMap_ok() throws Exception {

        OpenUpdateResponseDTO openUpdateResponseDTO = new UpdateRecordResponseMapper<UpdateRecordResult>(this.dummyUpdateRecordResult_ok).map();
        final String json = jsonbContext.marshall(openUpdateResponseDTO);
        assertNotNull(json);

        final JsonNode jsonTree = jsonbContext.getJsonTree(json);
        final List<JsonNode> status = jsonTree.findValues("status");

        assertNotNull(status);
        assertTrue(status.size() == 1);
        assertEquals("OK", status.get(0).asText());
    }

    @Test
    public void testMap_validationError() throws Exception {

        OpenUpdateResponseDTO openUpdateResponseDTO = new UpdateRecordResponseMapper<UpdateRecordResult>(this.dummyUpdateRecordResult_validation).map();
        final String json = jsonbContext.marshall(openUpdateResponseDTO);
        assertNotNull(json);
        System.out.println(json);

        final JsonNode jsonTree = jsonbContext.getJsonTree(json);
        final List<JsonNode> status = jsonTree.findValues("status");

        // Assert status
        assertNotNull(status);
        assertTrue(status.size() == 1);
        assertEquals("VALIDATION_ERROR", status.get(0).asText());

        final List<JsonNode> errorMessagesWrapper = jsonTree.findValues("errorMessages");
        assertTrue(errorMessagesWrapper.size() == 1);

        final JsonNode errorMessages = errorMessagesWrapper.get(0);
        assertTrue(errorMessages.size() == 3);

        for (JsonNode node : errorMessages) {
            assertEquals("ERROR", node.findValues("type").get(0).asText());
            assertEquals(1l, node.findValues("ordinalPositionOfField").get(0).asLong());
            assertEquals(1l, node.findValues("ordinalPositionOfSubField").get(0).asLong());
            assertTrue(!node.findValues("errorMessage").get(0).asText().isEmpty());
        }
    }
}