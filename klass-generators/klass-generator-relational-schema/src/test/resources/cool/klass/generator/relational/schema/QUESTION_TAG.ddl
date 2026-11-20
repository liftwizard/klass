drop table if exists QUESTION_TAG;

create table QUESTION_TAG
(
    question_id bigint not null,
    name varchar(255) not null
);
