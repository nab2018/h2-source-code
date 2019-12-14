-- Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
-- and the EPL 1.0 (https://h2database.com/html/license.html).
-- Initial Developer: H2 Group
--

CREATE TABLE TEST(A INT CONSTRAINT PK_1 PRIMARY KEY);
> ok

SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS;
> CONSTRAINT_NAME CONSTRAINT_TYPE
> --------------- ---------------
> PK_1            PRIMARY KEY
> rows: 1

DROP TABLE TEST;
> ok

CREATE TABLE TEST(ID IDENTITY, CONSTRAINT PK_1 PRIMARY KEY(ID));
> ok

SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS;
> CONSTRAINT_NAME CONSTRAINT_TYPE
> --------------- ---------------
> PK_1            PRIMARY KEY
> rows: 1

DROP TABLE TEST;
> ok

CREATE TABLE T1(ID INT PRIMARY KEY, COL2 INT);
> ok

INSERT INTO T1 VALUES (1, 2), (11, 22);
> update count: 2

CREATE TABLE T2 AS SELECT * FROM T1;
> ok

SELECT * FROM T2 ORDER BY ID;
> ID COL2
> -- ----
> 1  2
> 11 22
> rows (ordered): 2

DROP TABLE T2;
> ok

CREATE TABLE T2 AS SELECT * FROM T1 WITH DATA;
> ok

SELECT * FROM T2 ORDER BY ID;
> ID COL2
> -- ----
> 1  2
> 11 22
> rows (ordered): 2

DROP TABLE T2;
> ok

CREATE TABLE T2 AS SELECT * FROM T1 WITH NO DATA;
> ok

SELECT * FROM T2 ORDER BY ID;
> ID COL2
> -- ----
> rows (ordered): 0

DROP TABLE T2;
> ok

DROP TABLE T1;
> ok

CREATE TABLE TEST(A INT, B INT INVISIBLE);
> ok

SELECT * FROM TEST;
> A
> -
> rows: 0

SELECT A, B FROM TEST;
> A B
> - -
> rows: 0

SELECT COLUMN_NAME, IS_VISIBLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'TEST' ORDER BY ORDINAL_POSITION;
> COLUMN_NAME IS_VISIBLE
> ----------- ----------
> A           TRUE
> B           FALSE
> rows (ordered): 2

DROP TABLE TEST;
> ok

CREATE TABLE TEST1(ID IDENTITY);
> ok

CREATE TABLE TEST2(ID BIGINT IDENTITY);
> ok

CREATE TABLE TEST3(ID BIGINT GENERATED BY DEFAULT AS IDENTITY);
> ok

SELECT CONSTRAINT_TYPE, TABLE_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = 'PUBLIC';
> CONSTRAINT_TYPE TABLE_NAME
> --------------- ----------
> PRIMARY KEY     TEST1
> PRIMARY KEY     TEST2
> rows: 2

DROP TABLE TEST1, TEST2, TEST3;
> ok

CREATE TABLE TEST(A);
> exception UNKNOWN_DATA_TYPE_1

CREATE TABLE TEST(A, B, C) AS SELECT 1, 2, CAST ('A' AS VARCHAR);
> ok

SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'TEST';
> COLUMN_NAME COLUMN_TYPE
> ----------- -----------
> A           INTEGER
> B           INTEGER
> C           VARCHAR
> rows: 3

DROP TABLE TEST;
> ok

CREATE MEMORY TABLE TEST(A INT, B INT GENERATED ALWAYS AS (1), C INT GENERATED ALWAYS AS (B + 1));
> ok

SCRIPT NOPASSWORDS NOSETTINGS TABLE TEST;
> SCRIPT
> -----------------------------------------------------------------------------------------------------------------------
> -- 0 +/- SELECT COUNT(*) FROM PUBLIC.TEST;
> CREATE MEMORY TABLE "PUBLIC"."TEST"( "A" INT, "B" INT GENERATED ALWAYS AS (1), "C" INT GENERATED ALWAYS AS ("B" + 1) );
> CREATE USER IF NOT EXISTS "SA" PASSWORD '' ADMIN;
> rows: 3

DROP TABLE TEST;
> ok

CREATE TABLE TEST(A INT GENERATED BY DEFAULT AS (1));
> exception SYNTAX_ERROR_2

CREATE TABLE TEST(A IDENTITY GENERATED ALWAYS AS (1));
> exception SYNTAX_ERROR_2

CREATE TABLE TEST(A IDENTITY AS (1));
> exception SYNTAX_ERROR_2
