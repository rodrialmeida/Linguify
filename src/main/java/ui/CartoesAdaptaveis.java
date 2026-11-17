package ui;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

/** CardLayout que ocupa so o tamanho do cartao visivel (o padrao usa o maior de todos). */
public class CartoesAdaptaveis extends CardLayout {

    @Override
    public Dimension preferredLayoutSize(Container pai) {
        synchronized (pai.getTreeLock()) {
            Insets in = pai.getInsets();
            for (Component c : pai.getComponents()) {
                if (c.isVisible()) {
                    Dimension d = c.getPreferredSize();
                    return new Dimension(d.width + in.left + in.right, d.height + in.top + in.bottom);
                }
            }
            return super.preferredLayoutSize(pai);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container pai) {
        return preferredLayoutSize(pai);
    }
}
