package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitario para hashing e verificacao de senhas usando BCrypt (jBCrypt).
 *
 * <p>As senhas nunca sao armazenadas em texto puro: o metodo {@link #hash(String)}
 * gera um hash BCrypt (60 caracteres, prefixo "$2a$") a partir da senha em texto,
 * e {@link #verify(String, String)} verifica uma senha em texto contra um hash
 * previamente armazenado.</p>
 */
public final class PasswordHasher {

    /** Fator de custo (work factor) do BCrypt. 10 e um padrao seguro e equilibrado. */
    private static final int COST = 10;

    private PasswordHasher() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    /**
     * Gera um hash BCrypt para a senha informada.
     *
     * @param senhaEmTexto senha em texto puro (nao nula)
     * @return hash BCrypt (60 caracteres)
     */
    public static String hash(String senhaEmTexto) {
        if (senhaEmTexto == null) {
            throw new IllegalArgumentException("A senha nao pode ser nula.");
        }
        return BCrypt.hashpw(senhaEmTexto, BCrypt.gensalt(COST));
    }

    /**
     * Verifica se a senha em texto corresponde ao hash armazenado.
     *
     * @param senhaEmTexto senha em texto puro informada pelo usuario
     * @param hashArmazenado hash BCrypt vindo do banco de dados
     * @return {@code true} se a senha confere; {@code false} caso contrario
     */
    public static boolean verify(String senhaEmTexto, String hashArmazenado) {
        if (senhaEmTexto == null || hashArmazenado == null || hashArmazenado.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(senhaEmTexto, hashArmazenado);
        } catch (IllegalArgumentException e) {
            // Hash em formato invalido (ex.: senha antiga em texto puro). Nao autentica.
            return false;
        }
    }
}
