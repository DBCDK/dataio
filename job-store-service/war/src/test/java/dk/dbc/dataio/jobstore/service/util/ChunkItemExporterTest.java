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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.marc.binding.DataField;
import dk.dbc.dataio.marc.binding.Leader;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;
import dk.dbc.dataio.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.dataio.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.dataio.marc.writer.MarcWriterException;
import dk.dbc.dataio.marc.writer.MarcXchangeV1Writer;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ChunkItemExporterTest {
    private final Charset encoding = StandardCharsets.UTF_8;
    private final MarcRecord marcRecord = getMarcRecord();
    private final byte[] marcXchange = getMarcRecordAsMarcXchange(marcRecord);
    private final byte[] danMarc2LineFormat = getMarcRecordAsDanMarc2LineFormat(marcRecord);
    private final ChunkItemExporter chunkItemExporter = new ChunkItemExporter();
    private final ChunkItem chunkItem = new ChunkItemBuilder()
            .setType(ChunkItem.Type.MARCXCHANGE)
            .setData(marcXchange)
            .build();

    @Test
    public void export_chunkItemArgIsNull_throws() throws JobStoreException {
        try {
            chunkItemExporter.export(null, ChunkItem.Type.DANMARC2LINEFORMAT, encoding);
            fail("No NullPointerException thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void export_asTypeArgIsNull_throws() throws JobStoreException {
        try {
            chunkItemExporter.export(chunkItem, null, encoding);
            fail("No NullPointerException thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void export_encodedAsArgIsNull_throws() throws JobStoreException {
        try {
            chunkItemExporter.export(chunkItem, ChunkItem.Type.DANMARC2LINEFORMAT, null);
            fail("No NullPointerException thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void export_illegalConversion_throws() throws JobStoreException {
        try {
            chunkItemExporter.export(chunkItem, ChunkItem.Type.ADDI, encoding);
            fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void export_chunkItemWithMarcXchange_canBeExportedAsDanMarc2LineFormat() throws JobStoreException {
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.DANMARC2LINEFORMAT, encoding);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(danMarc2LineFormat)));
    }

    @Test
    public void export_chunkItemWithUnknownType_canBeExportedAsDanMarc2LineFormat() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(ChunkItem.Type.UNKNOWN)
                .setData(AddiUnwrapperTest.getValidAddi(StringUtil.asString(marcXchange)))
                .build();
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.DANMARC2LINEFORMAT, encoding);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(danMarc2LineFormat)));
    }

    @Test
    public void export_chunkItemWithMarcXchangeWrappedInAddi_canBeExportedAsDanMarc2LineFormat() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(Arrays.asList(ChunkItem.Type.ADDI, ChunkItem.Type.MARCXCHANGE))
                .setData(AddiUnwrapperTest.getValidAddi(StringUtil.asString(marcXchange), StringUtil.asString(marcXchange)))
                .build();
        final byte[] bytes = chunkItemExporter.export(chunkItem, ChunkItem.Type.DANMARC2LINEFORMAT, encoding);
        assertThat(StringUtil.asString(bytes), is(StringUtil.asString(danMarc2LineFormat) + StringUtil.asString(danMarc2LineFormat)));
    }

    private MarcRecord getMarcRecord() {
        final DataField dataField245 = new DataField()
                .setTag("245")
                .setInd1('0')
                .setInd2('0')
                .addSubfield(new SubField()
                    .setCode('a')
                    .setData("A *programmer is born"))
                .addSubfield(new SubField()
                    .setCode('b')
                    .setData("everyday@dbc"));
        return new MarcRecord()
                .setLeader(new Leader().setData(DanMarc2LineFormatReader.DEFAULT_LEADER))
                .addAllFields(Collections.singletonList(dataField245));
    }

    private byte[] getMarcRecordAsMarcXchange(MarcRecord record) {
        final MarcXchangeV1Writer writer = new MarcXchangeV1Writer();
        return writer.write(record, encoding);
    }

    private byte[] getMarcRecordAsDanMarc2LineFormat(MarcRecord record) {
        final DanMarc2LineFormatWriter writer = new DanMarc2LineFormatWriter();
        try {
            return writer.write(record, encoding);
        } catch (MarcWriterException e) {
            throw new IllegalStateException(e);
        }
    }
}