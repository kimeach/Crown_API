CREATE DATABASE IF NOT EXISTS crown_db DEFAULT CHARACTER SET utf8mb4;
USE crown_db;

-- 회원
CREATE TABLE IF NOT EXISTS member (
    member_id   BIGINT          AUTO_INCREMENT PRIMARY KEY,
    google_id   VARCHAR(100)    NOT NULL UNIQUE,
    nickname    VARCHAR(50)     NOT NULL,
    email       VARCHAR(100)    NOT NULL,
    profile_img VARCHAR(500),
    score       INT             NOT NULL DEFAULT 1000,
    win_count   INT             NOT NULL DEFAULT 0,
    lose_count  INT             NOT NULL DEFAULT 0,
    draw_count  INT             NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT NOW(),
    updated_at  DATETIME        NOT NULL DEFAULT NOW() ON UPDATE NOW()
);

-- 게임방
CREATE TABLE IF NOT EXISTS game_room (
    room_id             BIGINT      AUTO_INCREMENT PRIMARY KEY,
    black_member_id     BIGINT      NOT NULL,
    white_member_id     BIGINT      NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS, FINISHED, ABANDONED
    winner_id           BIGINT,
    black_score_change  INT,
    white_score_change  INT,
    created_at          DATETIME    NOT NULL DEFAULT NOW(),
    finished_at         DATETIME,
    FOREIGN KEY (black_member_id)   REFERENCES member(member_id),
    FOREIGN KEY (white_member_id)   REFERENCES member(member_id),
    FOREIGN KEY (winner_id)         REFERENCES member(member_id)
);

-- 착수 기록 (리플레이용)
CREATE TABLE IF NOT EXISTS game_move (
    move_id     BIGINT      AUTO_INCREMENT PRIMARY KEY,
    room_id     BIGINT      NOT NULL,
    member_id   BIGINT      NOT NULL,
    move_x      TINYINT     NOT NULL,
    move_y      TINYINT     NOT NULL,
    move_order  INT         NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT NOW(),
    FOREIGN KEY (room_id)   REFERENCES game_room(room_id),
    FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- ── ShortsMaker ────────────────────────────────────────────────────────────────

-- 쇼츠 프로젝트 (영상 1개 = 프로젝트 1개)
CREATE TABLE IF NOT EXISTS sm_project (
    project_id  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT          NOT NULL,
    category    VARCHAR(20)     NOT NULL DEFAULT 'stock',   -- stock | beauty | tech
    title       VARCHAR(200),
    script      JSON,                                       -- {slide_1: "...", slide_2: "..."}
    html_url    VARCHAR(500),                               -- Firebase Storage HTML URL
    status      VARCHAR(20)     NOT NULL DEFAULT 'draft',   -- draft | generating | done | error
    video_url   VARCHAR(500),                               -- Firebase Storage MP4 URL
    created_at  DATETIME        NOT NULL DEFAULT NOW(),
    updated_at  DATETIME        NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    FOREIGN KEY (member_id) REFERENCES member(member_id)
);

-- 쇼츠 영상 생성 잡
CREATE TABLE IF NOT EXISTS sm_job (
    job_id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT      NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending | running | done | error
    error_message   TEXT,
    started_at      DATETIME,
    finished_at     DATETIME,
    FOREIGN KEY (project_id) REFERENCES sm_project(project_id)
);
