package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/** Item do menu lateral: ao ser selecionado ganha um destaque roxo arredondado. */
public class BotaoMenu extends JToggleButton {

    public BotaoMenu(String texto, Icones.Tipo icone) {
        super(texto);
        setIcon(Icones.icone(icone, 20, Tema.ROXO_CLARO));
        setSelectedIcon(Icones.icone(icone, 20, Tema.BRANCO));
        setFont(Tema.fonte(java.awt.Font.BOLD, 14f));
        setForeground(new Color(0xD5D7F2));
        setHorizontalAlignment(SwingConstants.LEFT);
        setIconTextGap(14);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAlignmentX(LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g2.setColor(Tema.ROXO);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            } else if (getModel().isRollover()) {
                g2.setColor(Tema.NAVY_CLARO);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            }
        } finally {
            g2.dispose();
        }
        setForeground(isSelected() ? Tema.BRANCO : new Color(0xD5D7F2));
        super.paintComponent(g);
    }
}
