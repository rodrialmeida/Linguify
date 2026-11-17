package auth;

/**
 * Indica que uma operacao sobre usuarios nao pode ser concluida: ou porque uma
 * regra de negocio foi violada (perfil invalido, senha curta, username duplicado,
 * ultimo administrador etc.) ou porque houve falha ao persistir os dados.
 *
 * <p>A mensagem e voltada ao usuario final e pode ser exibida diretamente na
 * interface. Falhas de infraestrutura trazem a causa original em
 * {@link #getCause()}.</p>
 */
public class OperacaoUsuarioException extends Exception {

    private static final long serialVersionUID = 1L;

    public OperacaoUsuarioException(String mensagem) {
        super(mensagem);
    }

    public OperacaoUsuarioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
