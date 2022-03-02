--- MS-2888 Fjern mulighed for at sende filer til det gamle Posthus
ALTER TABLE gatekeeper_destinations DROP COLUMN copytoposthus;
ALTER TABLE gatekeeper_destinations DROP COLUMN notifyfromposthus;
