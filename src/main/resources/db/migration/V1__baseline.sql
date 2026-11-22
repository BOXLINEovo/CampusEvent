-- CampusEvent baseline schema (skeleton)
-- PostgreSQL 16

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE student (
    student_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE activity (
    activity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    category    VARCHAR(60)  NOT NULL,
    start_at    TIMESTAMPTZ  NOT NULL,
    location    VARCHAR(200) NOT NULL,
    capacity    INTEGER      NOT NULL CHECK (capacity > 0),
    policy_type VARCHAR(20)  NOT NULL CHECK (policy_type IN ('DIRECT', 'REVIEW')),
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_activity_visible ON activity (deleted_at, start_at);

CREATE TABLE registration (
    registration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id     UUID NOT NULL REFERENCES activity (activity_id),
    student_id      UUID NOT NULL REFERENCES student (student_id),
    status          VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 同一学生同一活动最多一条“待审核/已通过”有效报名；已拒绝/已取消保留历史。
CREATE UNIQUE INDEX ux_registration_active
    ON registration (activity_id, student_id)
    WHERE status IN ('PENDING', 'APPROVED');

CREATE INDEX ix_registration_activity_status ON registration (activity_id, status);
CREATE INDEX ix_registration_student_created ON registration (student_id, created_at DESC);

CREATE TABLE message (
    message_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID NOT NULL REFERENCES student (student_id),
    activity_id     UUID NOT NULL REFERENCES activity (activity_id),
    source_event_id UUID NOT NULL,
    type            VARCHAR(30) NOT NULL CHECK (type IN ('REGISTRATION_SUCCESS', 'REGISTRATION_APPROVED', 'REGISTRATION_REJECTED')),
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at         TIMESTAMPTZ,
    CONSTRAINT ux_message_source_student UNIQUE (source_event_id, student_id)
);

CREATE INDEX ix_message_student_created ON message (student_id, created_at DESC);
CREATE INDEX ix_message_student_unread ON message (student_id, is_read);

CREATE TABLE outbox_event (
    event_id        UUID PRIMARY KEY,
    event_type      VARCHAR(30) NOT NULL CHECK (event_type IN ('REGISTERED', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    retry_count     INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_outbox_status_next_attempt ON outbox_event (status, next_attempt_at);
