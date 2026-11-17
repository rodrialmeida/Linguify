package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/** Painel em forma de card: fundo branco, cantos arredondados, borda suave e sombra leve. */
public class CardPanel extends JPanel {

    private final int arco;
    private Color corFundo = Tema.BRANCO;

    public CardPanel(LayoutManager layout) {
        this(layout, 20, 20);
    }

    public CardPanel(LayoutManager layout, int arco, int margemInterna) {
        super(layout);
        this.arco = arco;
        setOpaque(false);
        // 3px extras embaixo/lados para a sombra.
        setBorder(BorderFactory.createEmptyBorder(margemInterna, margemInterna, margemInterna + 3, margemInterna + 1));
    }

    public CardPanel comFundo(Color cor) {
        this.corFundo = cor;
        return this;
    }

    private Runnable acao;

    /** Torna o card clicavel (cursor de mao e destaque ao passar o mouse). */
    public CardPanel aoClicar(Runnable acao) {
        this.acao = acao;
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                clicar();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                corFundo = new Color(0xFBFAFF);
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                corFundo = Tema.BRANCO;
                repaint();
            }
        });
        return this;
    }

    /** Executa a acao do card (usado pelo clique e pelos testes). */
    public void clicar() {
        if (acao != null) {
            acao.run();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 2;
            int h = getHeight() - 4;
            g2.setColor(new Color(25, 27, 75, 10));
            g2.fillRoundRect(1, 3, w, h, arco, arco);
            g2.setColor(new Color(25, 27, 75, 8));
            g2.fillRoundRect(0, 2, w + 2, h + 1, arco + 2, arco + 2);
            g2.setColor(corFundo);
            g2.fillRoundRect(0, 0, w + 1, h, arco, arco);
            g2.setColor(Tema.BORDA);
            g2.drawRoundRect(0, 0, w + 1, h - 1, arco, arco);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
