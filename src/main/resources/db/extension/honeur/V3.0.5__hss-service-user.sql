CREATE TABLE hss_service_user (
  present   VARCHAR(1)    UNIQUE CONSTRAINT only_one_value CHECK (present='X'),
  username  VARCHAR(255)  CONSTRAINT hss_service_user_pk PRIMARY KEY,
  password  VARCHAR(255)
);

grant select, insert, update, delete on hss_service_user to ohdsi_app;

INSERT INTO ${ohdsiSchema}.SEC_PERMISSION (id, value, description)
  VALUES (nextval('${ohdsiSchema}.sec_permission_id_seq'), 'hss:user:post', 'Change service user for HSS.');

INSERT INTO ${ohdsiSchema}.SEC_ROLE_PERMISSION (ID, ROLE_ID, PERMISSION_ID, STATUS)
  VALUES (
    nextval('${ohdsiSchema}.sec_role_permission_sequence'),
    (
      SELECT r.id FROM ${ohdsiSchema}.SEC_ROLE r WHERE r.name = 'HONEUR-local'
    ),
    (
      SELECT p.id FROM ${ohdsiSchema}.SEC_PERMISSION p WHERE p.value = 'hss:user:post'
    ),NULL );