drop table if exists QUESTION_VOTE;

create table QUESTION_VOTE
(
    question_id bigint not null,
    user_id varchar(255) not null,
    system_from timestamp not null,
    system_to timestamp not null
);
