package DAO;

import DTO.MateriasDTO;
import auth.OperacaoUsuarioException;
import auth.RegrasAula;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Acesso aos dados de aulas e materias (tabela {@code materias}).
 *
 * <ul>
 *   <li>Leitura: qualquer usuario autenticado (ADMIN ou ESTUDANTE).</li>
 *   <li>Cadastro, edicao e exclusao: somente ADMIN, verificado na sessao e
 *       reconfirmado no banco antes de cada operacao.</li>
 * </ul>
 * Uma aula e identificada pela combinacao idioma ({@code materia}) + nome
 * ({@code tema}); nao se permite duplicar essa combinacao.
 */
public class MateriasDAO {

    private static final String COLUNAS = "id_materias, materia, tema, professor, data, conteudo";

    private final Supplier<Connection> conexaoSupplier;

    public MateriasDAO() {
        this(() -> new ConexaoDAO().conectaBD());
    }

    /** Construtor para testes: permite fornecer a origem das conexoes. */
    public MateriasDAO(Supplier<Connection> conexaoSupplier) {
        this.conexaoSupplier = conexaoSupplier;
    }

    /** Lista todas as aulas, ordenadas por idioma e depois por id. Valores nulos viram texto vazio. */
    public List<MateriasDTO> listarAulas(SessaoUsuario sessao)
            throws AcessoNegadoException, OperacaoUsuarioException {
        exigirSessao(sessao);

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            Acesso.confirmarUsuarioNoBanco(c, sessao);

            List<MateriasDTO> resultado = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select " + COLUNAS + " from materias order by materia, id_materias");
                 ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    MateriasDTO aula = new MateriasDTO();
                    aula.setId_materias(r.getInt("id_materias"));
                    aula.setMateria(vazioSeNulo(r.getString("materia")));
                    aula.setTema(vazioSeNulo(r.getString("tema")));
                    aula.setProfessor(vazioSeNulo(r.getString("professor")));
                    aula.setData(vazioSeNulo(r.getString("data")));
                    aula.setConteudo(vazioSeNulo(r.getString("conteudo")));
                    resultado.add(aula);
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("listar as aulas", e);
        }
    }

    /** Cadastra uma aula. Somente ADMIN. */
    public void cadastrarAula(SessaoUsuario sessao, MateriasDTO aula)
            throws AcessoNegadoException, OperacaoUsuarioException {
        SessaoUsuario.exigirAdmin(sessao);

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            Acesso.confirmarAdminNoBanco(c, sessao);

            MateriasDTO limpa = RegrasAula.validar(aula);
            if (existeAula(c, limpa.getMateria(), limpa.getTema(), 0)) {
                throw duplicada(limpa);
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "insert into materias (professor, materia, data, tema, conteudo) values (?, ?, ?, ?, ?)")) {
                ps.setString(1, limpa.getProfessor());
                ps.setString(2, limpa.getMateria());
                ps.setString(3, limpa.getData());
                ps.setString(4, limpa.getTema());
                ps.setString(5, limpa.getConteudo());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("cadastrar a aula", e);
        }
    }

    /** Atualiza todos os campos de uma aula existente. Somente ADMIN. */
    public void atualizarAula(SessaoUsuario sessao, MateriasDTO aula)
            throws AcessoNegadoException, OperacaoUsuarioException {
        SessaoUsuario.exigirAdmin(sessao);

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            Acesso.confirmarAdminNoBanco(c, sessao);

            MateriasDTO limpa = RegrasAula.validar(aula);
            if (!existeId(c, limpa.getId_materias())) {
                throw new OperacaoUsuarioException("Aula não encontrada. Atualize a lista e tente novamente.");
            }
            if (existeAula(c, limpa.getMateria(), limpa.getTema(), limpa.getId_materias())) {
                throw duplicada(limpa);
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "update materias set professor = ?, materia = ?, data = ?, tema = ?, conteudo = ? "
                    + "where id_materias = ?")) {
                ps.setString(1, limpa.getProfessor());
                ps.setString(2, limpa.getMateria());
                ps.setString(3, limpa.getData());
                ps.setString(4, limpa.getTema());
                ps.setString(5, limpa.getConteudo());
                ps.setInt(6, limpa.getId_materias());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("atualizar a aula", e);
        }
    }

    /** Exclui UMA aula pelo id (nunca em massa). Somente ADMIN. */
    public void excluirAula(SessaoUsuario sessao, int idAula)
            throws AcessoNegadoException, OperacaoUsuarioException {
        SessaoUsuario.exigirAdmin(sessao);

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            Acesso.confirmarAdminNoBanco(c, sessao);

            try (PreparedStatement ps = c.prepareStatement("delete from materias where id_materias = ?")) {
                ps.setInt(1, idAula);
                if (ps.executeUpdate() == 0) {
                    throw new OperacaoUsuarioException("Aula não encontrada. Atualize a lista e tente novamente.");
                }
            }
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("excluir a aula", e);
        }
    }

    // ---------------------------- auxiliares ----------------------------

    private static void exigirSessao(SessaoUsuario sessao) throws AcessoNegadoException {
        if (sessao == null) {
            throw new AcessoNegadoException("Acesso negado: faça login para ver as aulas.");
        }
    }

    private static String vazioSeNulo(String valor) {
        return valor == null ? "" : valor;
    }

    private static OperacaoUsuarioException duplicada(MateriasDTO aula) {
        return new OperacaoUsuarioException(
                "Já existe a aula \"" + aula.getTema() + "\" em " + aula.getMateria() + ".");
    }

    private boolean existeId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select 1 from materias where id_materias = ?")) {
            ps.setInt(1, id);
            try (ResultSet r = ps.executeQuery()) {
                return r.next();
            }
        }
    }

    /** @param idIgnorado id a desconsiderar (0 = nenhum; os ids reais comecam em 1) */
    private boolean existeAula(Connection c, String idioma, String tema, int idIgnorado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select 1 from materias where materia = ? and tema = ? and id_materias <> ?")) {
            ps.setString(1, idioma);
            ps.setString(2, tema);
            ps.setInt(3, idIgnorado);
            try (ResultSet r = ps.executeQuery()) {
                return r.next();
            }
        }
    }
}
