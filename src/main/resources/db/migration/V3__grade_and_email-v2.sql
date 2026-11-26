-- Change request v2: grade eligibility and mock email.
-- Additive only: existing baseline tables keep their structure.

CREATE TABLE student_grade_v2 (
    student_id UUID PRIMARY KEY REFERENCES student (student_id),
    grade      SMALLINT NOT NULL CHECK (grade BETWEEN 1 AND 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Default existing students to grade 1; course demo data adjusts selected rows later.
INSERT INTO student_grade_v2 (student_id, grade)
SELECT student_id, 1 FROM student
ON CONFLICT (student_id) DO NOTHING;

CREATE TABLE activity_grade_rule_v2 (
    activity_id UUID PRIMARY KEY REFERENCES activity (activity_id),
    min_grade   SMALLINT NOT NULL CHECK (min_grade BETWEEN 1 AND 4),
    max_grade   SMALLINT NOT NULL CHECK (max_grade BETWEEN 1 AND 4),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_activity_grade_range CHECK (min_grade <= max_grade)
);

CREATE TABLE email_notification_v2 (
    email_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_event_id UUID NOT NULL,
    student_id      UUID NOT NULL REFERENCES student (student_id),
    recipient       VARCHAR(320) NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('SENT', 'FAILED')),
    error           TEXT,
    sent_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_email_source_student UNIQUE (source_event_id, student_id)
);

CREATE INDEX ix_email_created_at_v2 ON email_notification_v2 (created_at DESC);
