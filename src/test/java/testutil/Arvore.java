package testutil;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;

/** Busca de componentes Swing dentro de uma arvore (para os testes de interface). */
public final class Arvore {

    private Arvore() {
    }

    public static <T extends Component> List<T> todos(Container raiz, Class<T> tipo) {
        List<T> resultado = new ArrayList<>();
        for (Component filho : raiz.getComponents()) {
            if (tipo.isInstance(filho)) {
                resultado.add(tipo.cast(filho));
            }
            if (filho instanceof Container container) {
                resultado.addAll(todos(container, tipo));
            }
        }
        return resultado;
    }

    /**
     * Textos dos JLabel (sem HTML) e botoes que estao VISIVEIS dentro de {@code raiz}
     * (componentes ocultos e cartoes inativos de um CardLayout nao contam).
     */
    public static List<String> textos(Container raiz) {
        List<String> textos = new ArrayList<>();
        for (JLabel l : todos(raiz, JLabel.class)) {
            if (l.getText() != null && visivel(l, raiz)) {
                textos.add(semHtml(l.getText()));
            }
        }
        for (AbstractButton b : todos(raiz, AbstractButton.class)) {
            if (b.getText() != null && visivel(b, raiz)) {
                textos.add(semHtml(b.getText()));
            }
        }
        return textos;
    }

    /** Texto exatamente igual. */
    public static boolean temTexto(Container raiz, String texto) {
        return textos(raiz).stream().anyMatch(t -> t.trim().equals(texto));
    }

    /** Algum texto visivel que contenha o trecho informado. */
    public static boolean contemTexto(Container raiz, String trecho) {
        return textos(raiz).stream().anyMatch(t -> t.contains(trecho));
    }

    private static boolean visivel(Component c, Container raiz) {
        for (Component atual = c; atual != null && atual != raiz; atual = atual.getParent()) {
            if (!atual.isVisible()) {
                return false;
            }
        }
        return true;
    }

    public static AbstractButton botao(Container raiz, String texto) {
        return todos(raiz, AbstractButton.class).stream()
                .filter(b -> texto.equals(b.getText())).findFirst().orElse(null);
    }

    private static String semHtml(String s) {
        return s.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}
