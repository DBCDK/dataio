package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MarcRecordInfoBuilderTest {
    private final MarcRecordInfoBuilder recordInfoBuilder = new MarcRecordInfoBuilder();
    private final String id = "42";
    private final String parent = "42parent";
    private final DataField f001 = get001(id);
    private final DataField f004 = get004("e", "c");    // produces non-delete, standalone
    private final DataField f014 = get014(parent);
    private final ControlField c001 = new ControlField().setTag("001").setData(id);
    private final SinkContent.SequenceAnalysisOption sequenceAnalysisOption = SinkContent.SequenceAnalysisOption.ALL;

    @Test
    public void parse_marcRecordArgIsNull_returnsEmpty() {
        assertThat(recordInfoBuilder.parse(null).isPresent(), is(false));
    }

    @Test
    public void parse_without014() {
        final MarcRecord marcRecord = getMarcRecord(f001, f004);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.STANDALONE));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(false));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(newSet(id)));
    }

    @Test
    public void parse_standaloneWith014_014IsNeverParsed() {
        final MarcRecord marcRecord = getMarcRecord(f001, f004, f014);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.STANDALONE));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(false));
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(newSet(id)));
    }

    @Test
    public void parse_headWith014_014IsNeverParsed() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("h", "c"), f014);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.HEAD));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(false));
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(newSet(id)));
    }

    @Test
    public void parse_sectionWith014() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("s", "c"), f014);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.SECTION));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(true));
        assertThat("getParentRelation()", recordInfo.getParentRelation(), is(parent));
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(newSet(parent, id)));
    }

    @Test
    public void parse_volumeWith014() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("b", "c"), f014);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.VOLUME));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(true));
        assertThat("getParentRelation()", recordInfo.getParentRelation(), is(parent));
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(newSet(parent, id)));
    }

    @Test
    public void parse_empty004a_defaultsToStandaloneType() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("", "c"));
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.STANDALONE));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(newSet(id)));
    }

    @Test
    public void parse_deleteMarked() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("e", "d"));
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("isDelete()", recordInfo.isDelete(), is(true));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(newSet(id)));
    }

    @Test
    public void parse_001ControlField() {
        final MarcRecord marcRecord = getMarcRecord(c001);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        assertThat("getId()", recordInfoOptional.get().getId(), is(id));
    }

    public static MarcRecord getMarcRecord(Field... fields) {
        return new MarcRecord()
                .addAllFields(Arrays.asList(fields));
    }

    public static DataField get001(String a) {
        return new DataField()
                .setTag("001")
                .addSubfield(
                    new SubField()
                        .setCode('f')
                        .setData("danmarc2"))
                .addSubfield(
                    new SubField()
                        .setCode('a')
                        .setData(a));
    }

    public static DataField get004(String a, String r) {
        return new DataField()
                .setTag("004")
                .addSubfield(
                    new SubField()
                        .setCode('a')
                        .setData(a))
                .addSubfield(
                    new SubField()
                        .setCode('r')
                        .setData(r));
    }

    public static DataField get014(String a) {
        return new DataField()
                .setTag("014")
                .addSubfield(
                    new SubField()
                        .setCode('x')
                        .setData("SMS"))
                .addSubfield(
                    new SubField()
                        .setCode('a')
                        .setData(a));
    }

    public static Set<String> newSet(String... strings) {
        return Arrays.stream(strings).collect(Collectors.toSet());
    }
}
