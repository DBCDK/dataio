UPDATE harvester_configs SET content = (c.content - 'openAgencyTarget') FROM (SELECT content FROM harvester_configs) AS c;
