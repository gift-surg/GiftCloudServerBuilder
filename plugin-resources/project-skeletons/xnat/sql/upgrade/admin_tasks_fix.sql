DELETE FROM xdat_a_xdat_action_type_allowe_xdat_role_type WHERE xdat_action_type_action_name='admin' AND xdat_a_xdat_action_type_allowe_xdat_role_type_id NOT IN(
SELECT xdat_a_xdat_action_type_allowe_xdat_role_type_id FROM xdat_a_xdat_action_type_allowe_xdat_role_type WHERE xdat_action_type_action_name='admin' LIMIT 1);

DELETE FROM xdat_a_xdat_action_type_allowe_xdat_role_type WHERE xdat_action_type_action_name='browse' AND xdat_a_xdat_action_type_allowe_xdat_role_type_id NOT IN(
SELECT xdat_a_xdat_action_type_allowe_xdat_role_type_id FROM xdat_a_xdat_action_type_allowe_xdat_role_type WHERE xdat_action_type_action_name='browse' LIMIT 1);

DELETE FROM xdat_a_xdat_action_type_allowe_xdat_role_type WHERE xdat_action_type_action_name='mr_super_search' AND xdat_a_xdat_action_type_allowe_xdat_role_type_id NOT IN(
SELECT xdat_a_xdat_action_type_allowe_xdat_role_type_id FROM xdat_a_xdat_action_type_allowe_xdat_role_type WHERE xdat_action_type_action_name='mr_super_search' LIMIT 1);

DELETE FROM xdat_a_xdat_action_type_allowe_xdat_role_type WHERE xdat_action_type_action_name='search' AND xdat_a_xdat_action_type_allowe_xdat_role_type_id NOT IN(
SELECT xdat_a_xdat_action_type_allowe_xdat_role_type_id FROM xdat_a_xdat_action_type_allowe_xdat_role_type WHERE xdat_action_type_action_name='search' LIMIT 1);

INSERT INTO xdat_action_type (action_name,display_name,sequence) VALUES
('MyXNAT','My XNAT',7);

INSERT INTO xdat_action_type (action_name,display_name,sequence) VALUES
('XMLUpload','Upload XML',8);

INSERT INTO xdat_a_xdat_action_type_allowe_xdat_role_type (xdat_role_type_role_name,xdat_action_type_action_name)
 VALUES('SiteUser','MyXNAT');

INSERT INTO xdat_a_xdat_action_type_allowe_xdat_role_type (xdat_role_type_role_name,xdat_action_type_action_name)
 VALUES('DataManager','XMLUpload');