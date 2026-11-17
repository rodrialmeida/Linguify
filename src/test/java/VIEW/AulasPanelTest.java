package VIEW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DAO.MateriasDAO;
import DTO.MateriasDTO;
import DTO.UsuarioDTO;
import auth.SessaoUsuario;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.DadosDemo;
import testutil.FakeBanco;
import testutil.UiTeste;
import ui.Dialogos;

class AulasPanelTest extends UiTeste {

    private FakeBanco db;
    private AtomicInteger acessosNegados;
    private SessaoUsuario sessaoAdmin;
    private SessaoUsuario sessaoAluno;

    @BeforeEach
    void preparar() {
        db = DadosDemo.popular(new FakeBanco());
        acessosNegados = new AtomicInteger();
        sessaoAdmin = sessao(1, "admin", "ADMIN");
        sessaoAluno = sessao(2, "aluno", "ESTUDANTE");
    }

    private AulasPanel painel(SessaoUsuario sessao) {
        AulasPanel p = new AulasPanel(sessao, new MateriasDAO(db::connection), acessosNegados::incrementAndGet);
        p.aoExibir();
        return p;
    }

    private static List<String> temas(AulasPanel p) {
        return p.aulasVisiveis().stream().map(MateriasDTO::getTema).toList();
    }

    // ---------------- listagem, busca e filtro ----------------

    @Test
    void listaAsOitoAulasAgrupadasPorIdioma() {
        AulasPanel p = painel(sessaoAluno);

        assertEquals(8, p.aulasVisiveis().size());
        assertEquals(8, p.modelo.getRowCount());
        assertEquals(4, p.modelo.getColumnCount());
        assertEquals("Idioma", p.modelo.getColumnName(0));
        assertEquals("Alemão", p.modelo.getValueAt(0, 0));
    }

    @Test
    void comboDeIdiomaTemTodosEOsQuatroIdiomasDoBanco() {
        AulasPanel p = painel(sessaoAluno);

        List<String> itens = new ArrayList<>();
        for (int i = 0; i < p.cmbIdioma.getItemCount(); i++) {
            itens.add(p.cmbIdioma.getItemAt(i));
        }
        assertEquals(List.of("Todos os idiomas", "Alemão", "Espanhol", "Francês", "Inglês"), itens);
    }

    @Test
    void filtraPorIdioma() {
        AulasPanel p = painel(sessaoAluno);

        p.cmbIdioma.setSelectedItem("Francês");

        assertEquals(List.of("Salutations", "Verbes essentiels"), temas(p));

        p.cmbIdioma.setSelectedItem("Todos os idiomas");
        assertEquals(8, p.aulasVisiveis().size());
    }

    @Test
    void buscaPorNomeDaAulaIgnorandoCaixa() {
        AulasPanel p = painel(sessaoAluno);

        p.txtBusca.setText("GREETINGS");

        assertEquals(List.of("Greetings"), temas(p));
    }

    @Test
    void buscaPorIdiomaIgnorandoAcentos() {
        AulasPanel p = painel(sessaoAluno);

        p.txtBusca.setText("ingles");
        assertEquals(2, p.aulasVisiveis().size());
        assertTrue(p.aulasVisiveis().stream().allMatch(a -> a.getMateria().equals("Inglês")));

        p.txtBusca.setText("frances");
        assertEquals(2, p.aulasVisiveis().size());
    }

    @Test
    void buscaEFiltroFuncionamJuntos() {
        AulasPanel p = painel(sessaoAluno);

        p.cmbIdioma.setSelectedItem("Espanhol");
        p.txtBusca.setText("verbos");

        assertEquals(List.of("Verbos básicos"), temas(p));
    }

    @Test
    void semResultadosMostraTabelaVaziaSemErro() {
        AulasPanel p = painel(sessaoAluno);

        p.txtBusca.setText("zzzz");

        assertTrue(p.aulasVisiveis().isEmpty());
        assertEquals(0, p.modelo.getRowCount());
        assertTrue(dialogos.mensagens.isEmpty());
    }

    @Test
    void limparABuscaVoltaATodasAsAulas() {
        AulasPanel p = painel(sessaoAluno);
        p.txtBusca.setText("zzzz");

        p.txtBusca.setText("");

        assertEquals(8, p.aulasVisiveis().size());
    }

    @Test
    void filtrarPorIdiomaDeFora() {
        AulasPanel p = painel(sessaoAluno);

        p.filtrarPorIdioma("Alemão");
        assertEquals(2, p.aulasVisiveis().size());

        p.filtrarPorIdioma(null);
        assertEquals(8, p.aulasVisiveis().size());
    }

    // ---------------- detalhes ----------------

    @Test
    void selecionarUmaAulaMostraSeusDetalhes() {
        AulasPanel p = painel(sessaoAluno);

        p.tabela.setRowSelectionInterval(0, 0);

        MateriasDTO aula = p.selecionada();
        assertNotNull(aula);
        assertEquals("Begrüßungen", aula.getTema());
        assertEquals("Begrüßungen auf Deutsch.", aula.getConteudo());
    }

    @Test
    void selecaoSobreviveAoFiltroQuandoAAulaContinuaVisivel() {
        AulasPanel p = painel(sessaoAluno);
        p.cmbIdioma.setSelectedItem("Inglês");
        p.tabela.setRowSelectionInterval(1, 1);
        assertEquals("Present Simple", p.selecionada().getTema());

        p.txtBusca.setText("present");

        assertEquals("Present Simple", p.selecionada().getTema());
        p.txtBusca.setText("greet");
        assertNull(p.selecionada());
    }

    @Test
    void linhaSelecionadaEPintadaComODestaqueDoTema() {
        AulasPanel p = painel(sessaoAluno);
        p.tabela.setSize(700, 400);
        p.tabela.doLayout();
        p.tabela.setRowSelectionInterval(0, 0);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(700, 400, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        p.tabela.paint(g);
        g.dispose();
        int altura = p.tabela.getRowHeight();
        assertEquals(ui.Tema.SELECAO.getRGB(), img.getRGB(150, altura / 2), "linha selecionada");
        assertEquals(java.awt.Color.WHITE.getRGB(), img.getRGB(150, altura + altura / 2), "linha comum");
    }

    // ---------------- permissoes ----------------

    @Test
    void estudanteSoConsultaEmBotoesAdministrativosOcultos() {
        AulasPanel p = painel(sessaoAluno);

        assertFalse(p.btnNova.isVisible());
        assertFalse(p.btnEditar.isVisible());
        assertFalse(p.btnExcluir.isVisible());
    }

    @Test
    void adminVeOsBotoesDeGerenciamento() {
        AulasPanel p = painel(sessaoAdmin);

        assertTrue(p.btnNova.isVisible());
        assertTrue(p.btnEditar.isVisible());
        assertTrue(p.btnExcluir.isVisible());
    }

    @Test
    void estudanteNaoConsegueAbrirFormulariosNemExcluirMesmoPorCodigo() {
        AulasPanel p = painel(sessaoAluno);
        p.tabela.setRowSelectionInterval(0, 0);
        dialogos.responder = true;

        p.novaAula();
        p.editarSelecionada();
        p.excluirSelecionada();

        assertEquals(8, db.aulas.size(), "O banco nao pode ter sido alterado.");
        assertTrue(dialogos.teve(Dialogos.Tipo.AVISO), "O acesso e negado pelo DAO com aviso.");
        assertEquals(1, acessosNegados.get());
    }

    // ---------------- exclusao ----------------

    @Test
    void adminExcluiAulaDepoisDeConfirmar() {
        AulasPanel p = painel(sessaoAdmin);
        p.tabela.setRowSelectionInterval(0, 0);     // Begrüßungen
        dialogos.responder = true;

        p.excluirSelecionada();

        assertNull(db.aulaPorTema("Alemão", "Begrüßungen"));
        assertEquals(7, db.aulas.size());
        assertEquals(7, p.aulasVisiveis().size(), "A lista e atualizada depois da exclusao.");
        assertEquals(1, dialogos.confirmacoes.size());
        assertTrue(dialogos.teve(Dialogos.Tipo.SUCESSO));
    }

    @Test
    void cancelarAConfirmacaoNaoExclui() {
        AulasPanel p = painel(sessaoAdmin);
        p.tabela.setRowSelectionInterval(0, 0);
        dialogos.responder = false;

        p.excluirSelecionada();

        assertEquals(8, db.aulas.size());
        assertFalse(dialogos.teve(Dialogos.Tipo.SUCESSO));
    }

    @Test
    void excluirSemSelecaoPedeParaSelecionar() {
        AulasPanel p = painel(sessaoAdmin);

        p.excluirSelecionada();

        assertTrue(dialogos.teve(Dialogos.Tipo.AVISO));
        assertTrue(dialogos.confirmacoes.isEmpty());
        assertEquals(8, db.aulas.size());
    }

    @Test
    void editarSemSelecaoPedeParaSelecionar() {
        AulasPanel p = painel(sessaoAdmin);

        p.editarSelecionada();

        assertTrue(dialogos.teve(Dialogos.Tipo.AVISO));
    }

    // ---------------- atualizacao da lista ----------------

    @Test
    void recarregarDepoisDeCadastrarSelecionaAAulaNova() {
        AulasPanel p = painel(sessaoAdmin);
        db.addAula("Espanhol", "Comida", "Linguify Demo", "20/11/2026", "Vocabulario de comida.");

        p.recarregar("Espanhol", "Comida");

        assertEquals(9, p.aulasVisiveis().size());
        assertEquals("Comida", p.selecionada().getTema());
    }

    // ---------------- erros ----------------

    @Test
    void bancoIndisponivelNaoQuebraAPaginaEPermiteTentarDeNovo() {
        AulasPanel p = new AulasPanel(sessaoAluno, new MateriasDAO(() -> null), acessosNegados::incrementAndGet);

        p.aoExibir();

        assertTrue(p.aulasVisiveis().isEmpty());
        assertEquals(0, acessosNegados.get());
    }

    @Test
    void contaRemovidaDuranteASessaoEncerraAoCarregar() {
        AulasPanel p = new AulasPanel(sessao(99, "fantasma", "ESTUDANTE"), new MateriasDAO(db::connection),
                acessosNegados::incrementAndGet);

        p.aoExibir();

        assertEquals(1, acessosNegados.get());
        assertTrue(dialogos.teve(Dialogos.Tipo.AVISO));
    }

    // ---------------- auxiliares ----------------

    static SessaoUsuario sessao(int id, String username, String perfil) {
        UsuarioDTO d = new UsuarioDTO();
        d.setId_usuario(id);
        d.setUsername(username);
        d.setPerfil(perfil);
        return new SessaoUsuario(d);
    }
}
