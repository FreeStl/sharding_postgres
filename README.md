# Description

Comparison between 3 types of Postges sharding: single node (no sharding), PWD, Citrus multi node sharding.

# Single node

1) Use single/docker-compose.yml to run container
2) Run app in src folder. It will test insertion speed of 10k records and select all speed;
```postgresql
INSERT INTO customer(id, category, first_name, last_name)
VALUES (1, 1, 'F', 'L');
...
SELECT  * FROM customers;
```

# PWD

1) Use pwd_sharding/docker-compose.yml to run containers
2) prepare master
```postgresql
CREATE TABLE customer (
id int PRIMARY KEY,
category int NOT NULL,
first_name VARCHAR (50) NOT NULL,
last_name VARCHAR (50) NOT NULL
);

CREATE EXTENSION postgres_fdw;

CREATE SERVER customer_1_server
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS( host 'host.docker.internal', port '5433', dbname 'postgres');

CREATE USER MAPPING FOR postgres
SERVER customer_1_server
OPTIONS (user 'postgres', password 'example');

CREATE SERVER customer_2_server
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS( host 'host.docker.internal', port '5434', dbname 'postgres');

CREATE USER MAPPING FOR postgres
SERVER customer_2_server
OPTIONS (user 'postgres', password 'example');

CREATE FOREIGN TABLE customer_1 (
id int NOT NULL,
category int NOT NULL,
first_name VARCHAR (50) NOT NULL,
last_name VARCHAR (50) NOT NULL)
SERVER customer_1_server
OPTIONS (schema_name 'public', table_name 'customer');

CREATE FOREIGN TABLE customer_2 (
id int NOT NULL,
category int NOT NULL,
first_name VARCHAR (50) NOT NULL,
last_name VARCHAR (50) NOT NULL)
SERVER customer_2_server
OPTIONS (schema_name 'public', table_name 'customer');


CREATE VIEW customers AS
SELECT * FROM customer_1
UNION ALL
SELECT *FROM customer_2;


CREATE RULE customer_insert AS ON INSERT TO customer
DO INSTEAD NOTHING;
CREATE RULE customer_update AS ON UPDATE TO customer
DO INSTEAD NOTHING;
CREATE RULE customer_delete AS ON DELETE TO customer
DO INSTEAD NOTHING;


CREATE RULE customer_insert_to_1 AS ON INSERT TO customer
WHERE ( category = 1 )
DO INSTEAD INSERT INTO customer_1 VALUES (NEW.*);
CREATE RULE customer_insert_to_2 AS ON INSERT TO customer
WHERE ( category = 2 )
DO INSTEAD INSERT INTO customer_2 VALUES (NEW.*);
```
3) prepare node 1
```postgresql
CREATE TABLE customer (
id int PRIMARY KEY,
category int NOT NULL,
first_name VARCHAR (50) NOT NULL,
last_name VARCHAR (50) NOT NULL,
CONSTRAINT category_check CHECK ( category = 1 )
);

CREATE INDEX category_idx ON customer USING btree(category);
```
4) prepare node 1
```postgresql
CREATE TABLE customer (
id int PRIMARY KEY,
category int NOT NULL,
first_name VARCHAR (50) NOT NULL,
last_name VARCHAR (50) NOT NULL,
CONSTRAINT category_check CHECK ( category = 2 )
);

CREATE INDEX category_idx ON customer USING btree(category);
```
5) Run app in src folder. It will test insertion speed of 10k records and select all speed;
```postgresql
INSERT INTO customer(id, category, first_name, last_name)
VALUES (1, 1, 'F', 'L');

SELECT  * FROM customer;
```

# Citus Multi Node

1) run citus/docker-compose.yml. It automatically bounds master node and workers
2) Create table in master:
```postgresql
    CREATE TABLE customer (
    id int NOT NULL,
    category int NOT NULL,
    first_name VARCHAR (50) NOT NULL,
    last_name VARCHAR (50) NOT NULL,
    PRIMARY KEY(id, category)
    )
    partition by range (category);
```
3) Set up partitioning to workers:
```postgresql
CREATE TABLE customer1 PARTITION OF customer FOR VALUES FROM (1) TO (2);
CREATE TABLE customer2 PARTITION OF customer FOR VALUES FROM (2) TO (3);
```

# Comparison

| Sharding type     | Write 10k records, ms | Select all 10k, ms |
|-------------------|-----------------------|--------------------|
| Single node       | 26120                 | 35                 |
| PWD               | 56466                 | 108                |
| Citrus multi node | 27433                 | 63                 |



