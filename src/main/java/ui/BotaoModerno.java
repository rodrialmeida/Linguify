package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/** Botao arredondado padronizado, com variantes de cor e estados (hover, pressionado, desabilitado). */
public class BotaoModerno extends JButton {

    public enum Variante { PRIMARIO, SECUNDARIO, PERIGO, FANTASMA }

    private final Variante variante;

    public BotaoModerno(String texto, Variante variante) {
        super(texto);
        this.variante = variante;
        setFont(Tema.fonte(java.awt.Font.BOLD, 13f));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(corTexto());
        setIconTextGap(8);
    }

    public Variante getVariante() {
        return variante;
    }

    private Color corTexto() {
        switch (variante) {
            case SECUNDARIO:
                return Tema.TEXTO;
            case FANTASMA:
                return Tema.ROXO;
            default:
                return Tema.BRANCO;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 40));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean ativo = isEnabled();
            boolean pressionado = ativo && getModel().isPressed();
            boolean sobre = ativo && getModel().isRollover();
            int w = getWidth() - 1;
            int h = getHeight() - 1;

            Color fundo;
            Color borda = null;
            switch (variante) {
                case SECUNDARIO:
                    fundo = pressionado ? new Color(0xE9EBF5) : sobre ? new Color(0xF3F4FA) : Tema.BRANCO;
                    borda = Tema.BORDA;
                    break;
                case PERIGO:
                    fundo = pressionado ? new Color(0x9E2A34) : sobre ? Tema.ERRO_ESCURO : Tema.ERRO;
                    break;
                case FANTASMA:
                    fundo = pressionado ? new Color(0xE2DEFF) : sobre ? Tema.ROXO_SUAVE : new Color(0, 0, 0, 0);
                    break;
                default:
                    fundo = pressionado ? new Color(0x4F40BC) : sobre ? Tema.ROXO_ESCURO : Tema.ROXO;
                    break;
            }
            if (!ativo) {
                fundo = variante == Variante.FANTASMA ? new Color(0, 0, 0, 0) : new Color(0xD9DBE8);
                borda = variante == Variante.SECUNDARIO ? Tema.BORDA : null;
            }
            g2.setColor(fundo);
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            if (borda != null) {
                g2.setColor(borda);
                g2.drawRoundRect(0, 0, w, h, 14, 14);
            }
        } finally {
            g2.dispose();
        }
        setForeground(isEnabled() ? corTexto() : Tema.CINZA);
        super.paintComponent(g);
    }
}
