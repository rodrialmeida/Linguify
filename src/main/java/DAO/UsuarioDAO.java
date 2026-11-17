package DAO;

import DTO.UsuarioDTO;
import auth.OperacaoUsuarioException;
import auth.RegrasUsuario;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import util.PasswordHasher;

/**
 * Acesso aos dados de usuarios.
 *
 * <p>Toda operacao administrativa:</p>
 * <ol>
 *   <li>exige uma {@link SessaoUsuario} ADMIN (verificado ANTES de abrir conexao);</li>
 *   <li>reconfirma no BANCO que o ator ainda existe e ainda e ADMIN;</li>
 *   <li>valida as entradas com {@link RegrasUsuario};</li>
 *   <li>grava somente hashes BCrypt e nunca devolve senha/hash.</li>
 * </ol>
 *
 * <p>Limitacao: as tabelas sao MyISAM (sem transacoes nem bloqueio de linha),
 * entao a protecao do ultimo administrador e "contar e depois agir", sem
 * atomicidade entre dois clientes simultaneos.</p>
 */
public class UsuarioDAO {

    /** Origem das conexoes (permite injetar uma conexao nos testes). */
    private final Supplier<Connection> conexaoSupplier;

    public UsuarioDAO() {
        this(() -> new ConexaoDAO().conectaBD());
    }

    /**
     * Construtor para testes: permite fornecer a origem das conexoes.
     *
     * @param conexaoSupplier fornece uma nova {@link Connection} a cada chamada
     *        (fechada pelo DAO ao final de cada operacao)
     */
    public UsuarioDAO(Supplier<Connection> conexaoSupplier) {
        this.conexaoSupplier = conexaoSupplier;
    }

    // ------------------------------------------------------------------
    // Autenticacao
    // ------------------------------------------------------------------

    /**
     * Autentica um usuario buscando-o pelo {@code username} e verificando a
     * senha informada contra o hash BCrypt armazenado. A senha NAO e comparada
     * no SQL: a verificacao do hash e feita em Java.
     *
     * @return o usuario autenticado (id, username e perfil; nunca a senha) ou
     *         {@code null} se o usuario nao existir ou a senha estiver incorreta
     * @throws OperacaoUsuarioException se o banco estiver indisponivel
     */
    public UsuarioDTO autenticar(String username, String senhaEmTexto) throws OperacaoUsuarioException {
        if (username == null || username.isBlank() || senhaEmTexto == null || senhaEmTexto.isEmpty()) {
            return null;
        }
        try (Connection c = Acesso.abrir(conexaoSupplier);
             PreparedStatement ps = c.prepareStatement(
                     "select id_usuario, username, senha, perfil from usuario where username = ?")) {
            ps.setString(1, username.trim());
            try (ResultSet r = ps.executeQuery()) {
                if (r.next() && PasswordHasher.verify(senhaEmTexto, r.getString("senha"))) {
                    UsuarioDTO autenticado = new UsuarioDTO();
                    autenticado.setId_usuario(r.getInt("id_usuario"));
                    autenticado.setUsername(r.getString("username"));
                    autenticado.setPerfil(r.getString("perfil"));
                    // Nao propaga o hash/senha para fora da camada de dados.
                    return autenticado;
                }
            }
            return null;
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("autenticar", e);
        }
    }

    // ------------------------------------------------------------------
    // API administrativa
    // ------------------------------------------------------------------

    /**
     * Lista os usuarios cadastrados. Somente ADMIN. O resultado traz id,
     * username e perfil; a senha/hash NUNCA e preenchida.
     */
    public List<UsuarioDTO> listarUsuarios(SessaoUsuario sessao)
            throws AcessoNegadoException, OperacaoUsuarioException {
        SessaoUsuario.exigirAdmin(sessao);

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            Acesso.confirmarAdminNoBanco(c, sessao);

            List<UsuarioDTO> resultado = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "select id_usuario, username, perfil from usuario order by id_usuario");
                 ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    UsuarioDTO dto = new UsuarioDTO();
                    dto.setId_usuario(r.getInt("id_usuario"));
                    dto.setUsername(r.getString("username"));
                    dto.setPerfil(r.getString("perfil"));
                    resultado.add(dto);
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("listar os usuários", e);
        }
    }

    /**
     * Cadastro ADMINISTRATIVO. Somente ADMIN. Aceita os perfis ADMIN e
     * ESTUDANTE; e o unico caminho para criar um usuario ADMIN.
     */
    public void cadastrarUsuario(SessaoUsuario sessao, UsuarioDTO dto)
            throws AcessoNegadoException, OperacaoUsuarioException {
        SessaoUsuario.exigirAdmin(sessao);

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            Acesso.confirmarAdminNoBanco(c, sessao);

            exigirDto(dto);
            String username = RegrasUsuario.validarUsername(dto.getUsername());
            String perfil = RegrasUsuario.normalizarPerfil(dto.getPerfil());
            RegrasUsuario.validarSenhaNova(dto.getSenha());

            inserirUsuario(c, username, dto.getSenha(), perfil);
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("cadastrar o usuário", e);
        }
    }

    /**
     * Cadastro PUBLICO (autocadastro). Nao exige sessao, mas SEMPRE cria o
     * usuario com perfil ESTUDANTE: o perfil contido no {@code dto} e ignorado.
     */
    public void cadastrarEstudante(UsuarioDTO dto) throws OperacaoUsuarioException {
        exigirDto(dto);
        String username = RegrasUsuario.validarUsername(dto.getUsername());
        RegrasUsuario.validarSenhaNova(dto.getSenha());

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            inserirUsuario(c, username, dto.getSenha(), SessaoUsuario.PERFIL_ESTUDANTE);
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("cadastrar o usuário", e);
        }
    }

    /**
     * Atualiza username e perfil de um usuario. Somente ADMIN. A senha so e
     * alterada (com novo hash) se {@code dto.getSenha()} vier preenchida; vazia
     * ou nula significa "manter a senha atual". Impede rebaixar o ultimo
     * administrador.
     */
    public void atualizarUsuario(SessaoUsuario sessao, UsuarioDTO dto)
            throws AcessoNegadoException, OperacaoUsuarioException {
        SessaoUsuario.exigirAdmin(sessao);

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            Acesso.confirmarAdminNoBanco(c, sessao);

            exigirDto(dto);
            String username = RegrasUsuario.validarUsername(dto.getUsername());
            String perfil = RegrasUsuario.normalizarPerfil(dto.getPerfil());
            boolean trocarSenha = RegrasUsuario.validarSenhaOpcional(dto.getSenha());

            UsuarioDTO atual = buscarPorId(c, dto.getId_usuario());
            if (atual == null) {
                throw new OperacaoUsuarioException("Usuário não encontrado.");
            }
            if (usernameEmUso(c, username, dto.getId_usuario())) {
                throw new OperacaoUsuarioException("Já existe um usuário com o nome \"" + username + "\".");
            }

            boolean alvoEraAdmin = SessaoUsuario.PERFIL_ADMIN.equalsIgnoreCase(atual.getPerfil());
            if (alvoEraAdmin) {
                RegrasUsuario.validarMudancaDeAdmin(true, perfil, contarAdmins(c));
            }

            String sql = trocarSenha
                    ? "update usuario set username = ?, perfil = ?, senha = ? where id_usuario = ?"
                    : "update usuario set username = ?, perfil = ? where id_usuario = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, perfil);
                if (trocarSenha) {
                    ps.setString(3, PasswordHasher.hash(dto.getSenha()));
                    ps.setInt(4, dto.getId_usuario());
                } else {
                    ps.setInt(3, dto.getId_usuario());
                }
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("atualizar o usuário", e);
        }
    }

    /**
     * Exclui um usuario. Somente ADMIN. Nao permite que o administrador exclua
     * a propria conta nem que o ultimo administrador seja removido.
     */
    public void excluirUsuario(SessaoUsuario sessao, int idUsuario)
            throws AcessoNegadoException, OperacaoUsuarioException {
        SessaoUsuario.exigirAdmin(sessao);

        try (Connection c = Acesso.abrir(conexaoSupplier)) {
            Acesso.confirmarAdminNoBanco(c, sessao);

            if (idUsuario == sessao.getUsuario().getId_usuario()) {
                throw new OperacaoUsuarioException("Você não pode excluir a sua própria conta.");
            }

            UsuarioDTO alvo = buscarPorId(c, idUsuario);
            if (alvo == null) {
                throw new OperacaoUsuarioException("Usuário não encontrado.");
            }

            boolean alvoEraAdmin = SessaoUsuario.PERFIL_ADMIN.equalsIgnoreCase(alvo.getPerfil());
            if (alvoEraAdmin) {
                RegrasUsuario.validarMudancaDeAdmin(true, null, contarAdmins(c));
            }

            try (PreparedStatement ps = c.prepareStatement("delete from usuario where id_usuario = ?")) {
                ps.setInt(1, idUsuario);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw Acesso.falhaDeBanco("excluir o usuário", e);
        }
    }

    // ---------------------------- auxiliares ----------------------------

    private void inserirUsuario(Connection c, String username, String senhaEmTexto, String perfil)
            throws OperacaoUsuarioException, SQLException {
        if (usernameEmUso(c, username, 0)) {
            throw new OperacaoUsuarioException("Já existe um usuário com o nome \"" + username + "\".");
        }
        // A senha em texto puro nunca chega ao banco.
        String hash = PasswordHasher.hash(senhaEmTexto);
        try (PreparedStatement ps = c.prepareStatement(
                "insert into usuario (username, senha, perfil) values (?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, perfil);
            ps.executeUpdate();
        }
    }

    /** @param idIgnorado id a desconsiderar na checagem (0 = nenhum; os ids reais comecam em 1) */
    private boolean usernameEmUso(Connection c, String username, int idIgnorado) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select 1 from usuario where username = ? and id_usuario <> ?")) {
            ps.setString(1, username);
            ps.setInt(2, idIgnorado);
            try (ResultSet r = ps.executeQuery()) {
                return r.next();
            }
        }
    }

    /** Retorna id, username e perfil do usuario (sem senha) ou {@code null} se nao existir. */
    private UsuarioDTO buscarPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "select id_usuario, username, perfil from usuario where id_usuario = ?")) {
            ps.setInt(1, id);
            try (ResultSet r = ps.executeQuery()) {
                if (!r.next()) {
                    return null;
                }
                UsuarioDTO dto = new UsuarioDTO();
                dto.setId_usuario(r.getInt("id_usuario"));
                dto.setUsername(r.getString("username"));
                dto.setPerfil(r.getString("perfil"));
                return dto;
            }
        }
    }

    private int contarAdmins(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select count(*) from usuario where perfil = ?")) {
            ps.setString(1, SessaoUsuario.PERFIL_ADMIN);
            try (ResultSet r = ps.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        }
    }

    private static void exigirDto(UsuarioDTO dto) throws OperacaoUsuarioException {
        if (dto == null) {
            throw new OperacaoUsuarioException("Dados do usuário não informados.");
        }
    }
}
