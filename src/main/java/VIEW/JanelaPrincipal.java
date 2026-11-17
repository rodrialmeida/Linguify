package VIEW;

import DAO.MateriasDAO;
import DAO.UsuarioDAO;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ui.Avatar;
import ui.BotaoMenu;
import ui.Dialogos;
import ui.Icones;
import ui.Tema;
import ui.Ui;

/**
 * Janela unica do Linguify depois do login: menu lateral e conteudo interno
 * (Dashboard, Aulas e matérias e, somente para ADMIN, Usuários).
 */
public class JanelaPrincipal extends JFrame {

    public static final String SECAO_DASHBOARD = "dashboard";
    public static final String SECAO_AULAS = "aulas";
    public static final String SECAO_USUARIOS = "usuarios";

    private final SessaoUsuario sessao;
    private final Runnable aoSair;
    private final CardLayout cartoes = new CardLayout();
    private final JPanel conteudo = new JPanel(cartoes);
    private final Map<String, Secao> secoes = new LinkedHashMap<>();
    private final Map<String, BotaoMenu> botoes = new LinkedHashMap<>();
    private final ButtonGroup grupo = new ButtonGroup();

    private final DashboardPanel dashboard;
    private final AulasPanel aulas;
    private final UsuariosPanel usuarios;       // null quando o perfil nao e ADMIN
    private String secaoAtual;

    public JanelaPrincipal(SessaoUsuario sessao, UsuarioDAO usuarioDao, MateriasDAO materiasDao, Runnable aoSair) {
        super("Linguify");
        this.sessao = sessao;
        this.aoSair = aoSair;

        aulas = new AulasPanel(sessao, materiasDao, this::sairPorAcessoNegado);
        UsuariosPanel painelUsuarios = null;
        if (sessao.isAdmin()) {
            try {
                painelUsuarios = new UsuariosPanel(sessao, usuarioDao, this::sairPorAcessoNegado);
            } catch (AcessoNegadoException e) {
                painelUsuarios = null;
            }
        }
        usuarios = painelUsuarios;
        dashboard = new DashboardPanel(sessao, materiasDao, usuarioDao, new DashboardPanel.Navegador() {
            @Override
            public void abrirAulas(String idioma) {
                navegar(SECAO_AULAS);
                aulas.filtrarPorIdioma(idioma);
            }

            @Override
            public void novaAula() {
                navegar(SECAO_AULAS);
                aulas.novaAula();
            }

            @Override
            public void abrirUsuarios() {
                navegar(SECAO_USUARIOS);
            }
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Icones.iconeApp(64));
        montar();
        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        navegar(SECAO_DASHBOARD);
    }

    // ------------------------------------------------------------------

    private void montar() {
        conteudo.setBackground(Tema.FUNDO);
        conteudo.setBorder(Ui.margem(28, 32, 24, 32));
        registrar(SECAO_DASHBOARD, dashboard, dashboard);
        registrar(SECAO_AULAS, aulas, aulas);
        if (usuarios != null) {
            registrar(SECAO_USUARIOS, usuarios, usuarios);
        }

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(criarMenuLateral(), BorderLayout.WEST);
        raiz.add(conteudo, BorderLayout.CENTER);
        setContentPane(raiz);
    }

    private void registrar(String nome, JPanel painel, Secao secao) {
        secoes.put(nome, secao);
        conteudo.add(painel, nome);
    }

    private JPanel criarMenuLateral() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Tema.NAVY);
        menu.setBorder(Ui.margem(28, 18, 22, 18));
        menu.setPreferredSize(new Dimension(264, 100));

        JLabel logo = LoginView.logo(190);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        menu.add(logo);
        menu.add(Box.createVerticalStrut(30));

        JLabel rotuloMenu = Ui.rotulo("MENU", java.awt.Font.BOLD, 11f, Tema.ROXO_CLARO);
        rotuloMenu.setAlignmentX(LEFT_ALIGNMENT);
        rotuloMenu.setBorder(Ui.margem(0, 8, 0, 0));
        menu.add(rotuloMenu);
        menu.add(Box.createVerticalStrut(10));

        adicionarItem(menu, SECAO_DASHBOARD, "Dashboard", Icones.Tipo.DASHBOARD);
        adicionarItem(menu, SECAO_AULAS, "Aulas e matérias", Icones.Tipo.LIVRO);
        if (usuarios != null) {
            adicionarItem(menu, SECAO_USUARIOS, "Usuários", Icones.Tipo.USUARIOS);
        }

        menu.add(Box.createVerticalGlue());
        menu.add(criarCartaoUsuario());
        menu.add(Box.createVerticalStrut(12));

        BotaoMenu sair = new BotaoMenu("Sair da conta", Icones.Tipo.SAIR);
        sair.setIcon(Icones.icone(Icones.Tipo.SAIR, 20, new java.awt.Color(0xFFB4B9)));
        sair.addActionListener(e -> {
            sair.setSelected(false);
            sair();
        });
        botaoSair = sair;
        menu.add(sair);
        return menu;
    }

    private BotaoMenu botaoSair;

    private void adicionarItem(JPanel menu, String secao, String texto, Icones.Tipo icone) {
        BotaoMenu b = new BotaoMenu(texto, icone);
        b.addActionListener(e -> navegar(secao));
        grupo.add(b);
        botoes.put(secao, b);
        menu.add(b);
        menu.add(Box.createVerticalStrut(6));
    }

    private JPanel criarCartaoUsuario() {
        JPanel cartao = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Tema.NAVY_CLARO);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                } finally {
                    g2.dispose();
                }
            }
        };
        cartao.setOpaque(false);
        cartao.setBorder(Ui.margem(12, 12, 12, 12));
        cartao.setAlignmentX(LEFT_ALIGNMENT);
        cartao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        cartao.add(new Avatar(sessao.getUsername(), 44, Tema.ROXO), BorderLayout.WEST);
        JPanel texto = Ui.pilha();
        Ui.empilhar(texto, Ui.rotulo(sessao.getUsername(), java.awt.Font.BOLD, 14f, Tema.BRANCO), 0);
        Ui.empilhar(texto, Ui.rotulo(sessao.isAdmin() ? "Administrador" : "Estudante",
                java.awt.Font.PLAIN, 12f, Tema.ROXO_CLARO), 2);
        Ui.finalizarPilha(texto);
        cartao.add(texto, BorderLayout.CENTER);
        return cartao;
    }

    // ------------------------------------------------------------------
    // Navegacao e saida
    // ------------------------------------------------------------------

    /** Exibe uma secao e recarrega seus dados. Secoes nao permitidas ao perfil sao ignoradas. */
    public void navegar(String secao) {
        Secao destino = secoes.get(secao);
        if (destino == null) {
            return;
        }
        secaoAtual = secao;
        cartoes.show(conteudo, secao);
        botoes.get(secao).setSelected(true);
        destino.aoExibir();
    }

    /** Secao atualmente exibida. */
    public String secaoAtual() {
        return secaoAtual;
    }

    /** Secoes disponiveis para o perfil atual. */
    public java.util.Set<String> secoesDisponiveis() {
        return java.util.Collections.unmodifiableSet(secoes.keySet());
    }

    AulasPanel painelAulas() {
        return aulas;
    }

    BotaoMenu botaoSair() {
        return botaoSair;
    }

    /** Pede confirmacao e volta ao login. */
    public void sair() {
        if (Dialogos.confirmar(this, "Sair da conta", "Deseja sair do Linguify?", "Sair", false)) {
            encerrarSessao();
        }
    }

    private void sairPorAcessoNegado() {
        encerrarSessao();
    }

    private void encerrarSessao() {
        dispose();
        aoSair.run();
    }
}
