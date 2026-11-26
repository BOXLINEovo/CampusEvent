-- Dev-only demo data for change request v2. Loaded with the "dev" profile.

INSERT INTO student (student_id, name) VALUES
    ('33333333-3333-3333-3333-333333333333', '演示学生C'),
    ('44444444-4444-4444-4444-444444444444', '演示学生D')
ON CONFLICT (student_id) DO NOTHING;

UPDATE student_grade_v2 SET grade = 2, updated_at = now()
WHERE student_id = '11111111-1111-1111-1111-111111111111';

UPDATE student_grade_v2 SET grade = 3, updated_at = now()
WHERE student_id = '22222222-2222-2222-2222-222222222222';

INSERT INTO student_grade_v2 (student_id, grade) VALUES
    ('33333333-3333-3333-3333-333333333333', 1),
    ('44444444-4444-4444-4444-444444444444', 4)
ON CONFLICT (student_id) DO NOTHING;

INSERT INTO activity (
    activity_id, title, category, start_at, location, capacity, policy_type
) VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    '年级限定活动：大二大三专场',
    '讲座',
    now() + interval '14 days',
    '学生活动中心 301',
    2,
    'DIRECT'
)
ON CONFLICT (activity_id) DO NOTHING;

INSERT INTO activity_grade_rule_v2 (activity_id, min_grade, max_grade)
VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 2, 3)
ON CONFLICT (activity_id) DO NOTHING;
