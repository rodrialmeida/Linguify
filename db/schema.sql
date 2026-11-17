-- Linguify - estrutura do banco de dados (MySQL 8).
-- Somente a estrutura: nenhum dado e nenhuma senha.
-- Os dados de demonstracao sao criados por: util.CriarDadosDemo (veja o README).

CREATE DATABASE IF NOT EXISTS `login`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `login`;

-- Usuarios. A coluna `senha` guarda SOMENTE o hash BCrypt (60 caracteres).
CREATE TABLE IF NOT EXISTS `usuario` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `username`   varchar(50) DEFAULT NULL,
  `senha`      varchar(60) DEFAULT NULL,
  `perfil`     varchar(20) DEFAULT 'ESTUDANTE',
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uk_usuario_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Aulas e materias de idiomas: `materia` = idioma, `tema` = nome da aula.
CREATE TABLE IF NOT EXISTS `materias` (
  `id_materias` int NOT NULL AUTO_INCREMENT,
  `professor`   varchar(50)  DEFAULT NULL,
  `materia`     varchar(100) DEFAULT NULL,
  `data`        varchar(30)  DEFAULT NULL,
  `conteudo`    varchar(255) DEFAULT NULL,
  `tema`        varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_materias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
