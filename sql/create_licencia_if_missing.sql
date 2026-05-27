-- Solo para bases de datos que todavía no tengan la tabla licencia.
CREATE TABLE IF NOT EXISTS public.licencia (
    id BIGSERIAL PRIMARY KEY,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    codigo VARCHAR(255) NOT NULL,
    empresa_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    portatil VARCHAR(80) NOT NULL,
    CONSTRAINT uk_licencia_codigo UNIQUE (codigo),
    CONSTRAINT uk_licencia_empresa_portatil UNIQUE (empresa_id, portatil),
    CONSTRAINT fk_licencia_empresa FOREIGN KEY (empresa_id) REFERENCES public.empresa(id)
);
