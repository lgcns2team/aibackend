-- =========================
-- CLASS CODE 정의
-- =========================
-- Teacher Kim  : 1학년 1~5반 담당
-- Teacher Choi : 2학년 6~10반 담당

-- =========================
-- Teacher Kim (Grade 1)
-- =========================
INSERT INTO users (
    user_id,
    grade,
    name,
    role,
    nickname,
    password,
    classroom,
    class_code
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    1,
    'Teacher Kim',
    'TEACHER',
    'T_Kim',
    'password',
    1,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
);

-- =========================
-- Students (Grade 1, Class 1~5)
-- =========================
INSERT INTO users (
    user_id,
    grade,
    name,
    role,
    nickname,
    password,
    classroom,
    class_code
) VALUES
(
    '22222222-2222-2222-2222-222222222222',
    1,
    'Student Lee',
    'STUDENT',
    'S_Lee',
    'password',
    1,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
),
(
    '33333333-3333-3333-3333-333333333333',
    1,
    'Student Park',
    'STUDENT',
    'S_Park',
    'password',
    2,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
),
(
    '66666666-6666-6666-6666-666666666666',
    1,
    'Student Choi',
    'STUDENT',
    'S_Choi',
    'password',
    3,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
),
(
    '77777777-7777-7777-7777-777777777777',
    1,
    'Student Han',
    'STUDENT',
    'S_Han',
    'password',
    4,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
),
(
    '88888888-8888-8888-8888-888888888888',
    1,
    'Student Lim',
    'STUDENT',
    'S_Lim',
    'password',
    5,
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
);

-- =========================
-- Teacher Choi (Grade 2)
-- =========================
INSERT INTO users (
    user_id,
    grade,
    name,
    role,
    nickname,
    password,
    classroom,
    class_code
) VALUES (
    '44444444-4444-4444-4444-444444444444',
    2,
    'Teacher Choi',
    'TEACHER',
    'T_Choi',
    'password',
    6,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
);

-- =========================
-- Students (Grade 2, Class 6~10)
-- =========================
INSERT INTO users (
    user_id,
    grade,
    name,
    role,
    nickname,
    password,
    classroom,
    class_code
) VALUES
(
    '55555555-5555-5555-5555-555555555555',
    2,
    'Student Jung',
    'STUDENT',
    'S_Jung',
    'password',
    6,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
),
(
    '99999999-9999-9999-9999-999999999999',
    2,
    'Student Moon',
    'STUDENT',
    'S_Moon',
    'password',
    7,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
),
(
    'aaaaaaaa-1111-1111-1111-111111111111',
    2,
    'Student Shin',
    'STUDENT',
    'S_Shin',
    'password',
    8,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
),
(
    'bbbbbbbb-1111-1111-1111-111111111111',
    2,
    'Student Yoon',
    'STUDENT',
    'S_Yoon',
    'password',
    9,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
),
(
    'cccccccc-1111-1111-1111-111111111111',
    2,
    'Student Kang',
    'STUDENT',
    'S_Kang',
    'password',
    10,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
);
