# Plan: rawrepo-update sink (update-service v3)

Ny sink der sender poster til rawrepo-v3's update-service via REST/JSON.
Den eksisterende `sink/openupdate/` røres ikke.

## Maven-afhængighed

```xml
<dependency>
  <groupId>dk.dbc</groupId>
  <artifactId>update-service-dto</artifactId>
  <version>21.3.4-SNAPSHOT</version>
</dependency>
```

## Forskel på gammel og ny service

| | Gammel (openupdate) | Ny (rawrepo-v3 update-service) |
|---|---|---|
| Protokol | SOAP/JAX-WS | REST/JSON |
| Auth | Basic auth i SOAP-body | IDP-auth i JSON-body (userId/groupId/password) |
| Input-format | ADDI (XML-metadata + MarcXchange XML) | Enkelt JSON-objekt direkte i chunk-item |
| Record-format | MarcXchange XML (JAXB) | MARC JSON (MarcBinding) — ingen konvertering |
| Response | `UpdateRecordResult` med `updateStatus` | `UpdateResponse` med `status` + `errors` |
| Fejlstruktur | `MessageEntry` (type/message/field/subfield pos) | `ValidationMessage` (samme felter — direkte port) |

---

## Trin 1 — Connector-modul ✅

**Placering:** `commons/utils/rawrepo-update-dm3-service-connector/`

- `RawrepoUpdateDm3ServiceConnector` — Jersey-baseret HTTP-klient
  - `updateRecord(UpdateRequest) → UpdateResponse`
  - `validateRecord(UpdateRequest) → UpdateResponse`
- Retry-logik for 404/502/503 i connectoren

---

## Trin 2 — Sink-modul 

**Placering:** `sink/rawrepo-update/`

| Klasse | Ansvar |
|---|---|
| `RawrepoUpdateSinkApp` | Entry point |
| `RawrepoUpdateMessageConsumer` | Hoved-loop |
| `Preprocessor` | Parser JSON-indhold → `RecordEntryDTO` + groupId/template |
| `ChunkItemProcessor` | REST-kald, fejlhåndtering, metrics |
| `UpdateResponseErrorInterpreter` | `ValidationMessage` → `Diagnostic` |
| `RawrepoUpdateConfig` | Config fra flow-store; nulstiller `ignoredValidationErrors` uden for validate-only mode |
| `SinkConfig` | Env-config enum |

---

## Trin 3 — Input-format 

~~Record-format konvertering~~ — ikke relevant.

Chunk-items indeholder ét JSON-objekt direkte (ikke ADDI):

```json
{
  "format":         "dmat",
  "groupId":        "010100",
  "updateTemplate": "DanMarc3",
  "recordData": {
    "leader": ["0","0",...],
    "fields": [...]
  }
}
```

- `groupId` og `updateTemplate` læses fra top-niveau — ingen XML-parsing.
- `recordData` er MarcBinding JSON klar til update-service — ingen konvertering.
- `Preprocessor` udtrækker `bibliographicRecordId` (001a) og `agencyId` (001b) fra `recordData`.

---

## Trin 4 — Fejlfortolkning 

`UpdateResponseErrorInterpreter` er en næsten direkte port af `UpdateRecordErrorInterpreter`.
`ValidationMessage` har præcis de samme felter som den gamle `MessageEntry`.
Ikke-fatal fejl "Posten kan ikke slettes, da den ikke findes" håndteres som i den gamle sink.

---

## Trin 5 — Tests 

- `PreprocessorTest` — unit-tests for JSON-parsing, feltudtræk, deleted-flag
- `ChunkItemProcessorTest` — unit-tests med mockede `Preprocessor` og connector

---

## Trin 6 — Dockerfile 

`src/main/docker/Dockerfile` — identisk struktur med openupdate.

---

## Rækkefølge

- [x] Trin 1: Connector-modul
- [] Trin 2: Sink-modul
- [] Trin 3: Input-format (ADDI og record-format konvertering udgår)
- [] Trin 4: `UpdateResponseErrorInterpreter`
- [] Trin 5: Tests
- [] Trin 6: Dockerfile
