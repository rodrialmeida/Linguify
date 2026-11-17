package auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Regras de negocio puras sobre usuarios (sem acesso a banco ou interface).
 *
 * <p>Centraliza as validacoes de perfil, username e senha e a protecao do
 * ultimo administrador, para que possam ser testadas isoladamente e reutilizadas
 * pela camada de dados.</p>
 */
public final class RegrasUsuario {

    /** Tamanho maximo do username (coluna {@code usuario.username} e VARCHAR(50)). */
    public static final int USERNAME_MAX_CARACTERES = 50;

    /** Tamanho minimo da senha, em caracteres. */
    public static final int SENHA_MIN_CARACTERES = 6;

    /** Tamanho maximo da senha, em bytes UTF-8 (o BCrypt ignora o que passa de 72 bytes). */
    public static final int SENHA_MAX_BYTES = 72;

    private RegrasUsuario() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /**
     * Valida e normaliza um perfil: remove espacos nas pontas e converte para
     * maiusculas. Somente {@code ADMIN} e {@code ESTUDANTE} sao aceitos.
     *
     * @return o perfil normalizado ({@code "ADMIN"} ou {@code "ESTUDANTE"})
     * @throws OperacaoUsuarioException se o perfil for nulo, vazio ou desconhecido
     */
    public static String normalizarPerfil(String perfil) throws OperacaoUsuarioException {
        if (perfil == null || perfil.isBlank()) {
            throw new OperacaoUsuarioException("O perfil do usuário é obrigatório.");
        }
        String normalizado = perfil.trim().toUpperCase(Locale.ROOT);
        if (SessaoUsuario.PERFIL_ADMIN.equals(normalizado)
                || SessaoUsuario.PERFIL_ESTUDANTE.equals(normalizado)) {
            return normalizado;
        }
        throw new OperacaoUsuarioException(
                "Perfil inválido: " + perfil.trim() + ". Use ADMIN ou ESTUDANTE.");
    }

    /**
     * Valida o username e devolve o valor sem espacos nas pontas.
     *
     * @throws OperacaoUsuarioException se for nulo, vazio ou passar do limite da coluna
     */
    public static String validarUsername(String username) throws OperacaoUsuarioException {
        if (username == null || username.isBlank()) {
            throw new OperacaoUsuarioException("O nome de usuário é obrigatório.");
        }
        String limpo = username.trim();
        if (limpo.codePointCount(0, limpo.length()) > USERNAME_MAX_CARACTERES) {
            throw new OperacaoUsuarioException(
                    "O nome de usuário deve ter no máximo " + USERNAME_MAX_CARACTERES + " caracteres.");
        }
        return limpo;
    }

    /**
     * Valida uma senha nova (obrigatoria): minimo de {@value #SENHA_MIN_CARACTERES}
     * caracteres e maximo de {@value #SENHA_MAX_BYTES} bytes em UTF-8.
     *
     * @throws OperacaoUsuarioException se a senha for nula, vazia, curta ou longa demais
     */
    public static void validarSenhaNova(String senha) throws OperacaoUsuarioException {
        if (senha == null || senha.isEmpty()) {
            throw new OperacaoUsuarioException("A senha é obrigatória.");
        }
        if (senha.codePointCount(0, senha.length()) < SENHA_MIN_CARACTERES) {
            throw new OperacaoUsuarioException(
                    "A senha deve ter no mínimo " + SENHA_MIN_CARACTERES + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAX_BYTES) {
            throw new OperacaoUsuarioException(
                    "A senha deve ter no máximo " + SENHA_MAX_BYTES + " bytes (use uma senha mais curta).");
        }
    }

    /**
     * Valida uma senha opcional, usada na edicao de usuarios: senha ausente ou
     * vazia significa "manter a senha atual".
     *
     * @return {@code true} se uma senha nova foi informada (e e valida);
     *         {@code false} se a senha atual deve ser mantida
     * @throws OperacaoUsuarioException se uma senha foi informada mas e invalida
     */
    public static boolean validarSenhaOpcional(String senha) throws OperacaoUsuarioException {
        if (senha == null || senha.isEmpty()) {
            return false;
        }
        validarSenhaNova(senha);
        return true;
    }

    /**
     * Impede a remocao ou o rebaixamento do ultimo administrador.
     *
     * <p>Nao existe coluna de situacao no banco, entao "administrador ativo"
     * significa "registro com perfil ADMIN".</p>
     *
     * @param alvoEraAdmin  se o usuario afetado e ADMIN hoje
     * @param novoPerfil    perfil que o usuario tera apos a operacao; {@code null}
     *                      indica remocao do usuario
     * @param totalAdmins   quantidade atual de administradores, incluindo o alvo
     * @throws OperacaoUsuarioException se a operacao deixaria o sistema sem administradores
     */
    public static void validarMudancaDeAdmin(boolean alvoEraAdmin, String novoPerfil, int totalAdmins)
            throws OperacaoUsuarioException {
        if (!alvoEraAdmin) {
            return;
        }
        boolean continuaAdmin = novoPerfil != null
                && SessaoUsuario.PERFIL_ADMIN.equalsIgnoreCase(novoPerfil.trim());
        if (continuaAdmin) {
            return;
        }
        if (totalAdmins <= 1) {
            throw new OperacaoUsuarioException(
                    "Operação bloqueada: este é o último administrador do sistema. "
                    + "Cadastre ou promova outro administrador antes de removê-lo ou rebaixá-lo.");
        }
    }
}
