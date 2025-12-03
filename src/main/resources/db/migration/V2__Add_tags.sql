CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE INDEX idx_tags_name ON tags(name);

CREATE TABLE decision_tags (
    decision_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (decision_id, tag_id),
    FOREIGN KEY (decision_id) REFERENCES decisions(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_decision_tags_decision_id ON decision_tags(decision_id);
CREATE INDEX idx_decision_tags_tag_id ON decision_tags(tag_id);
