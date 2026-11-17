package DAO;

import auth.OperacaoUsuarioException;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Rotinas de acesso compartilhadas pelos DAOs: abertura de conexao e
 * reconfirmacao, no banco, de quem esta executando a operacao.
 */
final class Acesso {

    private Acesso() {
    }

    static Connection abrir(Supplier<Connection> origem) throws OperacaoUsuarioException {
        Connection c;
        try {
            c = origem.get();
        } catch (RuntimeException e) {
            throw new OperacaoUsuarioException("Não foi possível conectar ao banco de dados.", e);
        }
        if (c == null) {
            throw new OperacaoUsuarioException(
                    "Não foi possível conectar ao banco de dados. Verifique se o MySQL está em execução.");
        }
        return c;
    }

    /**
     * Reconfirma no banco que o ator ainda existe e ainda e ADMIN, sem confiar
     * no perfil guardado em memoria (que pode estar desatualizado).
     */
    static void confirmarAdminNoBanco(Connection c, SessaoUsuario sessao)
            throws AcessoNegadoException, OperacaoUsuarioException {
        String perfil = perfilNoBanco(c, sessao);
        if (perfil == null) {
            throw new AcessoNegadoException("Acesso negado: o usuário autenticado não existe mais.");
        }
        if (!SessaoUsuario.PERFIL_ADMIN.equalsIgnoreCase(perfil)) {
            throw new AcessoNegadoException(
                    "Acesso restrito a administradores: o perfil atual do usuário não é ADMIN.");
        }
    }

    /** Para operacoes de leitura: basta ser um usuario autenticado que ainda exista. */
    static void confirmarUsuarioNoBanco(Connection c, SessaoUsuario sessao)
            throws AcessoNegadoException, OperacaoUsuarioException {
        if (perfilNoBanco(c, sessao) == null) {
            throw new AcessoNegadoException("Acesso negado: o usuário autenticado não existe mais.");
        }
    }

    private static String perfilNoBanco(Connection c, SessaoUsuario sessao) throws OperacaoUsuarioException {
        try (PreparedStatement ps = c.prepareStatement("select perfil from usuario where id_usuario = ?")) {
            ps.setInt(1, sessao.getUsuario().getId_usuario());
            try (ResultSet r = ps.executeQuery()) {
                if (!r.next()) {
                    return null;
                }
                String perfil = r.getString("perfil");
                return perfil == null ? "" : perfil;
            }
        } catch (SQLException e) {
            throw falhaDeBanco("verificar a permissão", e);
        }
    }

    static OperacaoUsuarioException falhaDeBanco(String acao, SQLException causa) {
        return new OperacaoUsuarioException("Erro no banco de dados ao " + acao + ".", causa);
    }
}
