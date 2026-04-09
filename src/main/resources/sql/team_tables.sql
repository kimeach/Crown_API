-- 팀 협업 테이블 (2026-04-09)

CREATE TABLE IF NOT EXISTS sm_team (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    owner_id   INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW()
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sm_team_member (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    team_id    INT NOT NULL,
    member_id  INT NOT NULL,
    role       ENUM('owner','editor','viewer') NOT NULL DEFAULT 'viewer',
    status     ENUM('pending','active') NOT NULL DEFAULT 'pending',
    invited_at DATETIME NOT NULL DEFAULT NOW(),
    joined_at  DATETIME DEFAULT NULL,
    UNIQUE KEY uk_team_member (team_id, member_id),
    INDEX idx_member (member_id)
) DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sm_team_project (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    team_id    INT NOT NULL,
    project_id INT NOT NULL,
    shared_by  INT NOT NULL,
    shared_at  DATETIME NOT NULL DEFAULT NOW(),
    UNIQUE KEY uk_team_project (team_id, project_id),
    INDEX idx_team (team_id)
) DEFAULT CHARSET=utf8mb4;
