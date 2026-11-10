drop table if exists QUESTION;

create table QUESTION
(
    id bigint not null,
    system_from timestamp not null,
    system_to timestamp not null,
    title varchar(255) not null
);
