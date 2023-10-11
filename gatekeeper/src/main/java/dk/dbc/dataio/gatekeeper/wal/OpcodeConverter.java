package dk.dbc.dataio.gatekeeper.wal;

import dk.dbc.dataio.gatekeeper.operation.Opcode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OpcodeConverter implements AttributeConverter<Opcode, String> {
    @Override
    public String convertToDatabaseColumn(Opcode opcode) {
        return opcode.name();
    }

    @Override
    public Opcode convertToEntityAttribute(String opcodeName) {
        return Opcode.valueOf(opcodeName);
    }
}
