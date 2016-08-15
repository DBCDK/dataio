UPDATE sinks
SET content = REPLACE(content::TEXT, '"sinkConfig": null', '"sinkConfig": {"@class": "dk.dbc.dataio.commons.types.EsSinkConfig", "userId": 3, "esAction": "INSERT", "databaseName": "default"}')::jsonb
WHERE content->>'sinkType' = 'ES'
AND (content->>'sinkConfig') IS null;