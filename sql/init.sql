CREATE DATABASE board;

\ c board;

CREATE TABLE jobs(
    id uuid DEFAULT gen_random_uuid(),
    date bigint NOT NULL,
    ownerEmail text NOT NULL,
    company text NOT NULL,
    title text NOT NULL,
    description text NOT NULL,
    externalUrl text NOT NULL,
    location text NOT NULL,
    remote boolean NOT NULL DEFAULT false,
    salaryLo integer,
    salaryHi integer,
    currency text,
    country text,
    tags text [],
    image text,
    seniority text,
    other text,
    active boolean NOT NULL DEFAULT false
);

ALTER TABLE
    jobs
ADD
    CONSTRAINT jobs_pk PRIMARY KEY (id);

CREATE TABLE users (
    email text NOT NULL,
    hashedPassword text NOT NULL,
    firstName text,
    lastName text,
    company text NOT NULL,
    role text NOT NULL
);

ALTER TABLE
    users
ADD
    CONSTRAINT pk_users PRIMARY KEY (email);