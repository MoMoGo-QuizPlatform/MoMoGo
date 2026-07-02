-- ====================================================
-- 1. 테이블 생성 단계 (기본 컬럼 및 데이터 타입 정의)
-- ====================================================

-- 공간 테이블
CREATE TABLE "TBL_SPACE" (
                             "id"               UUID          NOT NULL,
                             "name"             VARCHAR(255)  NOT NULL,
                             "description"      TEXT          NULL,
                             "space_image_url"  VARCHAR(500)  NULL,
                             "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL,
                             "updated_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL,
                             "space_password"   VARCHAR(255)  NULL
);

-- 사용자 테이블
CREATE TABLE "TBL_USER" (
                            "id"               UUID          NOT NULL,
                            "space_id"         UUID          NULL,
                            "password"         VARCHAR(255)  NOT NULL,
                            "name"             VARCHAR(100)  NOT NULL,
                            "email"            VARCHAR(255)  NOT NULL,
                            "profile_image_url" VARCHAR(500) NULL,
                            "role"             VARCHAR(50)   NOT NULL, -- SUPER_ADMIN, ADMIN, USER
                            "social"           VARCHAR(50)   DEFAULT 'NONE' NOT NULL, -- NONE, GOOGLE, KAKAO
                            "is_banned"        BOOLEAN       DEFAULT FALSE NULL,
                            "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL,
                            "updated_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL,
                            "deleted_at"       TIMESTAMPTZ   NULL, -- 논리 삭제용 유예 기간 필드
                            CONSTRAINT ck_user_role   CHECK ("role" IN ('USER', 'ADMIN', 'SUPER_ADMIN')),
                            CONSTRAINT ck_social_type CHECK ("social" IN ('NONE','KAKAO', 'GOOGLE'))
);

-- 문제 카테고리 테이블
CREATE TABLE "TBL_PROBLEM_CATEGORY" (
                                        "id"               UUID          NOT NULL,
                                        "space_id"         UUID          NOT NULL,
                                        "name"             VARCHAR(100)  NOT NULL,
                                        "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL
);

-- 문제 메타데이터 테이블 (싱글모드 문제은행)
CREATE TABLE "TBL_PROBLEM" (
                               "id"               UUID          NOT NULL,
                               "category_id"      UUID          NOT NULL,
                               "name"             VARCHAR(255)  NOT NULL,
                               "content"          TEXT          NOT NULL,
                               "explanation"      TEXT          NULL,
                               "correct_answer"   TEXT          NULL,     -- 정답 레이블 추가
                               "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL,
                               "updated_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL
);

-- 문제 통계 테이블 (1:1 매핑)
CREATE TABLE "TBL_PROBLEM_COUNTERS" (
                                        "problem_id"       UUID          NOT NULL,
                                        "solved_count"     INTEGER       DEFAULT 0 NULL,
                                        "try_count"        INTEGER       DEFAULT 0 NULL,
                                        "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL
);

-- 실시간 시험 방 테이블
CREATE TABLE "TBL_ROOM" (
                            "id"               UUID          NOT NULL,
                            "space_id"         UUID          NOT NULL,
                            "name"             VARCHAR(50)   NULL,
                            "description"      VARCHAR(255)  NULL,
                            "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL,
                            "updated_at"       TIMESTAMPTZ   NULL,
                            "is_ended"         BOOLEAN       DEFAULT FALSE NULL,
                            "test_start_at"    TIMESTAMPTZ   NULL,     -- 시험 시작 시간 제약
                            "test_end_at"      TIMESTAMPTZ   NULL      -- 시험 종료 시간 제약
);

-- 방 참여자 매핑 테이블 (초대 및 점수 기록)
CREATE TABLE "TBL_ROOM_USER" (
                                 "room_id"          UUID          NOT NULL,
                                 "user_id"          UUID          NOT NULL,
                                 "is_attended"      BOOLEAN       DEFAULT FALSE NULL,
                                 "score"            INTEGER       DEFAULT 0 NULL,
                                 "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL
);

-- 방-문제 중간 테이블 (독립 시험지 구조 - 역정규화 스펙)
CREATE TABLE "TBL_ROOM_PROBLEM" (
                                    "id"               UUID          NOT NULL,
                                    "room_id"          UUID          NOT NULL,
                                    "problem_order"    INTEGER       NOT NULL,
                                    "name"             VARCHAR(255)  NULL,
                                    "content"          TEXT          NULL,
                                    "explanation"      TEXT          NULL,
                                    "correct_answer"   TEXT          NULL,
                                    "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL,
                                    "updated_at"       TIMESTAMPTZ   NULL
);

-- 실시간 시험(방) 전용 제출 답안 테이블
CREATE TABLE "TBL_USER_ROOM_ANSWER" (
                                        "id"               UUID          NOT NULL,
                                        "user_id"          UUID          NOT NULL,
                                        "room_problem_id"  UUID          NOT NULL, -- TBL_ROOM_PROBLEM 참조
                                        "is_solved"        BOOLEAN       NOT NULL,
                                        "user_answer"      TEXT          NULL,
                                        "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL
);

-- 개인별 싱글모드 문제 풀이 이력 테이블
CREATE TABLE "TBL_USER_PROBLEM" (
                                    "user_id"          UUID          NOT NULL,
                                    "problem_id"       UUID          NOT NULL,
                                    "is_solved"        BOOLEAN       NOT NULL,
                                    "user_answer"      TEXT          NULL,
                                    "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL
);

-- 알림 테이블
CREATE TABLE "TBL_NOTIFICATION" (
                                    "id"               UUID          NOT NULL,
                                    "receiver_id"      UUID          NOT NULL,
                                    "title"            VARCHAR(255)  NOT NULL,
                                    "content"          TEXT          NOT NULL,
                                    "type"             VARCHAR(50)   NOT NULL,
                                    "is_confirmed"     BOOLEAN       DEFAULT FALSE NULL,
                                    "created_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL,
                                    "updated_at"       TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP NULL
);


-- ====================================================
-- 2. 기본키(PRIMARY KEY) 제약 조건 주입 단계
-- ====================================================

ALTER TABLE "TBL_SPACE" ADD CONSTRAINT "PK_TBL_SPACE" PRIMARY KEY ("id");
ALTER TABLE "TBL_USER" ADD CONSTRAINT "PK_TBL_USER" PRIMARY KEY ("id");
ALTER TABLE "TBL_PROBLEM_CATEGORY" ADD CONSTRAINT "PK_TBL_PROBLEM_CATEGORY" PRIMARY KEY ("id");
ALTER TABLE "TBL_PROBLEM" ADD CONSTRAINT "PK_TBL_PROBLEM" PRIMARY KEY ("id");
ALTER TABLE "TBL_PROBLEM_COUNTERS" ADD CONSTRAINT "PK_TBL_PROBLEM_COUNTERS" PRIMARY KEY ("problem_id");
ALTER TABLE "TBL_ROOM" ADD CONSTRAINT "PK_TBL_ROOM" PRIMARY KEY ("id");
ALTER TABLE "TBL_ROOM_USER" ADD CONSTRAINT "PK_TBL_ROOM_USER" PRIMARY KEY ("room_id", "user_id");
ALTER TABLE "TBL_ROOM_PROBLEM" ADD CONSTRAINT "PK_TBL_ROOM_PROBLEM" PRIMARY KEY ("id");
ALTER TABLE "TBL_USER_ROOM_ANSWER" ADD CONSTRAINT "PK_TBL_USER_ROOM_ANSWER" PRIMARY KEY ("id");
ALTER TABLE "TBL_USER_PROBLEM" ADD CONSTRAINT "PK_TBL_USER_PROBLEM" PRIMARY KEY ("user_id", "problem_id");
ALTER TABLE "TBL_NOTIFICATION" ADD CONSTRAINT "PK_TBL_NOTIFICATION" PRIMARY KEY ("id");


-- ====================================================
-- 3. 외래키(FOREIGN KEY) 제약 조건 및 연관관계선 주입 단계
-- ====================================================

-- 유저 및 공간 연관관계
ALTER TABLE "TBL_USER" ADD CONSTRAINT "FK_TBL_SPACE_TO_TBL_USER" FOREIGN KEY ("space_id") REFERENCES "TBL_SPACE" ("id");

-- 카테고리 및 문제 관계
ALTER TABLE "TBL_PROBLEM_CATEGORY" ADD CONSTRAINT "FK_TBL_SPACE_TO_TBL_PROBLEM_CATEGORY" FOREIGN KEY ("space_id") REFERENCES "TBL_SPACE" ("id");
ALTER TABLE "TBL_PROBLEM" ADD CONSTRAINT "FK_TBL_PROBLEM_CATEGORY_TO_TBL_PROBLEM" FOREIGN KEY ("category_id") REFERENCES "TBL_PROBLEM_CATEGORY" ("id");
ALTER TABLE "TBL_PROBLEM_COUNTERS" ADD CONSTRAINT "FK_TBL_PROBLEM_TO_TBL_PROBLEM_COUNTERS" FOREIGN KEY ("problem_id") REFERENCES "TBL_PROBLEM" ("id") ON DELETE CASCADE;

-- 방 및 독립 시험지 세팅
ALTER TABLE "TBL_ROOM" ADD CONSTRAINT "FK_TBL_SPACE_TO_TBL_ROOM" FOREIGN KEY ("space_id") REFERENCES "TBL_SPACE" ("id");
ALTER TABLE "TBL_ROOM_PROBLEM" ADD CONSTRAINT "FK_TBL_ROOM_TO_TBL_ROOM_PROBLEM" FOREIGN KEY ("room_id") REFERENCES "TBL_ROOM" ("id") ON DELETE CASCADE;

-- 방 참여 유저 맵
ALTER TABLE "TBL_ROOM_USER" ADD CONSTRAINT "FK_TBL_ROOM_TO_TBL_ROOM_USER" FOREIGN KEY ("room_id") REFERENCES "TBL_ROOM" ("id") ON DELETE CASCADE;
ALTER TABLE "TBL_ROOM_USER" ADD CONSTRAINT "FK_TBL_USER_TO_TBL_ROOM_USER" FOREIGN KEY ("user_id") REFERENCES "TBL_USER" ("id");

-- 실시간 시험방 제출 답안 링크
ALTER TABLE "TBL_USER_ROOM_ANSWER" ADD CONSTRAINT "FK_TBL_USER_TO_TBL_USER_ROOM_ANSWER" FOREIGN KEY ("user_id") REFERENCES "TBL_USER" ("id");
ALTER TABLE "TBL_USER_ROOM_ANSWER" ADD CONSTRAINT "FK_TBL_ROOM_PROBLEM_TO_TBL_USER_ROOM_ANSWER" FOREIGN KEY ("room_problem_id") REFERENCES "TBL_ROOM_PROBLEM" ("id") ON DELETE CASCADE;

-- 개인별 싱글모드 문제 이력
ALTER TABLE "TBL_USER_PROBLEM" ADD CONSTRAINT "FK_TBL_USER_TO_TBL_USER_PROBLEM" FOREIGN KEY ("user_id") REFERENCES "TBL_USER" ("id");
ALTER TABLE "TBL_USER_PROBLEM" ADD CONSTRAINT "FK_TBL_PROBLEM_TO_TBL_USER_PROBLEM" FOREIGN KEY ("problem_id") REFERENCES "TBL_PROBLEM" ("id");

-- 알림 시스템 수신자
ALTER TABLE "TBL_NOTIFICATION" ADD CONSTRAINT "FK_TBL_USER_TO_TBL_NOTIFICATION" FOREIGN KEY ("receiver_id") REFERENCES "TBL_USER" ("id");

-- ====================================================
-- 4. 인덱스(INDEX) 정의 단계
-- ====================================================

-- 이메일 중복 가입 방지 부분 유니크 인덱스 (탈퇴한 회원의 이메일은 UQ 제약에서 제외하여 재가입 허용)
CREATE UNIQUE INDEX "UQ_USER_EMAIL_ACTIVE" ON "TBL_USER" ("email") WHERE "deleted_at" IS NULL;

-- 알림 목록 조회 성능 최적화를 위한 복합 인덱스
CREATE INDEX "idx_notification_receiver_confirmed" ON "TBL_NOTIFICATION" ("receiver_id", "is_confirmed");

-- Super Admin 부분 유니크 인덱스
CREATE UNIQUE INDEX "UQ_USER_SUPER_ADMIN_ONLY" ON "TBL_USER" ("role") WHERE "role" = 'SUPER_ADMIN';