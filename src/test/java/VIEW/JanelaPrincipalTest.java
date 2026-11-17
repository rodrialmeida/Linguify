package VIEW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DAO.MateriasDAO;
import DAO.UsuarioDAO;
import DTO.MateriasDTO;
import DTO.UsuarioDTO;
import auth.SessaoUsuario;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.Arvore;
import testutil.DadosDemo;
import testutil.FakeBanco;
import testutil.UiTeste;
import ui.BotaoMenu;
import ui.CardPanel;

class JanelaPrincipalTest extends UiTeste {

    private FakeBanco db;
    private AtomicInteger saidas;
    private JanelaPrincipal janela;

    @BeforeEach
    void preparar() {
        db = DadosDemo.popular(new FakeBanco());
        saidas = new AtomicInteger();
    }

    @AfterEach
    void fechar() {
        if (janela != null) {
            janela.dispose();
        }
    }

    private JanelaPrincipal abrir(int id, String username, String perfil) {
        UsuarioDTO u = new UsuarioDTO();
        u.setId_usuario(id);
        u.setUsername(username);
        u.setPerfil(perfil);
        janela = new JanelaPrincipal(new SessaoUsuario(u), new UsuarioDAO(db::connection),
                new MateriasDAO(db::connection), saidas::incrementAndGet);
        janela.pack();   // torna a janela displayable (sem exibi-la)
        return janela;
    }

    private List<BotaoMenu> itensDoMenu() {
        return Arvore.todos(janela, BotaoMenu.class);
    }

    private CardPanel cartaoCom(String texto) {
        return Arvore.todos(janela, CardPanel.class).stream()
                .filter(c -> Arvore.contemTexto(c, texto) && hasAcao(c))
                .findFirst().orElse(null);
    }

    private static boolean hasAcao(CardPanel c) {
        return c.getCursor().getType() == java.awt.Cursor.HAND_CURSOR;
    }

    // ---------------- permissoes ----------------

    @Test
    void adminVeDashboardAulasEUsuarios() {
        abrir(1, "admin", "ADMIN");

        assertEquals(Set.of("dashboard", "aulas", "usuarios"), janela.secoesDisponiveis());
        List<String> itens = itensDoMenu().stream().map(BotaoMenu::getText).toList();
        assertTrue(itens.contains("Dashboard"));
        assertTrue(itens.contains("Aulas e matérias"));
        assertTrue(itens.contains("Usuários"));
        assertTrue(itens.contains("Sair da conta"));
    }

    @Test
    void estudanteNaoVeNemAcessaUsuarios() {
        abrir(2, "aluno", "ESTUDANTE");

        assertEquals(Set.of("dashboard", "aulas"), janela.secoesDisponiveis());
        assertFalse(itensDoMenu().stream().anyMatch(b -> b.getText().equals("Usuários")),
                "O estudante nao pode ter o item Usuarios no menu.");

        janela.navegar(JanelaPrincipal.SECAO_USUARIOS);

        assertEquals(JanelaPrincipal.SECAO_DASHBOARD, janela.secaoAtual(), "A navegacao nao-permitida e ignorada.");
    }

    @Test
    void menuNaoTemMaisSecoesSeparadasDeMateriasEAulas() {
        abrir(1, "admin", "ADMIN");

        List<String> itens = itensDoMenu().stream().map(BotaoMenu::getText).toList();
        assertFalse(itens.contains("Minhas matérias"));
        assertFalse(itens.contains("Minhas aulas"));
    }

    // ---------------- navegacao ----------------

    @Test
    void abreNoDashboardComOItemSelecionado() {
        abrir(1, "admin", "ADMIN");

        assertEquals("dashboard", janela.secaoAtual());
        assertTrue(itensDoMenu().stream().filter(b -> b.getText().equals("Dashboard")).findFirst()
                .orElseThrow().isSelected());
    }

    @Test
    void cliqueNoMenuTrocaDeSecaoEDestacaOItem() {
        abrir(1, "admin", "ADMIN");

        Arvore.botao(janela, "Aulas e matérias").doClick(0);
        assertEquals("aulas", janela.secaoAtual());
        assertTrue(Arvore.botao(janela, "Aulas e matérias").isSelected());
        assertFalse(Arvore.botao(janela, "Dashboard").isSelected());

        Arvore.botao(janela, "Usuários").doClick(0);
        assertEquals("usuarios", janela.secaoAtual());

        Arvore.botao(janela, "Dashboard").doClick(0);
        assertEquals("dashboard", janela.secaoAtual());
    }

    @Test
    void secaoDeAulasCarregaAsOitoAulas() {
        abrir(2, "aluno", "ESTUDANTE");

        Arvore.botao(janela, "Aulas e matérias").doClick(0);

        assertEquals(8, janela.painelAulas().aulasVisiveis().size());
    }

    // ---------------- dashboard ----------------

    @Test
    void dashboardDoAdminMostraNumerosReaisDoBanco() {
        abrir(1, "admin", "ADMIN");

        List<String> textos = Arvore.textos(janela);
        assertTrue(textos.contains("Olá, Admin!"));
        assertTrue(textos.contains("4"), "4 idiomas");
        assertTrue(textos.contains("8"), "8 aulas");
        assertTrue(textos.contains("3"), "3 usuarios");
        assertTrue(textos.contains("Idiomas"));
        assertTrue(textos.contains("Aulas"));
        assertTrue(textos.contains("Usuários"));
    }

    @Test
    void dashboardDoEstudanteNaoMostraUsuarios() {
        abrir(2, "aluno", "ESTUDANTE");

        List<String> textos = Arvore.textos(janela);
        assertTrue(textos.contains("Olá, Aluno!"));
        assertTrue(textos.contains("8"));
        assertFalse(textos.contains("Gerenciar usuários"));
        assertFalse(textos.contains("Nova aula"));
        assertTrue(textos.contains("Explorar aulas"));
    }

    @Test
    void numerosDoDashboardAcompanhamOBanco() {
        db.addAula("Italiano", "Saluti", "Linguify Demo", "20/11/2026", "Saluti in italiano.");
        abrir(1, "admin", "ADMIN");

        List<String> textos = Arvore.textos(janela);
        assertTrue(textos.contains("5"), "5 idiomas");
        assertTrue(textos.contains("9"), "9 aulas");
        assertTrue(Arvore.temTexto(janela, "Italiano"));
    }

    @Test
    void cartaoDeIdiomaAbreAulasJaFiltradas() {
        abrir(2, "aluno", "ESTUDANTE");

        CardPanel alemao = cartaoCom("Alemão");
        assertNotNull(alemao, "Deve haver um card clicavel para o idioma Alemao.");
        alemao.clicar();

        assertEquals("aulas", janela.secaoAtual());
        List<MateriasDTO> visiveis = janela.painelAulas().aulasVisiveis();
        assertEquals(2, visiveis.size());
        assertTrue(visiveis.stream().allMatch(a -> a.getMateria().equals("Alemão")));
    }

    @Test
    void cartoesDeNumeroAbremAsSecoesCorrespondentes() {
        abrir(1, "admin", "ADMIN");

        cartaoCom("Gerenciar usuários").clicar();
        assertEquals("usuarios", janela.secaoAtual());

        Arvore.botao(janela, "Dashboard").doClick(0);
        cartaoCom("Ver todas as aulas").clicar();
        assertEquals("aulas", janela.secaoAtual());
        assertEquals(8, janela.painelAulas().aulasVisiveis().size(), "Sem filtro: todas as aulas.");
    }

    @Test
    void atalhosDoDashboardFuncionam() {
        abrir(1, "admin", "ADMIN");

        Arvore.botao(janela, "Gerenciar usuários").doClick(0);
        assertEquals("usuarios", janela.secaoAtual());

        Arvore.botao(janela, "Dashboard").doClick(0);
        Arvore.botao(janela, "Ver aulas").doClick(0);
        assertEquals("aulas", janela.secaoAtual());
    }

    @Test
    void dashboardMostraErroEPermiteTentarNovamenteQuandoOBancoFalha() {
        UsuarioDTO u = new UsuarioDTO();
        u.setId_usuario(1);
        u.setUsername("admin");
        u.setPerfil("ADMIN");
        janela = new JanelaPrincipal(new SessaoUsuario(u), new UsuarioDAO(() -> null),
                new MateriasDAO(() -> null), saidas::incrementAndGet);

        assertTrue(Arvore.temTexto(janela, "Não foi possível carregar os dados"));
        assertNotNull(Arvore.botao(janela, "Tentar novamente"));
    }

    // ---------------- sair ----------------

    @Test
    void sairComConfirmacaoFechaAJanelaEVoltaAoLogin() {
        abrir(2, "aluno", "ESTUDANTE");
        dialogos.responder = true;

        janela.botaoSair().doClick(0);

        assertEquals(1, saidas.get());
        assertFalse(janela.isDisplayable());
        assertEquals(1, dialogos.confirmacoes.size());
    }

    @Test
    void sairSemConfirmarMantemAJanela() {
        abrir(2, "aluno", "ESTUDANTE");
        dialogos.responder = false;

        janela.botaoSair().doClick(0);

        assertEquals(0, saidas.get());
        assertTrue(janela.isDisplayable());
    }

    @Test
    void perderPermissaoDeAdminEncerraASessaoAoUsarUsuarios() {
        abrir(1, "admin", "ADMIN");
        db.porId(1).perfil = "ESTUDANTE";    // outro administrador rebaixou esta conta

        janela.navegar(JanelaPrincipal.SECAO_USUARIOS);

        assertEquals(1, saidas.get(), "A sessao e encerrada quando o banco nega o acesso.");
        assertFalse(janela.isDisplayable());
    }
}
