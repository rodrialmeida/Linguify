package VIEW;

import DAO.UsuarioDAO;
import DTO.UsuarioDTO;
import auth.OperacaoUsuarioException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import ui.BotaoModerno;
import ui.CardPanel;
import ui.Icones;
import ui.Tarefa;
import ui.Tema;
import ui.Ui;

/**
 * Tela de entrada do Linguify: login e cadastro de estudantes na mesma janela.
 * Depois da autenticacao entrega o usuario ao {@code aoEntrar} e se fecha.
 */
public class LoginView extends JFrame {

    private static final Logger LOG = Logger.getLogger(LoginView.class.getName());
    private static final String CARTAO_LOGIN = "login";
    private static final String CARTAO_CADASTRO = "cadastro";

    private final UsuarioDAO dao;
    private final Consumer<UsuarioDTO> aoEntrar;
    private final CardLayout cartoes = new ui.CartoesAdaptaveis();
    private final JPanel formularios = new JPanel(cartoes);

    // ----- login (visiveis ao pacote para os testes) -----
    final JTextField txtUsuario = Ui.campo("Digite seu usuário", Icones.Tipo.USUARIO);
    final JPasswordField txtSenha = Ui.campoSenha("Digite sua senha");
    final JLabel lblErroLogin = criarRotuloErro();
    final BotaoModerno btnEntrar = Ui.botao("Entrar", null, BotaoModerno.Variante.PRIMARIO);
    final BotaoModerno btnIrCadastro = Ui.botao("Criar conta", null, BotaoModerno.Variante.FANTASMA);
    private final JProgressBar progressoLogin = criarProgresso();

    // ----- cadastro -----
    final JTextField cadUsuario = Ui.campo("Escolha um nome de usuário", Icones.Tipo.USUARIO);
    final JPasswordField cadSenha = Ui.campoSenha("Mínimo de 6 caracteres");
    final JPasswordField cadConfirmacao = Ui.campoSenha("Repita a senha");
    final JLabel lblErroCadastro = criarRotuloErro();
    final BotaoModerno btnCriar = Ui.botao("Criar conta", null, BotaoModerno.Variante.PRIMARIO);
    final BotaoModerno btnVoltar = Ui.botao("Voltar ao login", Icones.Tipo.VOLTAR, BotaoModerno.Variante.FANTASMA);
    private final JProgressBar progressoCadastro = criarProgresso();

    private String ultimoErroLogin = "";
    private String ultimoErroCadastro = "";

    public LoginView(UsuarioDAO dao, Consumer<UsuarioDTO> aoEntrar) {
        super("Linguify");
        this.dao = dao;
        this.aoEntrar = aoEntrar;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(Icones.iconeApp(64));
        montar();
        configurarAcoes();
        setSize(1000, 660);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        mostrar(CARTAO_LOGIN);
    }

    // ------------------------------------------------------------------
    // Montagem da interface
    // ------------------------------------------------------------------

    private void montar() {
        JPanel raiz = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.46;
        raiz.add(criarPainelMarca(), c);
        c.gridx = 1;
        c.weightx = 0.54;
        raiz.add(criarPainelFormularios(), c);
        setContentPane(raiz);
    }

    private JPanel criarPainelMarca() {
        JPanel marca = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0, 0, Tema.NAVY, getWidth(), getHeight(), new Color(0x3B2F9F)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(new Color(0x7565E8 & 0xFFFFFF | (40 << 24), true));
                    g2.fillOval(getWidth() - 170, -90, 300, 300);
                    g2.setColor(new Color(0xA99BFF & 0xFFFFFF | (30 << 24), true));
                    g2.fillOval(-120, getHeight() - 200, 320, 320);
                } finally {
                    g2.dispose();
                }
            }
        };
        marca.setOpaque(false);
        marca.setPreferredSize(new Dimension(400, 100));

        JPanel coluna = Ui.pilha();
        Ui.empilhar(coluna, logo(300), 0);

        JLabel titulo = new JLabel("<html>Aprenda idiomas<br>no seu ritmo.</html>");
        titulo.setFont(Tema.fonte(java.awt.Font.BOLD, 34f));
        titulo.setForeground(Tema.BRANCO);
        Ui.empilhar(coluna, titulo, 44);

        JLabel descricao = new JLabel("<html><body style='width:300px'>Aulas de Inglês, Espanhol, Francês e "
                + "Alemão reunidas em um só lugar, com conteúdos organizados por idioma.</body></html>");
        descricao.setFont(Tema.fonte(java.awt.Font.PLAIN, 14f));
        descricao.setForeground(new Color(0xCFD1F0));
        Ui.empilhar(coluna, descricao, 14);

        Color vidro = new Color(255, 255, 255, 38);
        Ui.empilhar(coluna, Ui.linha(8,
                Ui.chip("Inglês", vidro, Tema.BRANCO), Ui.chip("Espanhol", vidro, Tema.BRANCO),
                Ui.chip("Francês", vidro, Tema.BRANCO), Ui.chip("Alemão", vidro, Tema.BRANCO)), 26);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new java.awt.Insets(40, 50, 40, 40);
        c.anchor = GridBagConstraints.CENTER;
        marca.add(coluna, c);
        return marca;
    }

    private JPanel criarPainelFormularios() {
        formularios.setOpaque(false);
        formularios.add(painelLogin(), CARTAO_LOGIN);
        formularios.add(painelCadastro(), CARTAO_CADASTRO);

        CardPanel cartao = new CardPanel(new BorderLayout(), 24, 34);
        cartao.add(formularios, BorderLayout.CENTER);

        JPanel direita = new JPanel(new GridBagLayout());
        direita.setBackground(Tema.FUNDO);
        direita.setPreferredSize(new Dimension(400, 100));
        direita.add(cartao, new GridBagConstraints());
        return direita;
    }

    private JPanel painelLogin() {
        JPanel p = Ui.pilhaComLargura(360);
        Ui.empilhar(p, Ui.titulo("Bem-vindo de volta"), 0);
        Ui.empilhar(p, Ui.subtitulo("Entre para continuar seus estudos."), 4);
        Ui.empilhar(p, Ui.rotuloCampo("Usuário"), 26);
        Ui.empilhar(p, txtUsuario, 6);
        Ui.empilhar(p, Ui.rotuloCampo("Senha"), 16);
        Ui.empilhar(p, txtSenha, 6);
        Ui.empilhar(p, lblErroLogin, 10);
        Ui.empilhar(p, btnEntrar, 6);
        Ui.empilhar(p, progressoLogin, 8);

        JPanel rodape = Ui.transparente(new FlowLayout(FlowLayout.CENTER, 4, 0));
        JLabel semConta = Ui.rotulo("Ainda não tem conta?", java.awt.Font.PLAIN, 13f, Tema.CINZA);
        semConta.setBorder(Ui.margem(0, 0, 0, 6));
        rodape.add(semConta);
        rodape.add(btnIrCadastro);
        Ui.empilhar(p, rodape, 12);
        Ui.finalizarPilha(p);
        return p;
    }

    private JPanel painelCadastro() {
        JPanel p = Ui.pilhaComLargura(360);
        Ui.empilhar(p, Ui.titulo("Criar conta"), 0);
        Ui.empilhar(p, Ui.subtitulo("Cadastre-se como estudante para ver as aulas."), 4);
        Ui.empilhar(p, Ui.rotuloCampo("Usuário"), 22);
        Ui.empilhar(p, cadUsuario, 6);
        Ui.empilhar(p, Ui.rotuloCampo("Senha"), 14);
        Ui.empilhar(p, cadSenha, 6);
        Ui.empilhar(p, Ui.rotuloCampo("Confirmar senha"), 14);
        Ui.empilhar(p, cadConfirmacao, 6);
        Ui.empilhar(p, lblErroCadastro, 10);
        Ui.empilhar(p, btnCriar, 6);
        Ui.empilhar(p, progressoCadastro, 8);

        JPanel rodape = Ui.transparente(new FlowLayout(FlowLayout.CENTER, 4, 0));
        rodape.add(btnVoltar);
        Ui.empilhar(p, rodape, 12);
        Ui.finalizarPilha(p);
        return p;
    }

    private static JLabel criarRotuloErro() {
        JLabel l = Ui.rotulo(" ", java.awt.Font.PLAIN, 12.5f, Tema.ERRO);
        l.setPreferredSize(new Dimension(300, 34));
        l.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        return l;
    }

    private static JProgressBar criarProgresso() {
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        pb.setVisible(false);
        pb.setPreferredSize(new Dimension(300, 4));
        return pb;
    }

    /** Logotipo branco do Linguify, redimensionado; usa texto se a imagem nao existir. */
    static JLabel logo(int largura) {
        java.net.URL url = LoginView.class.getResource("/img/LOGO_W (3).png");
        if (url != null) {
            ImageIcon original = new ImageIcon(url);
            int altura = Math.max(1, original.getIconHeight() * largura / Math.max(1, original.getIconWidth()));
            Image redimensionada = original.getImage().getScaledInstance(largura, altura, Image.SCALE_SMOOTH);
            JLabel l = new JLabel(new ImageIcon(redimensionada));
            l.setHorizontalAlignment(JLabel.LEFT);
            return l;
        }
        return Ui.rotulo("Linguify", java.awt.Font.BOLD, 30f, Tema.BRANCO);
    }

    // ------------------------------------------------------------------
    // Comportamento
    // ------------------------------------------------------------------

    private void configurarAcoes() {
        btnEntrar.addActionListener(e -> entrar());
        txtUsuario.addActionListener(e -> txtSenha.requestFocusInWindow());
        txtSenha.addActionListener(e -> entrar());
        btnIrCadastro.addActionListener(e -> mostrar(CARTAO_CADASTRO));

        btnCriar.addActionListener(e -> criarConta());
        cadUsuario.addActionListener(e -> cadSenha.requestFocusInWindow());
        cadSenha.addActionListener(e -> cadConfirmacao.requestFocusInWindow());
        cadConfirmacao.addActionListener(e -> criarConta());
        btnVoltar.addActionListener(e -> mostrar(CARTAO_LOGIN));
    }

    private void mostrar(String cartao) {
        cartoes.show(formularios, cartao);
        boolean login = CARTAO_LOGIN.equals(cartao);
        getRootPane().setDefaultButton(login ? btnEntrar : btnCriar);
        definirErroLogin("");
        definirErroCadastro("");
        (login ? txtUsuario : cadUsuario).requestFocusInWindow();
    }

    /** Mostra o formulario de cadastro (usado pelo link "Criar conta" e pelos testes). */
    void mostrarCadastro() {
        mostrar(CARTAO_CADASTRO);
    }

    void entrar() {
        String usuario = txtUsuario.getText().trim();
        char[] senhaChars = txtSenha.getPassword();
        String senha = new String(senhaChars);
        Arrays.fill(senhaChars, '\0');

        if (usuario.isEmpty() || senha.isEmpty()) {
            definirErroLogin("Informe o usuário e a senha.");
            return;
        }
        definirErroLogin("");
        carregandoLogin(true);

        Tarefa.executar(() -> dao.autenticar(usuario, senha), autenticado -> {
            carregandoLogin(false);
            if (autenticado == null) {
                definirErroLogin("Usuário ou senha inválidos.");
                txtSenha.setText("");
                txtSenha.requestFocusInWindow();
                return;
            }
            try {
                aoEntrar.accept(autenticado);
                dispose();
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Falha ao abrir a janela principal", e);
                definirErroLogin("Não foi possível abrir o painel. Tente novamente.");
            }
        }, erro -> {
            carregandoLogin(false);
            definirErroLogin(mensagemAmigavel(erro));
        });
    }

    void criarConta() {
        String usuario = cadUsuario.getText().trim();
        char[] s1 = cadSenha.getPassword();
        char[] s2 = cadConfirmacao.getPassword();
        boolean iguais = Arrays.equals(s1, s2);
        String senha = new String(s1);
        Arrays.fill(s1, '\0');
        Arrays.fill(s2, '\0');

        if (usuario.isEmpty()) {
            definirErroCadastro("Informe um nome de usuário.");
            return;
        }
        if (senha.isEmpty()) {
            definirErroCadastro("Informe uma senha.");
            return;
        }
        if (!iguais) {
            definirErroCadastro("As senhas não coincidem.");
            return;
        }
        definirErroCadastro("");
        carregandoCadastro(true);

        UsuarioDTO novo = new UsuarioDTO();
        novo.setUsername(usuario);
        novo.setSenha(senha);   // o DAO grava somente o hash BCrypt; perfil sempre ESTUDANTE

        Tarefa.executar(() -> {
            dao.cadastrarEstudante(novo);
            return Boolean.TRUE;
        }, ok -> {
            carregandoCadastro(false);
            cadUsuario.setText("");
            cadSenha.setText("");
            cadConfirmacao.setText("");
            txtUsuario.setText(usuario);
            txtSenha.setText("");
            mostrar(CARTAO_LOGIN);
            txtSenha.requestFocusInWindow();
            ui.Dialogos.sucesso(this, "Conta criada com sucesso! Entre com o usuário \"" + usuario + "\".");
        }, erro -> {
            carregandoCadastro(false);
            definirErroCadastro(mensagemAmigavel(erro));
        });
    }

    private static String mensagemAmigavel(Exception erro) {
        if (erro instanceof OperacaoUsuarioException) {
            return erro.getMessage();
        }
        LOG.log(Level.SEVERE, "Erro inesperado", erro);
        return "Ocorreu um erro inesperado. Tente novamente.";
    }

    private void carregandoLogin(boolean carregando) {
        btnEntrar.setEnabled(!carregando);
        btnEntrar.setText(carregando ? "Entrando..." : "Entrar");
        progressoLogin.setVisible(carregando);
        txtUsuario.setEnabled(!carregando);
        txtSenha.setEnabled(!carregando);
    }

    private void carregandoCadastro(boolean carregando) {
        btnCriar.setEnabled(!carregando);
        btnCriar.setText(carregando ? "Criando..." : "Criar conta");
        progressoCadastro.setVisible(carregando);
    }

    private void definirErroLogin(String mensagem) {
        ultimoErroLogin = mensagem;
        lblErroLogin.setText(mensagem.isEmpty() ? " "
                : "<html><body style='width:300px'>" + Ui.html(mensagem) + "</body></html>");
    }

    private void definirErroCadastro(String mensagem) {
        ultimoErroCadastro = mensagem;
        lblErroCadastro.setText(mensagem.isEmpty() ? " "
                : "<html><body style='width:300px'>" + Ui.html(mensagem) + "</body></html>");
    }

    /** Ultima mensagem de erro exibida no login (vazia se nao houver). */
    String erroLogin() {
        return ultimoErroLogin;
    }

    /** Ultima mensagem de erro exibida no cadastro (vazia se nao houver). */
    String erroCadastro() {
        return ultimoErroCadastro;
    }
}
