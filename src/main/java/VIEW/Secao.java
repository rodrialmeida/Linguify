package VIEW;

/** Uma secao da janela principal. */
public interface Secao {

    /** Chamado sempre que a secao passa a ser exibida; deve recarregar os dados. */
    void aoExibir();
}
