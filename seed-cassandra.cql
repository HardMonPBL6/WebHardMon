-- Datos de prueba minimos
-- La empresa con id=1 debe existir ya en MySQL (creada por DatabaseSeeder)

INSERT INTO webhardmon.empresas (empresa_id, nombre)
VALUES (1, 'Acme Corp');

INSERT INTO webhardmon.ordenadores (empresa_id, nombre, procesador, ram, almacenamiento, ultima_vez)
VALUES (1, 'PC-1-1', 'Intel i5-1135G7', '16GB', '512GB SSD', toTimestamp(now()));

INSERT INTO webhardmon.mediciones (empresa_id, nombre, ts, cpu_percent, ram_percent, disco_percent, temperatura, bateria_percent, stress_score, ram, almacenamiento)
VALUES (1, 'PC-1-1', toTimestamp(now()), 35.2, 62.4, 71.0, 48.5, 89.0, 22.0, '16GB', '512GB SSD');
