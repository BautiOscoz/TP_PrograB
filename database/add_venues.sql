-- Additional tournament venues. Safe to run again: existing names are preserved.
BEGIN;
LOCK TABLE pais, ciudad, estadio IN SHARE ROW EXCLUSIVE MODE;
CREATE TEMP TABLE venue_seed (country text, city text, stadium text) ON COMMIT DROP;
INSERT INTO venue_seed VALUES
('Alemania', 'Berlín', 'Estadio Olímpico de Berlín'),
('Alemania', 'Dortmund', 'Westfalenstadion'),
('Alemania', 'Múnich', 'Allianz Arena'),
('Argentina', 'La Plata', 'Estadio Ciudad de La Plata'),
('Argentina', 'Rosario', 'Gigante de Arroyito'),
('Argentina', 'Córdoba', 'Mario Alberto Kempes'),
('Brasil', 'São Paulo', 'Morumbi'),
('Brasil', 'Brasilia', 'Mané Garrincha'),
('España', 'Sevilla', 'Ramón Sánchez-Pizjuán'),
('España', 'Valencia', 'Mestalla'),
('Francia', 'Marsella', 'Stade Vélodrome'),
('Francia', 'Saint-Denis', 'Stade de France'),
('Indonesia', 'Surabaya', 'Gelora Bung Tomo'),
('Indonesia', 'Yakarta', 'Gelora Bung Karno'),
('Italia', 'Turín', 'Juventus Stadium'),
('Italia', 'Nápoles', 'Diego Armando Maradona'),
('Japón', 'Yokohama', 'Estadio Internacional de Yokohama'),
('Japón', 'Saitama', 'Saitama Stadium 2002'),
('Nigeria', 'Lagos', 'Teslim Balogun'),
('Nigeria', 'Abuya', 'Estadio Nacional de Abuya'),
('Reino Unido', 'Liverpool', 'Anfield'),
('Reino Unido', 'Glasgow', 'Hampden Park'),
('Rusia', 'San Petersburgo', 'Estadio de San Petersburgo'),
('Rusia', 'Moscú', 'Luzhniki'),
('Turquía', 'Ankara', 'Eryaman'),
('Turquía', 'Estambul', 'Estadio Olímpico Atatürk');

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM venue_seed v WHERE NOT EXISTS
        (SELECT 1 FROM pais p WHERE lower(p.nombre) = lower(v.country))) THEN
        RAISE EXCEPTION 'A seed country is missing; no venues were added.';
    END IF;
END $$;

INSERT INTO ciudad (nombre, id_pais)
SELECT DISTINCT v.city, p.id FROM venue_seed v
JOIN pais p ON lower(p.nombre) = lower(v.country)
WHERE NOT EXISTS (SELECT 1 FROM ciudad c
    WHERE c.id_pais = p.id AND lower(c.nombre) = lower(v.city));

INSERT INTO estadio (nombre, ciudad)
SELECT v.stadium, c.id FROM venue_seed v
JOIN pais p ON lower(p.nombre) = lower(v.country)
JOIN ciudad c ON c.id_pais = p.id AND lower(c.nombre) = lower(v.city)
WHERE NOT EXISTS (SELECT 1 FROM estadio e
    WHERE e.ciudad = c.id AND lower(e.nombre) = lower(v.stadium));
COMMIT;
