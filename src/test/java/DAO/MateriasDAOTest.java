package DAO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DTO.MateriasDTO;
import DTO.UsuarioDTO;
import auth.OperacaoUsuarioException;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.FakeBanco;

/** Testes do {@link MateriasDAO} com banco falso em memoria (sem MySQL). */
class MateriasDAOTest {

    private FakeBanco db;
    private MateriasDAO dao;
    private SessaoUsuario admin;       // id 1
    private SessaoUsuario estudante;   // id 2

    @BeforeEach
    void preparar() {
        db = new FakeBanco();
        db.add("admin", "hash", "ADMIN");
        db.add("aluno", "hash", "ESTUDANTE");
        db.addAula("Inglês", "Greetings", "Linguify Demo", "09/11/2026", "Cumprimentos em inglês.");
        db.addAula("Francês", "Salutations", "Linguify Demo", "13/11/2026", "Cumprimentos em francês.");
        db.addAula("Alemão", "Begrüßungen", "Linguify Demo", "17/11/2026", "Cumprimentos em alemão.");
        dao = new MateriasDAO(db::connection);
        admin = sessao(1, "admin", "ADMIN");
        estudante = sessao(2, "aluno", "ESTUDANTE");
    }

    // ---------------- leitura ----------------

    @Test
    void adminEEstudantePodemListar() throws Exception {
        assertEquals(3, dao.listarAulas(admin).size());
        assertEquals(3, dao.listarAulas(estudante).size());
    }

    @Test
    void listagemVemOrdenadaPorIdiomaComTodosOsCampos() throws Exception {
        List<MateriasDTO> lista = dao.listarAulas(admin);

        assertEquals("Alemão", lista.get(0).getMateria());
        assertEquals("Francês", lista.get(1).getMateria());
        assertEquals("Inglês", lista.get(2).getMateria());
        MateriasDTO ingles = lista.get(2);
        assertEquals("Greetings", ingles.getTema());
        assertEquals("Linguify Demo", ingles.getProfessor());
        assertEquals("09/11/2026", ingles.getData());
        assertEquals("Cumprimentos em inglês.", ingles.getConteudo());
    }

    @Test
    void valoresNulosViramTextoVazio() throws Exception {
        FakeBanco.Aula a = db.addAula("Espanhol", "Saludos", null, null, null);

        MateriasDTO lida = dao.listarAulas(admin).stream()
                .filter(x -> x.getId_materias() == a.id).findFirst().orElseThrow();

        assertEquals("", lida.getProfessor());
        assertEquals("", lida.getData());
        assertEquals("", lida.getConteudo());
    }

    @Test
    void listarExigeUsuarioAutenticadoQueExistaNoBanco() {
        AtomicInteger conexoes = new AtomicInteger();
        MateriasDAO semBanco = new MateriasDAO(() -> {
            conexoes.incrementAndGet();
            return db.connection();
        });

        assertThrows(AcessoNegadoException.class, () -> semBanco.listarAulas(null));
        assertEquals(0, conexoes.get(), "Sessao nula deve ser negada antes de abrir conexao.");
        assertThrows(AcessoNegadoException.class, () -> dao.listarAulas(sessao(99, "fantasma", "ESTUDANTE")));
    }

    // ---------------- permissoes de escrita ----------------

    @Test
    void estudanteNaoCadastraEditaNemExclui() {
        assertTodasNegadas(estudante);
        assertEquals(3, db.aulas.size());
    }

    @Test
    void sessaoNulaEhNegadaSemAbrirConexao() {
        AtomicInteger conexoes = new AtomicInteger();
        MateriasDAO semBanco = new MateriasDAO(() -> {
            conexoes.incrementAndGet();
            return db.connection();
        });

        assertThrows(AcessoNegadoException.class, () -> semBanco.cadastrarAula(null, aula(0, "Inglês", "X")));
        assertThrows(AcessoNegadoException.class, () -> semBanco.atualizarAula(null, aula(1, "Inglês", "X")));
        assertThrows(AcessoNegadoException.class, () -> semBanco.excluirAula(null, 1));
        assertEquals(0, conexoes.get());
    }

    @Test
    void sessaoQueDizAdminMasNoBancoEEstudanteEhNegada() {
        assertTodasNegadas(sessao(2, "aluno", "ADMIN"));
        assertEquals(3, db.aulas.size());
    }

    // ---------------- cadastro ----------------

    @Test
    void adminCadastraAulaComTodasAsColunas() throws Exception {
        MateriasDTO nova = aula(0, "Espanhol", "Saludos");
        nova.setProfessor("Profa. Ana");
        nova.setData("12/11/2026");
        nova.setConteudo("Hola, buenos días.");

        dao.cadastrarAula(admin, nova);

        FakeBanco.Aula gravada = db.aulaPorTema("Espanhol", "Saludos");
        assertNotNull(gravada);
        assertEquals("Profa. Ana", gravada.professor);
        assertEquals("12/11/2026", gravada.data);
        assertEquals("Hola, buenos días.", gravada.conteudo, "O conteudo precisa ser gravado.");
        assertEquals(4, db.aulas.size());
    }

    @Test
    void cadastroAparaEspacosENormalizaAData() throws Exception {
        MateriasDTO nova = aula(0, "  Espanhol ", "  Verbos básicos ");
        nova.setData(" 05/01/2027 ");

        dao.cadastrarAula(admin, nova);

        FakeBanco.Aula gravada = db.aulaPorTema("Espanhol", "Verbos básicos");
        assertNotNull(gravada);
        assertEquals("Espanhol", gravada.materia);
        assertEquals("05/01/2027", gravada.data);
    }

    @Test
    void cadastroRejeitaCamposInvalidosSemGravar() {
        MateriasDTO semTema = aula(0, "Inglês", "  ");
        MateriasDTO dataRuim = aula(0, "Inglês", "Past Simple");
        dataRuim.setData("31/02/2026");
        MateriasDTO conteudoGrande = aula(0, "Inglês", "Past Simple");
        conteudoGrande.setConteudo("x".repeat(256));
        MateriasDTO professorGrande = aula(0, "Inglês", "Past Simple");
        professorGrande.setProfessor("p".repeat(51));
        MateriasDTO semConteudo = aula(0, "Inglês", "Past Simple");
        semConteudo.setConteudo("");

        for (MateriasDTO ruim : new MateriasDTO[] {semTema, dataRuim, conteudoGrande, professorGrande,
            semConteudo, null}) {
            assertThrows(OperacaoUsuarioException.class, () -> dao.cadastrarAula(admin, ruim));
        }
        assertEquals(3, db.aulas.size());
    }

    @Test
    void cadastroRejeitaAulaDuplicadaNoMesmoIdiomaIgnorandoCaixa() {
        OperacaoUsuarioException ex = assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarAula(admin, aula(0, "inglês", "GREETINGS")));

        assertTrue(ex.getMessage().contains("Já existe"));
        assertEquals(3, db.aulas.size());
    }

    @Test
    void mesmoNomeDeAulaEmOutroIdiomaEhPermitido() throws Exception {
        dao.cadastrarAula(admin, aula(0, "Espanhol", "Greetings"));

        assertEquals(4, db.aulas.size());
    }

    // ---------------- edicao ----------------

    @Test
    void adminAtualizaTodosOsCamposInclusiveConteudo() throws Exception {
        FakeBanco.Aula alvo = db.aulaPorTema("Inglês", "Greetings");
        MateriasDTO nova = aula(alvo.id, "Inglês", "Greetings and Introductions");
        nova.setProfessor("Prof. Novo");
        nova.setData("20/11/2026");
        nova.setConteudo("Conteúdo revisado.");

        dao.atualizarAula(admin, nova);

        assertEquals("Greetings and Introductions", db.aulaPorId(alvo.id).tema);
        assertEquals("Prof. Novo", db.aulaPorId(alvo.id).professor);
        assertEquals("20/11/2026", db.aulaPorId(alvo.id).data);
        assertEquals("Conteúdo revisado.", db.aulaPorId(alvo.id).conteudo);
        assertEquals(3, db.aulas.size());
    }

    @Test
    void edicaoPodeMantersoONomeSemContarComoDuplicada() throws Exception {
        FakeBanco.Aula alvo = db.aulaPorTema("Inglês", "Greetings");

        dao.atualizarAula(admin, aula(alvo.id, "Inglês", "Greetings"));

        assertEquals(3, db.aulas.size());
    }

    @Test
    void edicaoNaoPodeColidirComOutraAula() {
        FakeBanco.Aula alvo = db.aulaPorTema("Inglês", "Greetings");
        db.addAula("Inglês", "Present Simple", "Linguify Demo", "10/11/2026", "x");

        assertThrows(OperacaoUsuarioException.class,
                () -> dao.atualizarAula(admin, aula(alvo.id, "Inglês", "Present Simple")));
        assertEquals("Greetings", db.aulaPorId(alvo.id).tema);
    }

    @Test
    void edicaoDeAulaInexistenteEhRejeitada() {
        assertThrows(OperacaoUsuarioException.class, () -> dao.atualizarAula(admin, aula(999, "Inglês", "X")));
    }

    // ---------------- exclusao ----------------

    @Test
    void adminExcluiApenasAAulaIndicada() throws Exception {
        FakeBanco.Aula alvo = db.aulaPorTema("Francês", "Salutations");

        dao.excluirAula(admin, alvo.id);

        assertNull(db.aulaPorId(alvo.id));
        assertEquals(2, db.aulas.size());
        assertNotNull(db.aulaPorTema("Inglês", "Greetings"));
    }

    @Test
    void exclusaoDeAulaInexistenteEhRejeitada() {
        assertThrows(OperacaoUsuarioException.class, () -> dao.excluirAula(admin, 999));
        assertEquals(3, db.aulas.size());
    }

    // ---------------- recursos e erros ----------------

    @Test
    void conexoesSaoSempreFechadasInclusiveEmErros() throws Exception {
        dao.listarAulas(admin);
        dao.cadastrarAula(admin, aula(0, "Espanhol", "Saludos"));
        assertThrows(OperacaoUsuarioException.class, () -> dao.cadastrarAula(admin, aula(0, "Espanhol", "Saludos")));
        assertThrows(OperacaoUsuarioException.class, () -> dao.excluirAula(admin, 999));
        assertThrows(AcessoNegadoException.class, () -> dao.excluirAula(estudante, 1));

        assertTrue(db.abertas > 0);
        assertEquals(db.abertas, db.fechadas, "Toda conexao aberta precisa ser fechada.");
    }

    @Test
    void bancoIndisponivelViraMensagemAmigavel() {
        MateriasDAO semConexao = new MateriasDAO(() -> null);

        OperacaoUsuarioException ex = assertThrows(OperacaoUsuarioException.class,
                () -> semConexao.listarAulas(admin));
        assertTrue(ex.getMessage().contains("banco de dados"));
    }

    // ---------------- auxiliares ----------------

    private void assertTodasNegadas(SessaoUsuario sessao) {
        assertThrows(AcessoNegadoException.class, () -> dao.cadastrarAula(sessao, aula(0, "Italiano", "Ciao")));
        assertThrows(AcessoNegadoException.class,
                () -> dao.atualizarAula(sessao, aula(db.aulas.get(0).id, "Inglês", "Outro")));
        assertThrows(AcessoNegadoException.class, () -> dao.excluirAula(sessao, db.aulas.get(0).id));
    }

    private static MateriasDTO aula(int id, String idioma, String tema) {
        MateriasDTO a = new MateriasDTO();
        a.setId_materias(id);
        a.setMateria(idioma);
        a.setTema(tema);
        a.setProfessor("Linguify Demo");
        a.setData("01/12/2026");
        a.setConteudo("Conteúdo da aula.");
        return a;
    }

    private static SessaoUsuario sessao(int id, String username, String perfil) {
        UsuarioDTO d = new UsuarioDTO();
        d.setId_usuario(id);
        d.setUsername(username);
        d.setPerfil(perfil);
        return new SessaoUsuario(d);
    }
}
