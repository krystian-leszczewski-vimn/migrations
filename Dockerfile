FROM postgres:11.14-alpine
ADD src/main/resources/scripts/initialize-db.sql /docker-entrypoint-initdb.d/1_initialize-db.sql
