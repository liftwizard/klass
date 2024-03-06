drop table if exists PROJECTION_REFERENCE_PROPERTY;

create table PROJECTION_REFERENCE_PROPERTY
(
    name varchar(256) not null,
    id bigint not null,
    parent_id bigint,
    association_end_class varchar(256) not null,
    association_end_name varchar(256) not null,
    ordinal int not null
);

