-- noinspection SqlNoDataSourceInspectionForFile
# --- !Ups

create table "USERS" (
  "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL PRIMARY KEY,
  "NAME" VARCHAR(50) NOT NULL
)
;

# --- !Downs

DROP TABLE USERS;