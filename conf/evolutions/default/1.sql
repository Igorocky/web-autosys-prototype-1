-- noinspection SqlNoDataSourceInspectionForFile
# --- !Ups

create table INTPROPS (KEY VARCHAR NOT NULL PRIMARY KEY,VALUE INTEGER NOT NULL);

# --- !Downs

DROP TABLE INTPROPS;