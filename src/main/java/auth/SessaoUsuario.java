package auth;

import DTO.UsuarioDTO;

/**
 * Representa a sessao do usuario autenticado e centraliza a verificacao de
 * permissoes baseada no perfil (ADMIN ou ESTUDANTE).
 *
 * <p>A diferenciacao de permissoes NAO e baseada no nome de usuario: ela usa
 * exclusivamente o campo {@code perfil} recuperado do banco de dados no momento
 * da autenticacao.</p>
 */
public class SessaoUsuario {

    public static final String PERFIL_ADMIN = "ADMIN";
    public static final String PERFIL_ESTUDANTE = "ESTUDANTE";

    private final UsuarioDTO usuario;

    public SessaoUsuario(UsuarioDTO usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Sessao requer um usuario autenticado.");
        }
        this.usuario = usuario;
    }

    public String getUsername() {
        return usuario.getUsername();
    }

    public String getPerfil() {
        return usuario.getPerfil();
    }

    public UsuarioDTO getUsuario() {
        return usuario;
    }

    /**
     * @return {@code true} se o usuario autenticado tem perfil ADMIN.
     */
    public boolean isAdmin() {
        return PERFIL_ADMIN.equalsIgnoreCase(usuario.getPerfil());
    }

    /**
     * @return {@code true} se o usuario autenticado tem perfil ESTUDANTE.
     */
    public boolean isEstudante() {
        return PERFIL_ESTUDANTE.equalsIgnoreCase(usuario.getPerfil());
    }

    /**
     * Garante que o usuario atual possui permissao administrativa.
     *
     * @throws AcessoNegadoException se o usuario nao for ADMIN
     */
    public void exigirAdmin() throws AcessoNegadoException {
        if (!isAdmin()) {
            throw new AcessoNegadoException(
                "Acesso restrito a administradores. Perfil atual: " + usuario.getPerfil());
        }
    }

    /**
     * Versao estatica e tolerante a {@code null} de {@link #exigirAdmin()}.
     * Usada pela camada de dados: uma sessao ausente equivale a acesso negado.
     *
     * @param sessao sessao do ator da operacao (pode ser {@code null})
     * @throws AcessoNegadoException se a sessao for nula ou nao for ADMIN
     */
    public static void exigirAdmin(SessaoUsuario sessao) throws AcessoNegadoException {
        if (sessao == null) {
            throw new AcessoNegadoException(
                "Acesso restrito a administradores: nenhum usuario autenticado.");
        }
        sessao.exigirAdmin();
    }

    /**
     * Excecao lancada quando um usuario tenta acessar uma funcionalidade sem
     * permissao suficiente.
     */
    public static class AcessoNegadoException extends Exception {
        public AcessoNegadoException(String mensagem) {
            super(mensagem);
        }
    }
}
