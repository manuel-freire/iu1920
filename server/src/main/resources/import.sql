-- 
-- SQL script that will be used to initialize the database
-- Note that the syntax is very picky. A reference is available at
--   http://www.hsqldb.org/doc/guide/sqlgeneral-chapt.html
-- 
-- When writing INSERT statements, the order must be exactly as found in
-- the server logs (search for "create table user"), or as 
-- specified within (creation-compatible) parenthesis:
--     INSERT INTO user(id,enabled,login,password,roles) values (...)
-- vs
--     INSERT INTO user VALUES (...)
-- You can find the expected order by inspecting the output of Hibernate
-- in your server logs (assuming you use Spring + JPA)
--

-- On passwords:
--
-- valid bcrypt-iterated, salted-and-hashed passwords can be generated via
-- https://www.dailycred.com/article/bcrypt-calculator
-- (or any other implementation) and prepending the text '{bcrypt}'
--
-- Note that every time that you generate a bcrypt password with a given 
-- password you will get a different result, because the first characters
-- after the third $ correspond to a random salt that will be different each time.
--
-- a few simple examples:
-- {bcrypt}$2a$04$2ao4NQnJbq3Z6UeGGv24a.wRRX0FGq2l5gcy2Pjd/83ps7YaBXk9C == 'a'
-- {bcrypt}$2a$04$5v02dQ.kxt7B5tJIA4gh3u/JFQlxmoCadSnk76PnvoN35Oz.ge3GK == 'p'
-- {bcrypt}$2a$04$9rrSETFYL/gqiBxBCy3DMOIZ6qmLigzjqnOGbsNji/bt65q.YBfjK == 'q'

-- an admin with password 'a'
-- INSERT INTO user(id,enabled,login,password,roles) VALUES (
-- 	1, 1, 'a',
-- 	'{bcrypt}$2a$04$2ao4NQnJbq3Z6UeGGv24a.wRRX0FGq2l5gcy2Pjd/83ps7YaBXk9C',
-- 	'USER,ADMIN'
-- );

INSERT INTO instance(id) VALUES (1);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    1, 'ZX05e', 'ADMIN', 1, '68181108T', 1, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (2);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    2, 'ZX05e', 'ADMIN', 1, '79963941W', 2, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (3);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    3, 'ZX05e', 'ADMIN', 1, '19125224B', 3, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (4);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    4, 'ZX05e', 'ADMIN', 1, '46885519L', 4, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (5);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    5, 'ZX05e', 'ADMIN', 1, '82414993J', 5, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (6);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    6, 'ZX05e', 'ADMIN', 1, '52253412B', 6, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (7);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    7, 'ZX05e', 'ADMIN', 1, '89462644G', 7, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (8);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    8, 'ZX05e', 'ADMIN', 1, '28345598F', 8, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (9);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    9, 'ZX05e', 'ADMIN', 1, '16337065G', 9, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
INSERT INTO instance(id) VALUES (10);
INSERT INTO user(id, password, roles, enabled, uid, instance_id, first_name, last_name, tels) VALUES(
    10, 'ZX05e', 'ADMIN', 1, '43901720X', 10, 'Francisca', 'Ejemplo Ejémplez', '912-345-987'
);
