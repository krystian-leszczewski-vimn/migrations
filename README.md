### TODO
- create dump of every db till migration V7
- enable tests to run on every env
- verify works as desired
---


### Problem:
Tests run too long for migrations. 

### Goal:
Create fast tests.

Solution:
On tests:
* run container with postgres and bump of X migrations as baseline.
* apply dump against test db
* run test app on dev, qa, prod

1. Run postgres in container
```shell
docker-compose stop && docker-compose rm -f && docker-compose build && docker-compose up -d
```

2. Run migration inside container
```shell
docker exec -it migrations-db-1 sh
psql -U user migrations < V7__snapshot.pgsql
```

---
### Dumping db
```shell
pg_dump -U user migrations > /snapshot/B7__{{env}}_dump.pgsql
```