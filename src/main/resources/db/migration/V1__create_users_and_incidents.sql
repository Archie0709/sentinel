/*
 LOOKUP TABLES
*/

-- Roles
CREATE TABLE roles (
    id  SMALLINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- Severities
CREATE TABLE severities (
    id  SMALLINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    rank_order SMALLINT NOT NULL UNIQUE
);

-- Incident Statuses
CREATE TABLE incident_statuses (
    id  SMALLINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    sort_order SMALLINT NOT NULL UNIQUE
);


/*
 CHANGE VALIDATION
*/

-- Permitted Status Transitions
CREATE TABLE status_transitions (
    from_status_id  SMALLINT    NOT NULL,
    to_status_id    SMALLINT    NOT NULL,

    PRIMARY KEY (from_status_id, to_status_id),

    CONSTRAINT fk_transition_from   FOREIGN KEY (from_status_id)    REFERENCES incident_statuses(id),
    CONSTRAINT fk_transition_to     FOREIGN KEY (to_status_id)      REFERENCES incident_statuses(id)
);


/*
 MAIN TABLES
*/

-- Users
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL    UNIQUE,
    password_hash   VARCHAR(100)    NOT NULL,
    role_id         SMALLINT        NOT NULL,
    created_at      TIMESTAMP       NOT NULL    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Incidents
CREATE TABLE incidents (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL,
    description     TEXT            NOT NULL,
    severity_id     SMALLINT        NOT NULL,
    status_id       SMALLINT        NOT NULL,
    assigned_to     BIGINT          NULL,
    created_by      BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL    DEFAULT CURRENT_TIMESTAMP   ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_incident_severity FOREIGN KEY (severity_id)   REFERENCES severities(id),
    CONSTRAINT fk_incident_status   FOREIGN KEY (status_id)     REFERENCES incident_statuses(id),
    CONSTRAINT fk_incident_assigned FOREIGN KEY (assigned_to)   REFERENCES users(id),
    CONSTRAINT fk_incident_creator  FOREIGN KEY (created_by)    REFERENCES users(id)
);

-- Audit Logs
CREATE TABLE audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id     BIGINT          NOT NULL,
    changed_by      BIGINT          NOT NULL,
    field_name      VARCHAR(50)     NOT NULL,
    old_value       VARCHAR(500)    NULL,
    new_value       VARCHAR(500)    NULL,
    changed_at      TIMESTAMP       NOT NULL    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_incident    FOREIGN KEY (incident_id)   REFERENCES incidents(id),
    CONSTRAINT fk_audit_user        FOREIGN KEY (changed_by)    REFERENCES users(id)
);


/*
 INDEXES
*/

CREATE INDEX idx_incidents_staus    ON  incidents(status_id);
CREATE INDEX idx_incidents_severity ON  incidents(severity_id);
CREATE INDEX idx_incidents_assigned ON  incidents(assigned_to);

CREATE INDEX idx_audit_incident ON  audit_log(incident_id);


/*
 SEED DATA
*/

INSERT INTO roles (name) VALUES ('ANALYST'), ('ADMIN');

INSERT INTO severities (name, rank_order) VALUES
    ('LOW', 1), ('MEDIUM', 2), ('HIGH', 3), ('SEVERE', 4), ('CRITICAL', 5);

INSERT INTO incident_statuses (name, sort_order) VALUES
    ('PENDING', 1), ('INVESTIGATING', 2), ('CONTAINED', 3), ('RESOLVED', 4);

INSERT INTO status_transitions (from_status_id, to_status_id)
SELECT f.id, t.id FROM incident_statuses f, incident_statuses t
WHERE (f.name, t.name) IN (
    ('PENDING', 'INVESTIGATING'),
    ('INVESTIGATING', 'CONTAINED'),
    ('CONTAINED', 'RESOLVED'),
    ('CONTAINED', 'INVESTIGATING'), -- allows re-opening
    ('RESOLVED', 'INVESTIGATING') -- ^
);