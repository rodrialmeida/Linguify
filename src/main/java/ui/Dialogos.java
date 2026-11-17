package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Mensagens de sucesso, erro, aviso e confirmacao com o visual do Linguify.
 *
 * <p>As telas nunca usam {@code JOptionPane} diretamente. Os testes podem
 * trocar o {@link Provedor} para responder sem abrir janelas.</p>
 */
public final class Dialogos {

    public enum Tipo { SUCESSO, ERRO, AVISO, INFO }

    /** Estrategia de exibicao (substituivel nos testes). */
    public interface Provedor {
        void mensagem(Component pai, Tipo tipo, String texto);

        boolean confirmar(Component pai, String titulo, String texto, String rotuloConfirmar, boolean perigo);
    }

    private static Provedor provedor = new ProvedorSwing();

    private Dialogos() {
    }

    /** Troca o provedor (use {@code null} para voltar ao padrao com janelas). */
    public static void usar(Provedor novo) {
        provedor = novo == null ? new ProvedorSwing() : novo;
    }

    public static void sucesso(Component pai, String texto) {
        provedor.mensagem(pai, Tipo.SUCESSO, texto);
    }

    public static void erro(Component pai, String texto) {
        provedor.mensagem(pai, Tipo.ERRO, texto);
    }

    public static void aviso(Component pai, String texto) {
        provedor.mensagem(pai, Tipo.AVISO, texto);
    }

    public static void info(Component pai, String texto) {
        provedor.mensagem(pai, Tipo.INFO, texto);
    }

    public static boolean confirmar(Component pai, String titulo, String texto, String rotuloConfirmar,
            boolean perigo) {
        return provedor.confirmar(pai, titulo, texto, rotuloConfirmar, perigo);
    }

    // ------------------------------------------------------------------

    private static final class ProvedorSwing implements Provedor {

        @Override
        public void mensagem(Component pai, Tipo tipo, String texto) {
            Color cor;
            Icones.Tipo icone;
            String titulo;
            switch (tipo) {
                case SUCESSO:
                    cor = Tema.SUCESSO;
                    icone = Icones.Tipo.CHECK;
                    titulo = "Tudo certo";
                    break;
                case ERRO:
                    cor = Tema.ERRO;
                    icone = Icones.Tipo.ERRO;
                    titulo = "Algo deu errado";
                    break;
                case AVISO:
                    cor = Tema.AVISO;
                    icone = Icones.Tipo.ALERTA;
                    titulo = "Atenção";
                    break;
                default:
                    cor = Tema.ROXO;
                    icone = Icones.Tipo.INFO;
                    titulo = "Informação";
                    break;
            }
            BotaoModerno ok = new BotaoModerno("Entendi", BotaoModerno.Variante.PRIMARIO);
            JDialog d = montar(pai, titulo, texto, cor, icone, ok);
            ok.addActionListener(e -> d.dispose());
            d.getRootPane().setDefaultButton(ok);
            d.setVisible(true);
        }

        @Override
        public boolean confirmar(Component pai, String titulo, String texto, String rotuloConfirmar,
                boolean perigo) {
            boolean[] resposta = {false};
            BotaoModerno cancelar = new BotaoModerno("Cancelar", BotaoModerno.Variante.SECUNDARIO);
            BotaoModerno confirmar = new BotaoModerno(rotuloConfirmar,
                    perigo ? BotaoModerno.Variante.PERIGO : BotaoModerno.Variante.PRIMARIO);
            JDialog d = montar(pai, titulo, texto, perigo ? Tema.ERRO : Tema.ROXO,
                    perigo ? Icones.Tipo.ALERTA : Icones.Tipo.INFO, cancelar, confirmar);
            cancelar.addActionListener(e -> d.dispose());
            confirmar.addActionListener(e -> {
                resposta[0] = true;
                d.dispose();
            });
            // Em acoes destrutivas o Enter cancela (padrao mais seguro).
            d.getRootPane().setDefaultButton(perigo ? cancelar : confirmar);
            d.setVisible(true);
            return resposta[0];
        }

        private JDialog montar(Component pai, String titulo, String texto, Color cor, Icones.Tipo icone,
                BotaoModerno... botoes) {
            Window dono = pai instanceof Window ? (Window) pai
                    : pai == null ? null : SwingUtilities.getWindowAncestor(pai);
            JDialog d = new JDialog(dono, "Linguify", Dialog.ModalityType.APPLICATION_MODAL);
            d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JPanel raiz = new JPanel(new BorderLayout(18, 0));
            raiz.setBackground(Tema.BRANCO);
            raiz.setBorder(BorderFactory.createEmptyBorder(24, 26, 18, 26));

            raiz.add(new Selo(cor, Icones.icone(icone, 26, Tema.BRANCO)), BorderLayout.WEST);

            JPanel centro = new JPanel();
            centro.setOpaque(false);
            centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
            JLabel t = Ui.secao(titulo);
            t.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel m = new JLabel("<html><body style='width:300px'>" + Ui.html(texto) + "</body></html>");
            m.setFont(Tema.fonte(java.awt.Font.PLAIN, 13.5f));
            m.setForeground(Tema.TEXTO);
            m.setAlignmentX(Component.LEFT_ALIGNMENT);
            centro.add(t);
            centro.add(javax.swing.Box.createVerticalStrut(6));
            centro.add(m);
            raiz.add(centro, BorderLayout.CENTER);

            JPanel sul = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            sul.setOpaque(false);
            sul.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            for (BotaoModerno b : botoes) {
                sul.add(b);
            }
            raiz.add(sul, BorderLayout.SOUTH);

            d.setContentPane(raiz);
            ((JComponent) d.getContentPane()).registerKeyboardAction(e -> d.dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            d.pack();
            d.setResizable(false);
            d.setLocationRelativeTo(pai);
            return d;
        }
    }

    /** Circulo colorido com um icone, usado nas mensagens. */
    private static final class Selo extends JComponent {
        private final Color cor;
        private final Icon icone;

        Selo(Color cor, Icon icone) {
            this.cor = cor;
            this.icone = icone;
            setPreferredSize(new java.awt.Dimension(48, 48));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cor);
                g2.fillOval(0, 0, 47, 47);
                icone.paintIcon(this, g2, (48 - icone.getIconWidth()) / 2, (48 - icone.getIconHeight()) / 2);
            } finally {
                g2.dispose();
            }
        }
    }
}
