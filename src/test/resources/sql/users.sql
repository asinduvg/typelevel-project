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

INSERT INTO
    users (
        email,
        hashedPassword,
        firstName,
        lastName,
        company,
        role
    )
VALUES
    (
        'daniel@rockthejvm.com',
        '$2a$10$nVEJ3pJkjN1K6esp6aS6s.0mp2gGMep1x7Akaz3UgzTCrxGnAwC0a',
        'Daniel',
        'Ciocirlan',
        'Rock the JVM',
        'ADMIN'
    );

INSERT INTO
    users (
        email,
        hashedPassword,
        firstName,
        lastName,
        company,
        role
    )
VALUES
    (
        'riccardo@rockthejvm.com',
        '$2a$10$VgVDCMWMsS2F1FXn/dfZ.uBgRyVOao833QyQXy0Sn8/T9NqnRC7Mu',
        'Riccardo',
        'Cardin',
        'Rock the JVM',
        'RECRUITER'
    );