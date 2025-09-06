CREATE TABLE decisions (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    min_score INTEGER NOT NULL,
    max_score INTEGER NOT NULL,
    created_by TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE criteria (
    id SERIAL PRIMARY KEY,
    decision_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    weight INTEGER NOT NULL,
    FOREIGN KEY(decision_id) REFERENCES decisions(id)
);

CREATE TABLE options (
    id SERIAL PRIMARY KEY,
    decision_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    FOREIGN KEY(decision_id) REFERENCES decisions(id)
);

CREATE TABLE user_scores (
    id SERIAL PRIMARY KEY,
    decision_id INTEGER NOT NULL,
    option_id INTEGER NOT NULL,
    criteria_id INTEGER NOT NULL,
    score INTEGER NOT NULL,
    scored_by TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(decision_id) REFERENCES decisions(id),
    FOREIGN KEY(option_id) REFERENCES options(id),
    FOREIGN KEY(criteria_id) REFERENCES criteria(id)
);