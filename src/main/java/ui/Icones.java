package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Icones vetoriais simples (tracos arredondados), desenhados em Java2D numa
 * grade de 24x24. Nao dependem de arquivos nem de fontes do sistema.
 */
public final class Icones {

    public enum Tipo {
        DASHBOARD, LIVRO, USUARIOS, USUARIO, SAIR, OLHO, OLHO_CORTADO, BUSCA, MAIS, EDITAR,
        LIXEIRA, VOLTAR, CADEADO, CHECK, ALERTA, ERRO, INFO, SETA
    }

    private Icones() {
    }

    public static Icon icone(Tipo tipo, int tamanho, Color cor) {
        return new IconeVetorial(tipo, tamanho, cor);
    }

    /** Icone da aplicacao (janelas e barra de tarefas): quadrado roxo arredondado com um livro. */
    public static java.awt.Image iconeApp(int lado) {
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(lado, lado, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Tema.ROXO);
            int arco = (int) (lado * 0.28);
            g.fillRoundRect(0, 0, lado, lado, arco, arco);
            int miolo = (int) (lado * 0.6);
            icone(Tipo.LIVRO, miolo, Tema.BRANCO).paintIcon(null, g, (lado - miolo) / 2, (lado - miolo) / 2);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static final class IconeVetorial implements Icon {
        private final Tipo tipo;
        private final int tamanho;
        private final Color cor;

        IconeVetorial(Tipo tipo, int tamanho, Color cor) {
            this.tipo = tipo;
            this.tamanho = tamanho;
            this.cor = cor;
        }

        @Override
        public int getIconWidth() {
            return tamanho;
        }

        @Override
        public int getIconHeight() {
            return tamanho;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double escala = tamanho / 24.0;
                g2.scale(escala, escala);
                g2.setColor(cor);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                desenhar(g2, tipo);
            } finally {
                g2.dispose();
            }
        }
    }

    private static Path2D.Double caminho(double... xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        return p;
    }

    private static void linha(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void ponto(Graphics2D g, double x, double y) {
        g.fill(new Ellipse2D.Double(x - 1.2, y - 1.2, 2.4, 2.4));
    }

    private static void desenhar(Graphics2D g, Tipo tipo) {
        switch (tipo) {
            case DASHBOARD:
                g.draw(new RoundRectangle2D.Double(3, 3, 7.5, 7.5, 2, 2));
                g.draw(new RoundRectangle2D.Double(13.5, 3, 7.5, 7.5, 2, 2));
                g.draw(new RoundRectangle2D.Double(3, 13.5, 7.5, 7.5, 2, 2));
                g.draw(new RoundRectangle2D.Double(13.5, 13.5, 7.5, 7.5, 2, 2));
                break;
            case LIVRO: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(12, 6.5);
                p.curveTo(9.5, 4.5, 6, 4.2, 3, 5);
                p.lineTo(3, 18.5);
                p.curveTo(6, 17.7, 9.5, 18, 12, 20);
                p.curveTo(14.5, 18, 18, 17.7, 21, 18.5);
                p.lineTo(21, 5);
                p.curveTo(18, 4.2, 14.5, 4.5, 12, 6.5);
                g.draw(p);
                linha(g, 12, 6.5, 12, 20);
                break;
            }
            case USUARIOS: {
                g.draw(new Ellipse2D.Double(5, 4, 7, 7));
                Path2D.Double corpo = new Path2D.Double();
                corpo.moveTo(2.5, 20);
                corpo.curveTo(2.5, 15.5, 5.5, 14, 8.5, 14);
                corpo.curveTo(11.5, 14, 14.5, 15.5, 14.5, 20);
                g.draw(corpo);
                Path2D.Double outro = new Path2D.Double();
                outro.moveTo(15.5, 4.3);
                outro.curveTo(18.5, 4.3, 19.5, 8.5, 17, 10.3);
                g.draw(outro);
                Path2D.Double outroCorpo = new Path2D.Double();
                outroCorpo.moveTo(17.5, 14.3);
                outroCorpo.curveTo(20, 14.8, 21.5, 16.5, 21.5, 20);
                g.draw(outroCorpo);
                break;
            }
            case USUARIO: {
                g.draw(new Ellipse2D.Double(8, 3.5, 8, 8));
                Path2D.Double corpo = new Path2D.Double();
                corpo.moveTo(4.5, 21);
                corpo.curveTo(4.5, 16.5, 8, 14.5, 12, 14.5);
                corpo.curveTo(16, 14.5, 19.5, 16.5, 19.5, 21);
                g.draw(corpo);
                break;
            }
            case SAIR:
                g.draw(caminho(10, 4, 5, 4, 5, 20, 10, 20));
                linha(g, 10, 12, 20, 12);
                g.draw(caminho(16, 8, 20, 12, 16, 16));
                break;
            case OLHO:
            case OLHO_CORTADO: {
                Path2D.Double p = new Path2D.Double();
                p.moveTo(2, 12);
                p.curveTo(5, 6, 9, 5, 12, 5);
                p.curveTo(15, 5, 19, 6, 22, 12);
                p.curveTo(19, 18, 15, 19, 12, 19);
                p.curveTo(9, 19, 5, 18, 2, 12);
                g.draw(p);
                g.draw(new Ellipse2D.Double(9, 9, 6, 6));
                if (tipo == Tipo.OLHO_CORTADO) {
                    linha(g, 4, 20, 20, 4);
                }
                break;
            }
            case BUSCA:
                g.draw(new Ellipse2D.Double(4, 4, 12, 12));
                linha(g, 14.5, 14.5, 20, 20);
                break;
            case MAIS:
                linha(g, 12, 5, 12, 19);
                linha(g, 5, 12, 19, 12);
                break;
            case EDITAR: {
                Path2D.Double p = caminho(4, 20, 4, 16, 16, 4, 20, 8, 8, 20);
                p.closePath();
                g.draw(p);
                linha(g, 13, 7, 17, 11);
                break;
            }
            case LIXEIRA:
                linha(g, 4, 7, 20, 7);
                g.draw(caminho(9, 7, 9, 4, 15, 4, 15, 7));
                g.draw(caminho(6, 7, 6, 20, 18, 20, 18, 7));
                linha(g, 10, 11, 10, 16);
                linha(g, 14, 11, 14, 16);
                break;
            case VOLTAR:
                linha(g, 20, 12, 5, 12);
                g.draw(caminho(11, 6, 5, 12, 11, 18));
                break;
            case SETA:
                linha(g, 4, 12, 19, 12);
                g.draw(caminho(13, 6, 19, 12, 13, 18));
                break;
            case CADEADO:
                g.draw(new RoundRectangle2D.Double(5, 11, 14, 10, 3, 3));
                g.draw(caminho(8, 11, 8, 8, 9, 5.5, 12, 4.5, 15, 5.5, 16, 8, 16, 11));
                ponto(g, 12, 16);
                break;
            case CHECK:
                g.draw(caminho(5, 12.5, 10, 17, 19, 7));
                break;
            case ALERTA: {
                Path2D.Double p = caminho(12, 3.5, 21.5, 20, 2.5, 20);
                p.closePath();
                g.draw(p);
                linha(g, 12, 9.5, 12, 14);
                ponto(g, 12, 17);
                break;
            }
            case ERRO:
                linha(g, 6, 6, 18, 18);
                linha(g, 18, 6, 6, 18);
                break;
            case INFO:
                linha(g, 12, 11, 12, 17.5);
                ponto(g, 12, 7);
                break;
            default:
                break;
        }
    }
}
