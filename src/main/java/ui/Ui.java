package ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Fabrica de componentes padronizados: toda tela monta sua interface por aqui. */
public final class Ui {

    private Ui() {
    }

    // ------------------------------- textos -------------------------------

    public static JLabel titulo(String texto) {
        return rotulo(texto, Font.BOLD, 26f, Tema.NAVY);
    }

    public static JLabel secao(String texto) {
        return rotulo(texto, Font.BOLD, 16f, Tema.NAVY);
    }

    public static JLabel subtitulo(String texto) {
        return rotulo(texto, Font.PLAIN, 14f, Tema.CINZA);
    }

    public static JLabel rotuloCampo(String texto) {
        return rotulo(texto, Font.BOLD, 12f, Tema.TEXTO);
    }

    public static JLabel texto(String texto) {
        return rotulo(texto, Font.PLAIN, 13.5f, Tema.TEXTO);
    }

    public static JLabel rotulo(String texto, int estilo, float tamanho, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fonte(estilo, tamanho));
        l.setForeground(cor);
        return l;
    }

    /** Escapa HTML para uso em rotulos com texto multilinha. */
    public static String html(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    /** Pequena etiqueta arredondada (ex.: idioma, perfil). */
    public static JLabel chip(String texto, Color fundo, Color corTexto) {
        JLabel l = new JLabel(texto, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setBackground(fundo);
        l.setForeground(corTexto);
        l.setFont(Tema.fonte(Font.BOLD, 12f));
        l.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        return l;
    }

    // ------------------------------ painéis ------------------------------

    public static JPanel transparente(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static Border margem(int topo, int esquerda, int base, int direita) {
        return BorderFactory.createEmptyBorder(topo, esquerda, base, direita);
    }

    // ------------------------------- campos -------------------------------

    public static JTextField campo(String placeholder, Icones.Tipo iconeInicial) {
        JTextField f = new JTextField();
        estilizarCampo(f, placeholder, iconeInicial);
        return f;
    }

    /** Campo de senha com botao de mostrar/ocultar (olho). */
    public static JPasswordField campoSenha(String placeholder) {
        JPasswordField f = new JPasswordField();
        estilizarCampo(f, placeholder, Icones.Tipo.CADEADO);
        char oculto = f.getEchoChar() == 0 ? '\u2022' : f.getEchoChar();
        f.setEchoChar(oculto);

        Icon olho = Icones.icone(Icones.Tipo.OLHO, 18, Tema.CINZA);
        Icon olhoCortado = Icones.icone(Icones.Tipo.OLHO_CORTADO, 18, Tema.ROXO);
        JToggleButton alternar = new JToggleButton(olho);
        alternar.setSelectedIcon(olhoCortado);
        alternar.putClientProperty("JButton.buttonType", "toolBarButton");
        alternar.setToolTipText("Mostrar ou ocultar a senha");
        alternar.setFocusable(false);
        alternar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        alternar.addActionListener(e -> f.setEchoChar(alternar.isSelected() ? (char) 0 : oculto));
        f.putClientProperty("JTextField.trailingComponent", alternar);
        return f;
    }

    private static void estilizarCampo(JTextField f, String placeholder, Icones.Tipo iconeInicial) {
        f.setFont(Tema.fonte(Font.PLAIN, 14f));
        f.setForeground(Tema.TEXTO);
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.putClientProperty("JComponent.minimumHeight", 42);
        if (iconeInicial != null) {
            f.putClientProperty("JTextField.leadingIcon", Icones.icone(iconeInicial, 18, Tema.CINZA));
        }
    }

    public static JTextArea areaTexto(int linhas) {
        JTextArea a = new JTextArea(linhas, 20);
        a.setFont(Tema.fonte(Font.PLAIN, 14f));
        a.setForeground(Tema.TEXTO);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return a;
    }

    /** Area de texto dentro de uma moldura arredondada, no mesmo estilo dos campos. */
    public static JScrollPane areaComMoldura(JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createLineBorder(Tema.BORDA, 1, true));
        sp.putClientProperty("JScrollPane.smoothScrolling", true);
        return sp;
    }

    public static <T> JComboBox<T> combo(T[] itens) {
        JComboBox<T> c = new JComboBox<>(itens);
        c.setFont(Tema.fonte(Font.PLAIN, 14f));
        c.putClientProperty("JComponent.minimumHeight", 42);
        return c;
    }

    // ------------------------------- botoes -------------------------------

    public static BotaoModerno botao(String texto, Icones.Tipo icone, BotaoModerno.Variante variante) {
        BotaoModerno b = new BotaoModerno(texto, variante);
        if (icone != null) {
            Color cor = variante == BotaoModerno.Variante.SECUNDARIO ? Tema.TEXTO
                    : variante == BotaoModerno.Variante.FANTASMA ? Tema.ROXO : Tema.BRANCO;
            b.setIcon(Icones.icone(icone, 16, cor));
        }
        return b;
    }

    // ------------------------------- tabelas -------------------------------

    /** Modelo de tabela que nao permite edicao direta das celulas. */
    public static DefaultTableModel modeloSomenteLeitura(String... colunas) {
        return new DefaultTableModel(new Object[0][0], colunas) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    public static JTable tabela(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setFont(Tema.fonte(Font.PLAIN, 13.5f));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setRowHeight(42);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setFont(Tema.fonte(Font.BOLD, 12f));
        javax.swing.table.TableCellRenderer cabecalho = t.getTableHeader().getDefaultRenderer();
        t.getTableHeader().setDefaultRenderer((tabela, valor, selecionada, foco, linha, coluna) -> {
            Component c = cabecalho.getTableCellRendererComponent(tabela, valor, selecionada, foco, linha, coluna);
            if (c instanceof JLabel) {
                ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
            }
            return c;
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                    boolean foco, int linha, int coluna) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        tabela, valor, selecionada, false, linha, coluna);
                l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                return l;
            }
        });
        return t;
    }

    public static JScrollPane rolagem(Component conteudo) {
        JScrollPane sp = new JScrollPane(conteudo);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Tema.BRANCO);
        return sp;
    }

    /** Linha horizontal alinhada a esquerda, com um espaco fixo entre os componentes. */

    /** Painel que empilha componentes na vertical, todos ocupando a largura inteira. */
    public static JPanel pilha() {
        JPanel p = transparente(new java.awt.GridBagLayout());
        p.putClientProperty("pilha.linha", 0);
        return p;
    }

    /** Linha horizontal alinhada a esquerda, com um espaco fixo entre os componentes. */
    public static JPanel linha(int espaco, Component... itens) {
        return linhaHorizontal(false, espaco, itens);
    }

    /** Linha horizontal alinhada a direita (ex.: botoes de um formulario). */
    public static JPanel linhaADireita(int espaco, Component... itens) {
        return linhaHorizontal(true, espaco, itens);
    }

    private static JPanel linhaHorizontal(boolean aDireita, int espaco, Component[] itens) {
        JPanel p = transparente(null);
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.X_AXIS));
        if (aDireita) {
            p.add(javax.swing.Box.createHorizontalGlue());
        }
        for (int i = 0; i < itens.length; i++) {
            if (i > 0) {
                p.add(javax.swing.Box.createHorizontalStrut(espaco));
            }
            p.add(itens[i]);
        }
        if (!aDireita) {
            p.add(javax.swing.Box.createHorizontalGlue());
        }
        return p;
    }

    /** Como {@link #pilha()}, mas nunca mais estreita que {@code larguraMinima} (a altura continua automatica). */
    public static JPanel pilhaComLargura(int larguraMinima) {
        JPanel p = new JPanel(new java.awt.GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.max(d.width, larguraMinima), d.height);
            }
        };
        p.setOpaque(false);
        p.putClientProperty("pilha.linha", 0);
        return p;
    }

    /** Adiciona um componente ao fim da pilha, com um espaco acima dele. */
    public static void empilhar(JPanel pilha, Component componente, int espacoAntes) {
        int linha = (Integer) pilha.getClientProperty("pilha.linha");
        java.awt.GridBagConstraints g = new java.awt.GridBagConstraints();
        g.gridx = 0;
        g.gridy = linha;
        g.weightx = 1;
        g.fill = java.awt.GridBagConstraints.HORIZONTAL;
        g.anchor = java.awt.GridBagConstraints.NORTH;
        g.insets = new java.awt.Insets(espacoAntes, 0, 0, 0);
        pilha.add(componente, g);
        pilha.putClientProperty("pilha.linha", linha + 1);
    }

    /** Empurra o conteudo da pilha para o topo quando sobra espaco. */
    public static void finalizarPilha(JPanel pilha) {
        java.awt.GridBagConstraints g = new java.awt.GridBagConstraints();
        g.gridx = 0;
        g.gridy = (Integer) pilha.getClientProperty("pilha.linha");
        g.weighty = 1;
        g.fill = java.awt.GridBagConstraints.VERTICAL;
        pilha.add(javax.swing.Box.createGlue(), g);
    }

    /** Selo arredondado com um icone dentro (usado nos cards). */
    public static javax.swing.JComponent seloIcone(Icones.Tipo tipo, Color fundo, Color corIcone, int lado) {
        Icon icone = Icones.icone(tipo, (int) (lado * 0.5), corIcone);
        javax.swing.JComponent c = new javax.swing.JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(fundo);
                    g2.fillRoundRect(0, 0, lado - 1, lado - 1, lado / 2, lado / 2);
                    icone.paintIcon(this, g2, (lado - icone.getIconWidth()) / 2, (lado - icone.getIconHeight()) / 2);
                } finally {
                    g2.dispose();
                }
            }
        };
        Dimension d = new Dimension(lado, lado);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
        return c;
    }

    /** Selo arredondado com um texto curto (ex.: sigla do idioma). */
    public static javax.swing.JComponent seloTexto(String texto, Color fundo, Color corTexto, int lado) {
        javax.swing.JComponent c = new javax.swing.JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(fundo);
                    g2.fillRoundRect(0, 0, lado - 1, lado - 1, lado / 2, lado / 2);
                    g2.setColor(corTexto);
                    g2.setFont(Tema.fonte(Font.BOLD, lado * 0.34f));
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(texto, (lado - fm.stringWidth(texto)) / 2,
                            (lado - fm.getHeight()) / 2 + fm.getAscent());
                } finally {
                    g2.dispose();
                }
            }
        };
        Dimension d = new Dimension(lado, lado);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
        return c;
    }
}
