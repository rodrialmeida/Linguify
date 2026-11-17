package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/** Circulo colorido com a inicial do nome (ou um icone) usado como foto de perfil. */
public class Avatar extends JComponent {

    private final String inicial;
    private final int tamanho;
    private final Color cor;

    public Avatar(String nome, int tamanho, Color cor) {
        this.inicial = nome == null || nome.isBlank() ? "?" : nome.trim().substring(0, 1).toUpperCase();
        this.tamanho = tamanho;
        this.cor = cor;
        setPreferredSize(new Dimension(tamanho, tamanho));
        setMinimumSize(new Dimension(tamanho, tamanho));
        setMaximumSize(new Dimension(tamanho, tamanho));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(cor);
            g2.fillOval(0, 0, tamanho - 1, tamanho - 1);
            g2.setColor(Tema.BRANCO);
            g2.setFont(Tema.fonte(java.awt.Font.BOLD, tamanho * 0.42f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(inicial, (tamanho - fm.stringWidth(inicial)) / 2,
                    (tamanho - fm.getHeight()) / 2 + fm.getAscent());
        } finally {
            g2.dispose();
        }
    }
}
