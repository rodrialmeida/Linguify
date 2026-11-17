package VIEW;

import DAO.MateriasDAO;
import DAO.UsuarioDAO;
import DTO.MateriasDTO;
import auth.OperacaoUsuarioException;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import ui.BotaoModerno;
import ui.CardPanel;
import ui.Icones;
import ui.PaginaRolavel;
import ui.Tarefa;
import ui.Tema;
import ui.Ui;

/**
 * Painel inicial: saudacao, numeros reais do banco (idiomas, aulas e, para ADMIN,
 * usuarios), atalhos e os idiomas disponiveis. Todos os cards abrem uma secao.
 */
public class DashboardPanel extends JPanel implements Secao {

    /** Acoes de navegacao oferecidas pela janela principal. */
    public interface Navegador {
        /** Abre "Aulas e matérias", opcionalmente ja filtrada por idioma ({@code null} = todos). */
        void abrirAulas(String idioma);

        /** Abre o formulario de nova aula (somente ADMIN). */
        void novaAula();

        /** Abre a secao de usuarios (somente ADMIN). */
        void abrirUsuarios();
    }

    private static final Map<String, String> SIGLAS = Map.of(
            "inglês", "EN", "espanhol", "ES", "francês", "FR", "alemão", "DE",
            "italiano", "IT", "português", "PT", "japonês", "JA");

    private final SessaoUsuario sessao;
    private final MateriasDAO materiasDao;
    private final UsuarioDAO usuarioDao;
    private final Navegador navegador;
    private final PaginaRolavel pagina = new PaginaRolavel(new BorderLayout());

    public DashboardPanel(SessaoUsuario sessao, MateriasDAO materiasDao, UsuarioDAO usuarioDao,
            Navegador navegador) {
        super(new BorderLayout());
        this.sessao = sessao;
        this.materiasDao = materiasDao;
        this.usuarioDao = usuarioDao;
        this.navegador = navegador;
        setOpaque(false);

        JScrollPane rolagem = new JScrollPane(pagina);
        rolagem.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        rolagem.setOpaque(false);
        rolagem.getViewport().setOpaque(false);
        add(rolagem, BorderLayout.CENTER);
        mostrarMensagem("Carregando...");
    }

    @Override
    public void aoExibir() {
        Tarefa.executar(() -> {
            List<MateriasDTO> aulas = materiasDao.listarAulas(sessao);
            int usuarios = sessao.isAdmin() ? usuarioDao.listarUsuarios(sessao).size() : -1;
            return new Dados(aulas, usuarios);
        }, this::montar, erro -> {
            String msg = erro instanceof OperacaoUsuarioException || erro instanceof AcessoNegadoException
                    ? erro.getMessage() : "Não foi possível carregar o painel.";
            mostrarErro(msg);
        });
    }

    private record Dados(List<MateriasDTO> aulas, int usuarios) {
    }

    // ------------------------------------------------------------------

    private void mostrarMensagem(String texto) {
        pagina.removeAll();
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(Ui.subtitulo(texto), new GridBagConstraints());
        pagina.add(p, BorderLayout.CENTER);
        pagina.revalidate();
        pagina.repaint();
    }

    private void mostrarErro(String mensagem) {
        pagina.removeAll();
        JPanel p = Ui.pilha();
        Ui.empilhar(p, Ui.titulo("Painel"), 0);
        CardPanel cartao = new CardPanel(new BorderLayout(0, 14), 20, 24);
        JPanel conteudo = Ui.pilha();
        Ui.empilhar(conteudo, Ui.secao("Não foi possível carregar os dados"), 0);
        Ui.empilhar(conteudo, Ui.texto("<html><body style='width:420px'>" + Ui.html(mensagem) + "</body></html>"), 6);
        BotaoModerno tentar = Ui.botao("Tentar novamente", null, BotaoModerno.Variante.PRIMARIO);
        tentar.addActionListener(e -> aoExibir());
        JPanel linha = Ui.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linha.add(tentar);
        Ui.empilhar(conteudo, linha, 16);
        cartao.add(conteudo, BorderLayout.CENTER);
        Ui.empilhar(p, cartao, 18);
        Ui.finalizarPilha(p);
        pagina.add(p, BorderLayout.CENTER);
        pagina.revalidate();
        pagina.repaint();
    }

    private void montar(Dados dados) {
        // Idioma -> quantidade de aulas, na ordem em que aparecem.
        Map<String, Integer> porIdioma = new LinkedHashMap<>();
        for (MateriasDTO a : dados.aulas()) {
            if (!a.getMateria().isBlank()) {
                porIdioma.merge(a.getMateria(), 1, Integer::sum);
            }
        }
        boolean admin = sessao.isAdmin();

        JPanel p = Ui.pilha();
        Ui.empilhar(p, cabecalho(), 0);
        Ui.empilhar(p, estatisticas(porIdioma.size(), dados.aulas().size(), dados.usuarios()), 24);
        Ui.empilhar(p, Ui.secao("Idiomas disponíveis"), 30);
        Ui.empilhar(p, idiomas(porIdioma, admin), 14);
        Ui.empilhar(p, Ui.secao("Atalhos"), 30);
        Ui.empilhar(p, atalhos(admin), 14);
        Ui.finalizarPilha(p);

        pagina.removeAll();
        pagina.add(p, BorderLayout.CENTER);
        pagina.revalidate();
        pagina.repaint();
    }

    private JPanel cabecalho() {
        String nome = sessao.getUsername();
        String bonito = nome == null || nome.isBlank() ? "" : nome.substring(0, 1).toUpperCase() + nome.substring(1);
        JPanel p = Ui.pilha();
        Ui.empilhar(p, Ui.titulo("Olá, " + bonito + "!"), 0);
        Ui.empilhar(p, Ui.subtitulo(sessao.isAdmin()
                ? "Painel administrativo: acompanhe o conteúdo e gerencie os acessos."
                : "Escolha um idioma e continue seus estudos."), 4);
        return p;
    }

    private JPanel estatisticas(int idiomas, int aulas, int usuarios) {
        boolean admin = sessao.isAdmin();
        JPanel linha = new JPanel(new GridLayout(1, admin ? 3 : 2, 18, 0));
        linha.setOpaque(false);
        linha.add(cartaoNumero(Icones.Tipo.LIVRO, String.valueOf(idiomas),
                idiomas == 1 ? "Idioma" : "Idiomas", "Ver aulas", () -> navegador.abrirAulas(null)));
        linha.add(cartaoNumero(Icones.Tipo.DASHBOARD, String.valueOf(aulas),
                aulas == 1 ? "Aula" : "Aulas", "Ver todas as aulas", () -> navegador.abrirAulas(null)));
        if (admin) {
            linha.add(cartaoNumero(Icones.Tipo.USUARIOS, String.valueOf(usuarios),
                    usuarios == 1 ? "Usuário" : "Usuários", "Gerenciar usuários", navegador::abrirUsuarios));
        }
        return linha;
    }

    private CardPanel cartaoNumero(Icones.Tipo icone, String valor, String rotulo, String acao, Runnable aoClicar) {
        CardPanel c = new CardPanel(new BorderLayout(16, 0), 20, 22);
        c.add(Ui.seloIcone(icone, Tema.ROXO_SUAVE, Tema.ROXO, 56), BorderLayout.WEST);
        JPanel texto = Ui.pilha();
        Ui.empilhar(texto, Ui.rotulo(valor, java.awt.Font.BOLD, 30f, Tema.NAVY), 0);
        Ui.empilhar(texto, Ui.rotulo(rotulo, java.awt.Font.PLAIN, 13.5f, Tema.CINZA), 0);
        Ui.empilhar(texto, Ui.rotulo(acao + "  \u2192", java.awt.Font.BOLD, 12f, Tema.ROXO), 8);
        Ui.finalizarPilha(texto);
        c.add(texto, BorderLayout.CENTER);
        c.aoClicar(aoClicar);
        return c;
    }

    private JPanel idiomas(Map<String, Integer> porIdioma, boolean admin) {
        if (porIdioma.isEmpty()) {
            CardPanel vazio = new CardPanel(new BorderLayout(0, 12), 20, 24);
            JPanel p = Ui.pilha();
            Ui.empilhar(p, Ui.texto("Ainda não há aulas cadastradas."), 0);
            if (admin) {
                BotaoModerno nova = Ui.botao("Cadastrar a primeira aula", Icones.Tipo.MAIS,
                        BotaoModerno.Variante.PRIMARIO);
                nova.addActionListener(e -> navegador.novaAula());
                JPanel linha = Ui.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
                linha.add(nova);
                Ui.empilhar(p, linha, 12);
            }
            vazio.add(p, BorderLayout.CENTER);
            JPanel envoltorio = new JPanel(new BorderLayout());
            envoltorio.setOpaque(false);
            envoltorio.add(vazio, BorderLayout.CENTER);
            return envoltorio;
        }
        JPanel grade = new JPanel(new GridLayout(0, 4, 18, 18));
        grade.setOpaque(false);
        for (Map.Entry<String, Integer> e : porIdioma.entrySet()) {
            grade.add(cartaoIdioma(e.getKey(), e.getValue()));
        }
        // Completa a ultima linha para que todos os cards tenham a mesma largura.
        int resto = porIdioma.size() % 4;
        if (resto != 0) {
            for (int i = resto; i < 4; i++) {
                JPanel vazio = new JPanel();
                vazio.setOpaque(false);
                grade.add(vazio);
            }
        }
        return grade;
    }

    private CardPanel cartaoIdioma(String idioma, int quantidade) {
        CardPanel c = new CardPanel(new BorderLayout(14, 0), 20, 18);
        c.add(Ui.seloTexto(sigla(idioma), Tema.ROXO_SUAVE, Tema.ROXO_ESCURO, 48), BorderLayout.WEST);
        JPanel texto = Ui.pilha();
        Ui.empilhar(texto, Ui.rotulo(idioma, java.awt.Font.BOLD, 16f, Tema.NAVY), 0);
        Ui.empilhar(texto, Ui.rotulo(quantidade == 1 ? "1 aula" : quantidade + " aulas",
                java.awt.Font.PLAIN, 13f, Tema.CINZA), 2);
        Ui.finalizarPilha(texto);
        c.add(texto, BorderLayout.CENTER);
        JLabel seta = new JLabel(Icones.icone(Icones.Tipo.SETA, 18, Tema.ROXO));
        c.add(seta, BorderLayout.EAST);
        c.aoClicar(() -> navegador.abrirAulas(idioma));
        return c;
    }

    private JPanel atalhos(boolean admin) {
        if (admin) {
            BotaoModerno nova = Ui.botao("Nova aula", Icones.Tipo.MAIS, BotaoModerno.Variante.PRIMARIO);
            nova.addActionListener(e -> navegador.novaAula());
            BotaoModerno aulas = Ui.botao("Ver aulas", Icones.Tipo.LIVRO, BotaoModerno.Variante.SECUNDARIO);
            aulas.addActionListener(e -> navegador.abrirAulas(null));
            BotaoModerno usuarios = Ui.botao("Gerenciar usuários", Icones.Tipo.USUARIOS,
                    BotaoModerno.Variante.SECUNDARIO);
            usuarios.addActionListener(e -> navegador.abrirUsuarios());
            return Ui.linha(12, nova, aulas, usuarios);
        } else {
            BotaoModerno explorar = Ui.botao("Explorar aulas", Icones.Tipo.LIVRO, BotaoModerno.Variante.PRIMARIO);
            explorar.addActionListener(e -> navegador.abrirAulas(null));
            return Ui.linha(12, explorar);
        }
    }

    private static String sigla(String idioma) {
        String s = SIGLAS.get(idioma.toLowerCase(java.util.Locale.ROOT));
        if (s != null) {
            return s;
        }
        String limpo = idioma.trim();
        return limpo.substring(0, Math.min(2, limpo.length())).toUpperCase();
    }
}
