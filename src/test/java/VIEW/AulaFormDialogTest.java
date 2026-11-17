package VIEW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DAO.MateriasDAO;
import DTO.MateriasDTO;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.DadosDemo;
import testutil.FakeBanco;
import testutil.UiTeste;
import ui.Dialogos;

class AulaFormDialogTest extends UiTeste {

    private FakeBanco db;
    private MateriasDAO dao;
    private SessaoUsuario admin;
    private List<MateriasDTO> salvas;
    private List<AcessoNegadoException> negados;

    @BeforeEach
    void preparar() {
        db = DadosDemo.popular(new FakeBanco());
        dao = new MateriasDAO(db::connection);
        admin = AulasPanelTest.sessao(1, "admin", "ADMIN");
        salvas = new ArrayList<>();
        negados = new ArrayList<>();
    }

    private AulaFormDialog novo(SessaoUsuario sessao) {
        return new AulaFormDialog(null, sessao, dao, null, List.of(), salvas::add, negados::add);
    }

    private static void preencher(AulaFormDialog d, String idioma, String tema, String professor, String data,
            String conteudo) {
        d.cmbIdioma.setSelectedItem(idioma);
        d.cmbIdioma.getEditor().setItem(idioma);
        d.txtTema.setText(tema);
        d.txtProfessor.setText(professor);
        d.txtData.setText(data);
        d.txtConteudo.setText(conteudo);
    }

    @Test
    void novaAulaComecaComDataDeHojeEIdiomaPadrao() {
        AulaFormDialog d = novo(admin);

        assertTrue(d.txtData.getText().matches("\\d{2}/\\d{2}/\\d{4}"));
        assertEquals("Inglês", d.cmbIdioma.getEditor().getItem());
        assertEquals("Nova aula", d.getTitle());
        d.dispose();
    }

    @Test
    void salvarCadastraAulaComTodosOsCamposEAvisaOChamador() {
        AulaFormDialog d = novo(admin);
        preencher(d, "Espanhol", "Comida", "Profa. Ana", "20/11/2026", "Vocabulario de comida em español.");

        d.btnSalvar.doClick(0);

        FakeBanco.Aula gravada = db.aulaPorTema("Espanhol", "Comida");
        assertNotNull(gravada);
        assertEquals("Profa. Ana", gravada.professor);
        assertEquals("20/11/2026", gravada.data);
        assertEquals("Vocabulario de comida em español.", gravada.conteudo);
        assertEquals(9, db.aulas.size());
        assertEquals(1, salvas.size());
        assertEquals("Espanhol", salvas.get(0).getMateria());
        assertEquals("Comida", salvas.get(0).getTema());
        assertTrue(dialogos.teve(Dialogos.Tipo.SUCESSO));
        assertFalse(d.isDisplayable(), "O formulario fecha depois de salvar.");
    }

    @Test
    void aceitaUmIdiomaNovoDigitado() {
        AulaFormDialog d = novo(admin);
        preencher(d, "Italiano", "Saluti", "Prof. Luca", "21/11/2026", "Saluti e presentazioni.");

        d.btnSalvar.doClick(0);

        assertNotNull(db.aulaPorTema("Italiano", "Saluti"));
    }

    @Test
    void camposInvalidosMostramErroNoFormularioENaoGravamNemFecham() {
        AulaFormDialog d = novo(admin);

        preencher(d, "Inglês", "", "Prof.", "20/11/2026", "x");
        d.btnSalvar.doClick(0);
        assertEquals("O nome da aula é obrigatório.", d.erro());

        preencher(d, "Inglês", "Past Simple", "Prof.", "32/13/2026", "x");
        d.btnSalvar.doClick(0);
        assertTrue(d.erro().contains("Data inválida"));

        preencher(d, "Inglês", "Past Simple", "Prof.", "20/11/2026", "x".repeat(256));
        d.btnSalvar.doClick(0);
        assertTrue(d.erro().contains("no máximo 255"));

        assertEquals(8, db.aulas.size());
        assertTrue(salvas.isEmpty());
        assertTrue(d.isDisplayable());
        assertFalse(dialogos.teve(Dialogos.Tipo.SUCESSO));
        d.dispose();
    }

    @Test
    void aulaDuplicadaMostraErro() {
        AulaFormDialog d = novo(admin);
        preencher(d, "Inglês", "Greetings", "Prof.", "20/11/2026", "x");

        d.btnSalvar.doClick(0);

        assertTrue(d.erro().contains("Já existe"));
        assertEquals(8, db.aulas.size());
        d.dispose();
    }

    @Test
    void editarCarregaOsDadosEGravaAsAlteracoesInclusiveConteudo() {
        FakeBanco.Aula alvo = db.aulaPorTema("Inglês", "Greetings");
        MateriasDTO inicial = new MateriasDTO();
        inicial.setId_materias(alvo.id);
        inicial.setMateria(alvo.materia);
        inicial.setTema(alvo.tema);
        inicial.setProfessor(alvo.professor);
        inicial.setData(alvo.data);
        inicial.setConteudo(alvo.conteudo);
        AulaFormDialog d = new AulaFormDialog(null, admin, dao, inicial, List.of(), salvas::add, negados::add);

        assertEquals("Editar aula", d.getTitle());
        assertEquals("Greetings", d.txtTema.getText());
        assertEquals("Cumprimentos em inglês.", d.txtConteudo.getText());

        d.txtTema.setText("Greetings and Goodbyes");
        d.txtConteudo.setText("Cumprimentos e despedidas em inglês.");
        d.btnSalvar.doClick(0);

        assertEquals("Greetings and Goodbyes", db.aulaPorId(alvo.id).tema);
        assertEquals("Cumprimentos e despedidas em inglês.", db.aulaPorId(alvo.id).conteudo);
        assertEquals(8, db.aulas.size());
        assertTrue(dialogos.ultima().contains("atualizada"));
    }

    @Test
    void estudanteNaoConsegueSalvarMesmoComOFormularioAberto() {
        AulaFormDialog d = novo(AulasPanelTest.sessao(2, "aluno", "ESTUDANTE"));
        preencher(d, "Inglês", "Hack", "Prof.", "20/11/2026", "x");

        d.btnSalvar.doClick(0);

        assertEquals(8, db.aulas.size());
        assertEquals(1, negados.size());
        assertFalse(d.isDisplayable());
    }

    @Test
    void cancelarFechaSemGravar() {
        AulaFormDialog d = novo(admin);
        preencher(d, "Inglês", "Novo", "Prof.", "20/11/2026", "x");

        d.btnCancelar.doClick(0);

        assertFalse(d.isDisplayable());
        assertEquals(8, db.aulas.size());
        assertTrue(salvas.isEmpty());
    }
}
