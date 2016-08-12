CREATE TABLE progressWal (
  configId        BIGINT,
  configVersion   BIGINT,
  harvestedFrom   TIMESTAMP,
  harvestedUntil  TIMESTAMP,
  PRIMARY KEY (configId)
);