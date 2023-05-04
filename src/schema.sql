CREATE TABLE user
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    uuid        BIGINT       NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100)          DEFAULT NULL,
    phone       VARCHAR(20)           DEFAULT NULL,
    password    VARCHAR(128) NOT NULL,
    salt        VARCHAR(32)  NOT NULL,
    nickname    VARCHAR(50)           DEFAULT NULL,
    avatar      VARCHAR(200)          DEFAULT NULL,
    gender      TINYINT               DEFAULT NULL,
    birthday    DATE                  DEFAULT NULL,
    country     VARCHAR(50)           DEFAULT NULL,
    province    VARCHAR(50)           DEFAULT NULL,
    city        VARCHAR(50)           DEFAULT NULL,
    address     VARCHAR(200)          DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_uuid (uuid),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE friend
(
    id              BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    friend_id       BIGINT NOT NULL,
    relation_type   INT    NOT NULL DEFAULT 0,
    relation_status INT    NOT NULL DEFAULT 0,
    remark          VARCHAR(255),
    permission      INT    NOT NULL DEFAULT 1,
    group_id        INT,
    latest_dynamic  VARCHAR(255),
    is_recommend    BOOLEAN         DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_friend (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES user (id),
    FOREIGN KEY (friend_id) REFERENCES user (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;