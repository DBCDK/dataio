-- a bit complected select to work around plv8 bug.

CREATE OR REPLACE FUNCTION chunk_component_delete_guard()
    RETURNS TRIGGER AS $$

    var plan=plv8.prepare("select content->'name' as name from flows where content @> $1", ['JSONB']);
    var cursor=plan.cursor( [{"components":[{"id": OLD.id}]}] );

    var row, i=0, usage=[];
    while (row = cursor.fetch()) {
       usage.push(row);
       if( ++i>3 ) {
          break;
       }
    }
    cursor.close();
    plan.free();

    if( usage.length > 0 ) {
      plv8.elog(ERROR, "Error Component " + OLD.content.name + "/" + OLD.id + " is used in " + JSON.stringify(usage) );
    }
    return OLD;
$$ LANGUAGE plv8 STABLE STRICT;


CREATE TRIGGER chunk_component_delete_guard BEFORE DELETE ON flow_components
    FOR EACH ROW EXECUTE PROCEDURE chunk_component_delete_guard();


