package util;

import DAO.ConexaoDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Carga inicial de demonstracao para uma instalacao nova: cria as contas
 * {@code admin} (ADMIN), {@code aluno} e {@code rodrigo} (ESTUDANTE) e 8 aulas de
 * idiomas (Ingles, Espanhol, Frances e Alemao).
 *
 * <p>As senhas NAO ficam no codigo: sao informadas na linha de comando e gravadas
 * somente como hash BCrypt. A carga e segura para repetir: nunca altera nem apaga
 * nada; contas que ja existem sao mantidas e as aulas so sao criadas se a tabela
 * {@code materias} estiver vazia.</p>
 *
 * <p>Uso: {@code mvnw exec:java -Dexec.mainClass=util.CriarDadosDemo
 * -Dexec.args="SENHA_ADMIN SENHA_ALUNO SENHA_RODRIGO"}</p>
 */
public final class CriarDadosDemo {

    /** Resumo do que a carga fez. */
    public record Resultado(int usuariosCriados, int usuariosJaExistiam, int aulasCriadas) {
    }

    private record Aula(String idioma, String tema, String data, String conteudo) {
    }

    private static final String PROFESSOR = "Linguify Demo";

    private static final List<Aula> AULAS = List.of(
            new Aula("Inglês", "Greetings", "09/11/2026",
                    "Aprenda a cumprimentar e se apresentar em inglês: Hello, Good morning, Nice to meet you. "
                    + "Pratique frases como \"My name is ... and I am from ...\"."),
            new Aula("Inglês", "Present Simple", "10/11/2026",
                    "Use o presente simples para rotinas e fatos: I work, she works. "
                    + "Em perguntas e negativas use do/does: \"Do you speak English?\" e \"She doesn't work here\"."),
            new Aula("Espanhol", "Saludos", "11/11/2026",
                    "Aprenda saudações e apresentações em espanhol: Hola, Buenos días, Mucho gusto. "
                    + "Pratique \"Me llamo ... y soy de ...\"."),
            new Aula("Espanhol", "Verbos básicos", "12/11/2026",
                    "Conheça os verbos ser e estar: yo soy, tú eres, él es; yo estoy, tú estás. "
                    + "Ser indica identidade; estar indica estado ou lugar: \"Estoy en casa\"."),
            new Aula("Francês", "Salutations", "13/11/2026",
                    "Aprenda a cumprimentar e se apresentar em francês: Bonjour, Bonsoir, Enchanté. "
                    + "Pratique \"Je m'appelle ... et je viens de ...\"."),
            new Aula("Francês", "Verbes essentiels", "16/11/2026",
                    "Estude os verbos être e avoir no presente: je suis, tu es, il est; j'ai, tu as, il a. "
                    + "Exemplo: \"J'ai vingt ans\" e \"Elle est brésilienne\"."),
            new Aula("Alemão", "Begrüßungen", "17/11/2026",
                    "Aprenda saudações e apresentações em alemão: Hallo, Guten Morgen, Freut mich. "
                    + "Pratique \"Ich heiße ... und komme aus ...\"."),
            new Aula("Alemão", "Grundverben", "18/11/2026",
                    "Conheça os verbos sein e haben no presente: ich bin, du bist, er ist; ich habe, du hast, er hat. "
                    + "Exemplo: \"Ich habe einen Bruder\"."));

    private CriarDadosDemo() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Uso: CriarDadosDemo <senha-admin> <senha-aluno> <senha-rodrigo>");
            System.exit(2);
        }
        try {
            Resultado r = executar(() -> new ConexaoDAO().conectaBD(), args[0], args[1], args[2]);
            System.out.println("Usuários criados: " + r.usuariosCriados()
                    + " (já existiam: " + r.usuariosJaExistiam() + ")");
            System.out.println("Aulas criadas: " + r.aulasCriadas());
        } catch (Exception e) {
            System.err.println("Não foi possível concluir a carga: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Executa a carga numa conexao fornecida (separado do {@code main} para poder ser testado). */
    public static Resultado executar(Supplier<Connection> origem, String senhaAdmin, String senhaAluno,
            String senhaRodrigo) throws SQLException {
        if (vazia(senhaAdmin) || vazia(senhaAluno) || vazia(senhaRodrigo)) {
            throw new IllegalArgumentException("As três senhas são obrigatórias.");
        }
        try (Connection c = origem.get()) {
            if (c == null) {
                throw new SQLException("Não foi possível conectar ao banco de dados.");
            }
            int criados = 0;
            int existentes = 0;
            String[][] contas = {
                {"admin", senhaAdmin, "ADMIN"},
                {"aluno", senhaAluno, "ESTUDANTE"},
                {"rodrigo", senhaRodrigo, "ESTUDANTE"}};
            for (String[] conta : contas) {
                if (usuarioExiste(c, conta[0])) {
                    existentes++;
                } else {
                    inserirUsuario(c, conta[0], PasswordHasher.hash(conta[1]), conta[2]);
                    criados++;
                }
            }

            int aulas = 0;
            if (contarAulas(c) == 0) {
                for (Aula a : AULAS) {
                    inserirAula(c, a);
                    aulas++;
                }
            }
            return new Resultado(criados, existentes, aulas);
        }
    }

    private static boolean vazia(String s) {
        return s == null || s.isEmpty();
    }

    private static boolean usuarioExiste(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select count(*) from usuario where username = ?")) {
            ps.setString(1, username);
            try (ResultSet r = ps.executeQuery()) {
                return r.next() && r.getInt(1) > 0;
            }
        }
    }

    private static void inserirUsuario(Connection c, String username, String hash, String perfil)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "insert into usuario (username, senha, perfil) values (?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, perfil);
            ps.executeUpdate();
        }
    }

    private static int contarAulas(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select count(*) from materias");
             ResultSet r = ps.executeQuery()) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private static void inserirAula(Connection c, Aula a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "insert into materias (professor, materia, data, tema, conteudo) values (?, ?, ?, ?, ?)")) {
            ps.setString(1, PROFESSOR);
            ps.setString(2, a.idioma());
            ps.setString(3, a.data());
            ps.setString(4, a.tema());
            ps.setString(5, a.conteudo());
            ps.executeUpdate();
        }
    }
}
