--
-- PostgreSQL database dump
--

-- Dumped from database version 11.14
-- Dumped by pg_dump version 11.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: migrations; Type: SCHEMA; Schema: -; Owner: user
--

CREATE SCHEMA migrations;


ALTER SCHEMA migrations OWNER TO "user";

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: migrations; Owner: user
--

CREATE TABLE migrations.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE migrations.flyway_schema_history OWNER TO "user";

--
-- Name: test_table; Type: TABLE; Schema: migrations; Owner: user
--

CREATE TABLE migrations.test_table (
    id character varying(16) NOT NULL,
    variable1 character varying(16) NOT NULL,
    variable2 character varying(16) NOT NULL,
    added_column character varying(16)
);


ALTER TABLE migrations.test_table OWNER TO "user";

--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: migrations; Owner: user
--

COPY migrations.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init	SQL	V1__init.sql	-340853948	user	2022-08-18 17:48:28.343177	6	t
2	2	add column	SQL	V2__add_column.sql	-604825677	user	2022-08-18 17:48:28.359202	4	t
3	3	populate	SQL	V3__populate_first_row.sql	838182787	user	2022-08-18 17:48:28.371983	4	t
4	4	populate	SQL	V4__populate_second_row.sql	-585163400	user	2022-08-18 17:48:28.386723	3	t
5	5	populate	SQL	V5__populate_dev.sql	-173798623	user	2022-08-18 17:48:28.399938	4	t
\.


--
-- Data for Name: test_table; Type: TABLE DATA; Schema: migrations; Owner: user
--

COPY migrations.test_table (id, variable1, variable2, added_column) FROM stdin;
1	sth	sth2	\N
2	after dump	after dump	\N
3	dev	dev	\N
\.


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: migrations; Owner: user
--

ALTER TABLE ONLY migrations.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: test_table test_table_pkey; Type: CONSTRAINT; Schema: migrations; Owner: user
--

ALTER TABLE ONLY migrations.test_table
    ADD CONSTRAINT test_table_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: migrations; Owner: user
--

CREATE INDEX flyway_schema_history_s_idx ON migrations.flyway_schema_history USING btree (success);


--
-- PostgreSQL database dump complete
--

