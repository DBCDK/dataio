UPDATE job SET specification=dbc_jsonb_append_key_value(specification, 'type', 'TRANSIENT');
