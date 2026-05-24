-- ============================================================================
-- seed-teste.sql — popula banco com 80 livros + 40 alunos pra testes em prod
-- ============================================================================
-- Use no Neon SQL Editor (https://console.neon.tech) OU via psql:
--   psql "postgresql://neondb_owner:senha@host.neon.tech/neondb?sslmode=require" -f seed-teste.sql
--
-- Pra LIMPAR tudo depois (manter admin), rode `seed-teste-cleanup.sql`.
--
-- Convencoes:
-- - Matriculas: 2026100 a 2026139 (40 alunos, a partir de 100 pra nao conflitar com testes manuais)
-- - ISBNs: 9788999000001 a 9788999000080 (prefixo 978-8999 improvavel de conflitar com cadastro real)
-- - Turmas: 6A, 7B, 8C, 9A — 10 alunos por turma
-- - Quantidades de exemplares variando 2-8 pra dashboard ficar visualmente interessante
-- ============================================================================

-- ============================================================================
-- 80 LIVROS — clássicos BR + internacional + infantil + didático + HQ
-- ============================================================================
INSERT INTO livros (titulo, autor, isbn, ano, quantidade_exemplares, quantidade_disponivel, version, created_at, updated_at) VALUES
  -- Classicos brasileiros (20)
  ('Dom Casmurro', 'Machado de Assis', '9788999000001', 1899, 6, 6, 0, NOW(), NOW()),
  ('Memorias Postumas de Bras Cubas', 'Machado de Assis', '9788999000002', 1881, 4, 4, 0, NOW(), NOW()),
  ('O Cortico', 'Aluisio Azevedo', '9788999000003', 1890, 5, 5, 0, NOW(), NOW()),
  ('Capitaes da Areia', 'Jorge Amado', '9788999000004', 1937, 6, 6, 0, NOW(), NOW()),
  ('Gabriela Cravo e Canela', 'Jorge Amado', '9788999000005', 1958, 3, 3, 0, NOW(), NOW()),
  ('Vidas Secas', 'Graciliano Ramos', '9788999000006', 1938, 5, 5, 0, NOW(), NOW()),
  ('Sao Bernardo', 'Graciliano Ramos', '9788999000007', 1934, 3, 3, 0, NOW(), NOW()),
  ('Iracema', 'Jose de Alencar', '9788999000008', 1865, 4, 4, 0, NOW(), NOW()),
  ('O Guarani', 'Jose de Alencar', '9788999000009', 1857, 3, 3, 0, NOW(), NOW()),
  ('Senhora', 'Jose de Alencar', '9788999000010', 1875, 3, 3, 0, NOW(), NOW()),
  ('Macunaima', 'Mario de Andrade', '9788999000011', 1928, 4, 4, 0, NOW(), NOW()),
  ('Triste Fim de Policarpo Quaresma', 'Lima Barreto', '9788999000012', 1915, 3, 3, 0, NOW(), NOW()),
  ('A Hora da Estrela', 'Clarice Lispector', '9788999000013', 1977, 5, 5, 0, NOW(), NOW()),
  ('A Paixao Segundo G.H.', 'Clarice Lispector', '9788999000014', 1964, 2, 2, 0, NOW(), NOW()),
  ('Grande Sertao Veredas', 'Joao Guimaraes Rosa', '9788999000015', 1956, 3, 3, 0, NOW(), NOW()),
  ('Sagarana', 'Joao Guimaraes Rosa', '9788999000016', 1946, 2, 2, 0, NOW(), NOW()),
  ('Quincas Borba', 'Machado de Assis', '9788999000017', 1891, 3, 3, 0, NOW(), NOW()),
  ('A Moreninha', 'Joaquim Manuel de Macedo', '9788999000018', 1844, 4, 4, 0, NOW(), NOW()),
  ('Helena', 'Machado de Assis', '9788999000019', 1876, 3, 3, 0, NOW(), NOW()),
  ('Os Sertoes', 'Euclides da Cunha', '9788999000020', 1902, 2, 2, 0, NOW(), NOW()),

  -- Literatura internacional (15)
  ('1984', 'George Orwell', '9788999000021', 1949, 6, 6, 0, NOW(), NOW()),
  ('A Revolucao dos Bichos', 'George Orwell', '9788999000022', 1945, 5, 5, 0, NOW(), NOW()),
  ('O Pequeno Principe', 'Antoine de Saint-Exupery', '9788999000023', 1943, 8, 8, 0, NOW(), NOW()),
  ('Cem Anos de Solidao', 'Gabriel Garcia Marquez', '9788999000024', 1967, 4, 4, 0, NOW(), NOW()),
  ('O Amor nos Tempos do Colera', 'Gabriel Garcia Marquez', '9788999000025', 1985, 3, 3, 0, NOW(), NOW()),
  ('Crime e Castigo', 'Fiodor Dostoievski', '9788999000026', 1866, 3, 3, 0, NOW(), NOW()),
  ('Os Irmaos Karamazov', 'Fiodor Dostoievski', '9788999000027', 1880, 2, 2, 0, NOW(), NOW()),
  ('Guerra e Paz', 'Liev Tolstoi', '9788999000028', 1869, 2, 2, 0, NOW(), NOW()),
  ('Anna Karenina', 'Liev Tolstoi', '9788999000029', 1877, 3, 3, 0, NOW(), NOW()),
  ('O Velho e o Mar', 'Ernest Hemingway', '9788999000030', 1952, 4, 4, 0, NOW(), NOW()),
  ('Orgulho e Preconceito', 'Jane Austen', '9788999000031', 1813, 5, 5, 0, NOW(), NOW()),
  ('Persuasao', 'Jane Austen', '9788999000032', 1817, 3, 3, 0, NOW(), NOW()),
  ('O Apanhador no Campo de Centeio', 'J.D. Salinger', '9788999000033', 1951, 4, 4, 0, NOW(), NOW()),
  ('O Grande Gatsby', 'F. Scott Fitzgerald', '9788999000034', 1925, 4, 4, 0, NOW(), NOW()),
  ('Fahrenheit 451', 'Ray Bradbury', '9788999000035', 1953, 3, 3, 0, NOW(), NOW()),

  -- Infantojuvenil e fantasia (15)
  ('Harry Potter e a Pedra Filosofal', 'J.K. Rowling', '9788999000036', 1997, 7, 7, 0, NOW(), NOW()),
  ('Harry Potter e a Camara Secreta', 'J.K. Rowling', '9788999000037', 1998, 6, 6, 0, NOW(), NOW()),
  ('Harry Potter e o Prisioneiro de Azkaban', 'J.K. Rowling', '9788999000038', 1999, 6, 6, 0, NOW(), NOW()),
  ('Harry Potter e o Calice de Fogo', 'J.K. Rowling', '9788999000039', 2000, 5, 5, 0, NOW(), NOW()),
  ('O Hobbit', 'J.R.R. Tolkien', '9788999000040', 1937, 5, 5, 0, NOW(), NOW()),
  ('O Senhor dos Aneis A Sociedade do Anel', 'J.R.R. Tolkien', '9788999000041', 1954, 4, 4, 0, NOW(), NOW()),
  ('As Cronicas de Narnia', 'C.S. Lewis', '9788999000042', 1950, 5, 5, 0, NOW(), NOW()),
  ('Percy Jackson e o Ladrao de Raios', 'Rick Riordan', '9788999000043', 2005, 6, 6, 0, NOW(), NOW()),
  ('Diario de um Banana', 'Jeff Kinney', '9788999000044', 2007, 8, 8, 0, NOW(), NOW()),
  ('A Menina que Roubava Livros', 'Markus Zusak', '9788999000045', 2005, 4, 4, 0, NOW(), NOW()),
  ('Extraordinario', 'R.J. Palacio', '9788999000046', 2012, 5, 5, 0, NOW(), NOW()),
  ('O Diario de Anne Frank', 'Anne Frank', '9788999000047', 1947, 4, 4, 0, NOW(), NOW()),
  ('Alice no Pais das Maravilhas', 'Lewis Carroll', '9788999000048', 1865, 5, 5, 0, NOW(), NOW()),
  ('Pinoquio', 'Carlo Collodi', '9788999000049', 1883, 3, 3, 0, NOW(), NOW()),
  ('O Magico de Oz', 'L. Frank Baum', '9788999000050', 1900, 3, 3, 0, NOW(), NOW()),

  -- Best-sellers contemporaneos (10)
  ('O Alquimista', 'Paulo Coelho', '9788999000051', 1988, 5, 5, 0, NOW(), NOW()),
  ('O Codigo Da Vinci', 'Dan Brown', '9788999000052', 2003, 3, 3, 0, NOW(), NOW()),
  ('Jogos Vorazes', 'Suzanne Collins', '9788999000053', 2008, 6, 6, 0, NOW(), NOW()),
  ('A Culpa e das Estrelas', 'John Green', '9788999000054', 2012, 5, 5, 0, NOW(), NOW()),
  ('A Cabana', 'William P. Young', '9788999000055', 2007, 3, 3, 0, NOW(), NOW()),
  ('Sapiens Uma Breve Historia da Humanidade', 'Yuval Noah Harari', '9788999000056', 2011, 3, 3, 0, NOW(), NOW()),
  ('O Poder do Habito', 'Charles Duhigg', '9788999000057', 2012, 3, 3, 0, NOW(), NOW()),
  ('Mindset A Nova Psicologia do Sucesso', 'Carol S. Dweck', '9788999000058', 2006, 2, 2, 0, NOW(), NOW()),
  ('Garota Exemplar', 'Gillian Flynn', '9788999000059', 2012, 3, 3, 0, NOW(), NOW()),
  ('Maravilhoso Mundo Novo', 'Aldous Huxley', '9788999000060', 1932, 3, 3, 0, NOW(), NOW()),

  -- Didaticos e referencia (10)
  ('Atlas Geografico Escolar', 'IBGE', '9788999000061', 2023, 5, 5, 0, NOW(), NOW()),
  ('Minidicionario Aurelio', 'Aurelio Buarque de Holanda', '9788999000062', 2024, 8, 8, 0, NOW(), NOW()),
  ('Gramatica Pratica da Lingua Portuguesa', 'Cegalla', '9788999000063', 2022, 6, 6, 0, NOW(), NOW()),
  ('Historia do Brasil', 'Boris Fausto', '9788999000064', 2019, 4, 4, 0, NOW(), NOW()),
  ('Pequeno Vocabulario da Lingua Portuguesa', 'Academia Brasileira de Letras', '9788999000065', 2021, 5, 5, 0, NOW(), NOW()),
  ('Fundamentos de Matematica Elementar', 'Gelson Iezzi', '9788999000066', 2020, 4, 4, 0, NOW(), NOW()),
  ('Biologia para o Ensino Medio', 'Cesar e Sezar', '9788999000067', 2022, 4, 4, 0, NOW(), NOW()),
  ('Fisica Conceitual', 'Paul G. Hewitt', '9788999000068', 2015, 3, 3, 0, NOW(), NOW()),
  ('Quimica para o Novo Ensino Medio', 'Usberco e Salvador', '9788999000069', 2021, 3, 3, 0, NOW(), NOW()),
  ('Filosofia Vida e Saber', 'Maria Lucia de Arruda Aranha', '9788999000070', 2020, 2, 2, 0, NOW(), NOW()),

  -- HQ e graphic novels (10)
  ('Turma da Monica em Quadrinhos', 'Mauricio de Sousa', '9788999000071', 2023, 6, 6, 0, NOW(), NOW()),
  ('Cebolinha Almanaque', 'Mauricio de Sousa', '9788999000072', 2022, 5, 5, 0, NOW(), NOW()),
  ('Asterix o Gaules', 'Rene Goscinny', '9788999000073', 1961, 3, 3, 0, NOW(), NOW()),
  ('Tintim no Tibete', 'Herge', '9788999000074', 1960, 3, 3, 0, NOW(), NOW()),
  ('Mafalda Toda', 'Quino', '9788999000075', 1973, 4, 4, 0, NOW(), NOW()),
  ('Persepolis', 'Marjane Satrapi', '9788999000076', 2000, 2, 2, 0, NOW(), NOW()),
  ('Maus a Historia de um Sobrevivente', 'Art Spiegelman', '9788999000077', 1986, 2, 2, 0, NOW(), NOW()),
  ('Watchmen', 'Alan Moore', '9788999000078', 1986, 2, 2, 0, NOW(), NOW()),
  ('V de Vinganca', 'Alan Moore', '9788999000079', 1988, 2, 2, 0, NOW(), NOW()),
  ('Sandman', 'Neil Gaiman', '9788999000080', 1989, 2, 2, 0, NOW(), NOW());

-- ============================================================================
-- 40 ALUNOS — matriculas 2026100 a 2026139, 4 turmas com 10 cada
-- ============================================================================
INSERT INTO alunos (matricula, nome, turma, created_at, updated_at) VALUES
  -- Turma 6A
  ('2026100', 'Ana Beatriz Silva Pereira', '6A', NOW(), NOW()),
  ('2026101', 'Bruno Henrique Souza Lima', '6A', NOW(), NOW()),
  ('2026102', 'Carolina Mendes Ribeiro', '6A', NOW(), NOW()),
  ('2026103', 'Diego Almeida Costa', '6A', NOW(), NOW()),
  ('2026104', 'Eduarda Fernandes Castro', '6A', NOW(), NOW()),
  ('2026105', 'Felipe Augusto Martins', '6A', NOW(), NOW()),
  ('2026106', 'Gabriela Cunha Barros', '6A', NOW(), NOW()),
  ('2026107', 'Henrique Oliveira Santos', '6A', NOW(), NOW()),
  ('2026108', 'Isabela Rocha Pinto', '6A', NOW(), NOW()),
  ('2026109', 'Joao Pedro Vieira Souza', '6A', NOW(), NOW()),

  -- Turma 7B
  ('2026110', 'Larissa Moraes Cardoso', '7B', NOW(), NOW()),
  ('2026111', 'Matheus Carvalho Nunes', '7B', NOW(), NOW()),
  ('2026112', 'Nathalia Pires Melo', '7B', NOW(), NOW()),
  ('2026113', 'Otavio Ferreira Lopes', '7B', NOW(), NOW()),
  ('2026114', 'Patricia Gomes Teixeira', '7B', NOW(), NOW()),
  ('2026115', 'Rafael Costa Andrade', '7B', NOW(), NOW()),
  ('2026116', 'Sofia Lima Cavalcante', '7B', NOW(), NOW()),
  ('2026117', 'Thiago Mendonca Ramos', '7B', NOW(), NOW()),
  ('2026118', 'Valentina Araujo Freitas', '7B', NOW(), NOW()),
  ('2026119', 'Vinicius Borges Tavares', '7B', NOW(), NOW()),

  -- Turma 8C
  ('2026120', 'Amanda Cristina Macedo', '8C', NOW(), NOW()),
  ('2026121', 'Bernardo Lacerda Antunes', '8C', NOW(), NOW()),
  ('2026122', 'Camila Duarte Rezende', '8C', NOW(), NOW()),
  ('2026123', 'Daniel Pacheco Bittencourt', '8C', NOW(), NOW()),
  ('2026124', 'Elisa Tavares Monteiro', '8C', NOW(), NOW()),
  ('2026125', 'Fernando Aguiar Mello', '8C', NOW(), NOW()),
  ('2026126', 'Giovanna Brito Sales', '8C', NOW(), NOW()),
  ('2026127', 'Hugo Pereira Vasconcelos', '8C', NOW(), NOW()),
  ('2026128', 'Iris Magalhaes Albuquerque', '8C', NOW(), NOW()),
  ('2026129', 'Julia Ramalho Sampaio', '8C', NOW(), NOW()),

  -- Turma 9A
  ('2026130', 'Kaua Dantas Figueiredo', '9A', NOW(), NOW()),
  ('2026131', 'Leticia Bezerra Quintanilha', '9A', NOW(), NOW()),
  ('2026132', 'Marcos Cordeiro Linhares', '9A', NOW(), NOW()),
  ('2026133', 'Natalia Siqueira Marinho', '9A', NOW(), NOW()),
  ('2026134', 'Otavio Quintela Lobato', '9A', NOW(), NOW()),
  ('2026135', 'Priscila Cardoso Esteves', '9A', NOW(), NOW()),
  ('2026136', 'Rogerio Faria Beltrao', '9A', NOW(), NOW()),
  ('2026137', 'Sabrina Lima Tavares', '9A', NOW(), NOW()),
  ('2026138', 'Tiago Severino Albuquerque', '9A', NOW(), NOW()),
  ('2026139', 'Ursula Caetano Rocha', '9A', NOW(), NOW());

-- ============================================================================
-- Verificacao final
-- ============================================================================
SELECT 'livros inseridos' AS info, COUNT(*) AS qtd FROM livros WHERE isbn LIKE '9788999%'
UNION ALL
SELECT 'alunos inseridos', COUNT(*) FROM alunos WHERE matricula BETWEEN '2026100' AND '2026139';
