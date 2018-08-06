UPDATE harvester_configs SET content = jsonb_set(content, '{harvesterType}', '"STANDARD"') WHERE type = 'dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig';
