-- Dev-only demo data. Loaded only with the "dev" profile.
-- Do not use in production.

INSERT INTO student (student_id, name) VALUES
    ('11111111-1111-1111-1111-111111111111', '演示学生A'),
    ('22222222-2222-2222-2222-222222222222', '演示学生B')
ON CONFLICT (student_id) DO NOTHING;

INSERT INTO activity (activity_id, title, category, start_at, location, capacity, policy_type) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '迎新讲座（直接报名）', '讲座', now() + interval '7 days', '学术报告厅', 2, 'DIRECT'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '编程工作坊（审核报名）', '工作坊', now() + interval '10 days', '实验楼 201', 2, 'REVIEW')
ON CONFLICT (activity_id) DO NOTHING;
