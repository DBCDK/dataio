# DataIO harvesters

- DataIO harvesters are responsible for retrieving data from various sources both internal and external to DBC.
- Harvesters periodically fetches their configurations using the flow-store-service REST API in a scheduled EJB bean.
- Harvesters create jobs using the job-store-service REST API.

