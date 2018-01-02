for database in jobstore logstore flowstore filestore phlog; do
    createdb $database
    psql -c "CREATE EXTENSION IF NOT EXISTS plv8;" $database
    psql -c "CREATE EXTENSION IF NOT EXISTS intarray;" $database
done
