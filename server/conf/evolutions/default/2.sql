-- noinspection SqlNoDataSourceInspectionForFile
# --- !Ups

create unique index "USER_NAME_IDX" on "USERS" ("NAME")
;

# --- !Downs

DROP INDEX IF EXISTS  "USER_NAME_IDX";