/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.oss.ns.catalogingupdate.ValidateEntry;
import dk.dbc.oss.ns.catalogingupdate.ValidateInstance;
import dk.dbc.oss.ns.catalogingupdate.ValidateWarningOrErrorEnum;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.math.BigInteger;

import static junit.framework.TestCase.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UpdateRecordResultMarshallerTest {

    @Test
    public void callAsXml_updateRecordResultIsNull_ok() {
        try {
            new UpdateRecordResultMarshaller().asXml(null);
        } catch (JAXBException e) {
            fail("Unexpected Exception thrown");
        }
    }

    @Test
    public void callAsXml_updateRecordResultIsNotNull_ok() throws JAXBException {
        final ValidateEntry validateEntry = new ValidateEntry();
        validateEntry.setMessage("UpdateRecordResultMarshallerTest validation error");
        validateEntry.setOrdinalPositionOfField(BigInteger.ONE);
        validateEntry.setOrdinalPositionOfSubField(BigInteger.ONE);
        validateEntry.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);

        final ValidateInstance validateInstance = new ValidateInstance();
        validateInstance.getValidateEntry().add(validateEntry);

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        updateRecordResult.setValidateInstance(validateInstance);
        assertThat(new UpdateRecordResultMarshaller().asXml(updateRecordResult), is(getExpectedUpdateRecordResultAsXml()));
    }

    private String getExpectedUpdateRecordResultAsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<UpdateRecordResult xmlns=\"http://oss.dbc.dk/ns/catalogingUpdate\">" +
                    "<updateStatus>validation_error</updateStatus>" +
                    "<validateInstance>" +
                        "<validateEntry>" +
                            "<warningOrError>error</warningOrError>" +
                            "<ordinalPositionOfField>1</ordinalPositionOfField>" +
                            "<ordinalPositionOfSubField>1</ordinalPositionOfSubField>" +
                            "<message>UpdateRecordResultMarshallerTest validation error</message>" +
                        "</validateEntry>" +
                    "</validateInstance>" +
                "</UpdateRecordResult>";
    }
}
