UPDATE harvester_configs SET content = jsonb_set(content, '{harvesterType}', '"IMS"') WHERE type = 'dk.dbc.dataio.harvester.types.RRHarvesterConfig' AND content ->> 'imsHarvester' = 'true';
UPDATE harvester_configs SET content = jsonb_set(content, '{harvesterType}', '"WORLDCAT"') WHERE type = 'dk.dbc.dataio.harvester.types.RRHarvesterConfig' AND content ->> 'worldCatHarvester' = 'true';
UPDATE harvester_configs SET content = jsonb_set(content, '{harvesterType}', '"STANDARD"') WHERE type = 'dk.dbc.dataio.harvester.types.RRHarvesterConfig' AND content -> 'harvesterType' is null;

UPDATE harvester_configs SET content = content - 'worldCatHarvester'  WHERE content -> 'worldCatHarvester' IS NOT NULL;
UPDATE harvester_configs SET content = content - 'imsHarvester'  WHERE content -> 'imsHarvester' IS NOT NULL;