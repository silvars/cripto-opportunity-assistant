-- Fase 6 — Decision Memory (SDD §54). Screenshot fica fora do Git
-- (data/screenshots/, ver .gitignore) — aqui só o caminho é persistido.
CREATE TABLE operation_screenshots (
    screenshot_id UUID PRIMARY KEY,
    operation_id UUID NOT NULL REFERENCES operations(operation_id),
    file_path VARCHAR(500) NOT NULL,
    confidence VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_operation_screenshots_operation_id ON operation_screenshots (operation_id);
