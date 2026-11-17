package DAO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DTO.UsuarioDTO;
import auth.OperacaoUsuarioException;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.FakeBanco;
import util.PasswordHasher;

/**
 * Testes da API segura do {@link UsuarioDAO} SEM MySQL: usam um JDBC falso em
 * memoria ({@link FakeBanco}) que interpreta exatamente os SQLs emitidos pelo DAO.
 */
class UsuarioDAOTest {

    private static final String HASH_ADMIN = PasswordHasher.hash("admin-teste-1");
    private static final String HASH_ALUNO = PasswordHasher.hash("aluno-teste-1");

    private FakeBanco db;
    private UsuarioDAO dao;
    private SessaoUsuario sessaoAdmin;      // id 1 (ADMIN)
    private SessaoUsuario sessaoEstudante;  // id 2 (ESTUDANTE)

    @BeforeEach
    void preparar() {
        db = new FakeBanco();
        db.add("admin", HASH_ADMIN, "ADMIN");      // id 1
        db.add("aluno", HASH_ALUNO, "ESTUDANTE");  // id 2
        dao = new UsuarioDAO(db::connection);
        sessaoAdmin = sessao(1, "admin", "ADMIN");
        sessaoEstudante = sessao(2, "aluno", "ESTUDANTE");
    }

    // ------------------------------------------------------------------
    // Autorizacao
    // ------------------------------------------------------------------

    @Test
    void sessaoNulaEhNegadaSemAbrirConexao() {
        AtomicInteger conexoes = new AtomicInteger();
        UsuarioDAO semBanco = new UsuarioDAO(() -> {
            conexoes.incrementAndGet();
            return db.connection();
        });

        assertTodasNegadas(semBanco, null);
        assertEquals(0, conexoes.get(), "Nenhuma conexao pode ser aberta antes da checagem de sessao.");
    }

    @Test
    void estudanteEhNegadoSemAbrirConexao() {
        AtomicInteger conexoes = new AtomicInteger();
        UsuarioDAO semBanco = new UsuarioDAO(() -> {
            conexoes.incrementAndGet();
            return db.connection();
        });

        assertTodasNegadas(semBanco, sessaoEstudante);
        assertEquals(0, conexoes.get());
    }

    @Test
    void sessaoAdminDesatualizadaEhNegadaPeloBanco() {
        // A sessao em memoria diz ADMIN, mas no banco o usuario 2 e ESTUDANTE.
        SessaoUsuario forjada = sessao(2, "aluno", "ADMIN");

        assertTodasNegadas(dao, forjada);
        assertEquals(2, db.rows.size());
        assertEquals("ESTUDANTE", db.porId(2).perfil);
    }

    @Test
    void sessaoDeUsuarioInexistenteEhNegada() {
        SessaoUsuario fantasma = sessao(99, "fantasma", "ADMIN");

        assertTodasNegadas(dao, fantasma);
    }

    @Test
    void adminRebaixadoNoMeioDaSessaoPerdeOAcesso() throws Exception {
        db.porId(1).perfil = "ESTUDANTE"; // outro admin rebaixou o ator

        assertThrows(AcessoNegadoException.class, () -> dao.listarUsuarios(sessaoAdmin));
    }

    @Test
    void falhaDeConexaoViraOperacaoUsuarioException() {
        UsuarioDAO semConexao = new UsuarioDAO(() -> null);

        assertThrows(OperacaoUsuarioException.class, () -> semConexao.listarUsuarios(sessaoAdmin));
        assertThrows(OperacaoUsuarioException.class,
                () -> semConexao.cadastrarEstudante(dto(0, "novo", "senha123", null)));
    }

    // ------------------------------------------------------------------
    // listarUsuarios
    // ------------------------------------------------------------------

    @Test
    void listagemNuncaRetornaSenhaOuHash() throws Exception {
        List<UsuarioDTO> lista = dao.listarUsuarios(sessaoAdmin);

        assertEquals(2, lista.size());
        for (UsuarioDTO u : lista) {
            assertNull(u.getSenha(), "A listagem nao pode expor senha/hash.");
        }
        assertEquals("admin", lista.get(0).getUsername());
        assertEquals("ADMIN", lista.get(0).getPerfil());
        assertEquals("aluno", lista.get(1).getUsername());
        assertEquals("ESTUDANTE", lista.get(1).getPerfil());
    }

    // ------------------------------------------------------------------
    // cadastrarEstudante (publico)
    // ------------------------------------------------------------------

    @Test
    void cadastroPublicoForcaEstudanteEIgnoraOPerfilDoDto() throws Exception {
        dao.cadastrarEstudante(dto(0, "rodrigo", "rodrigo-teste-1", "ADMIN"));

        FakeBanco.Row criado = db.porUsername("rodrigo");
        assertNotNull(criado);
        assertEquals("ESTUDANTE", criado.perfil);
    }

    @Test
    void cadastroPublicoGravaApenasHashBcrypt() throws Exception {
        dao.cadastrarEstudante(dto(0, "rodrigo", "rodrigo-teste-1", null));

        FakeBanco.Row criado = db.porUsername("rodrigo");
        assertNotEquals("rodrigo-teste-1", criado.senha);
        assertEquals(60, criado.senha.length());
        assertTrue(criado.senha.startsWith("$2a$"));
        assertTrue(PasswordHasher.verify("rodrigo-teste-1", criado.senha));
    }

    @Test
    void cadastroPublicoValidaEntradas() {
        assertThrows(OperacaoUsuarioException.class, () -> dao.cadastrarEstudante(null));
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarEstudante(dto(0, "", "senha123", null)));
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarEstudante(dto(0, "novo", "123", null)));
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarEstudante(dto(0, "novo", null, null)));
        assertEquals(2, db.rows.size());
    }

    @Test
    void cadastroPublicoRejeitaUsernameDuplicadoIgnorandoCaixa() {
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarEstudante(dto(0, "ALUNO", "senha123", null)));
        assertEquals(2, db.rows.size());
    }

    // ------------------------------------------------------------------
    // cadastrarUsuario (administrativo)
    // ------------------------------------------------------------------

    @Test
    void adminPodeCriarOutroAdminComHash() throws Exception {
        dao.cadastrarUsuario(sessaoAdmin, dto(0, "chefe", "chefe123", "ADMIN"));

        FakeBanco.Row criado = db.porUsername("chefe");
        assertEquals("ADMIN", criado.perfil);
        assertTrue(PasswordHasher.verify("chefe123", criado.senha));
    }

    @Test
    void adminPodeCriarEstudante() throws Exception {
        dao.cadastrarUsuario(sessaoAdmin, dto(0, "novo", "novo1234", "estudante"));

        assertEquals("ESTUDANTE", db.porUsername("novo").perfil);
    }

    @Test
    void cadastroAdministrativoValidaEntradas() {
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarUsuario(sessaoAdmin, dto(0, "x1", "senha123", "ROOT")));
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarUsuario(sessaoAdmin, dto(0, "x2", "senha123", null)));
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarUsuario(sessaoAdmin, dto(0, "x3", "12345", "ESTUDANTE")));
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarUsuario(sessaoAdmin, dto(0, "aluno", "senha123", "ESTUDANTE")));
        assertThrows(OperacaoUsuarioException.class, () -> dao.cadastrarUsuario(sessaoAdmin, null));
        assertEquals(2, db.rows.size());
    }

    // ------------------------------------------------------------------
    // atualizarUsuario
    // ------------------------------------------------------------------

    @Test
    void atualizarComSenhaVaziaMantemOHashAtual() throws Exception {
        dao.atualizarUsuario(sessaoAdmin, dto(2, "aluno2", "", "ESTUDANTE"));

        FakeBanco.Row aluno = db.porId(2);
        assertEquals("aluno2", aluno.username);
        assertEquals(HASH_ALUNO, aluno.senha, "Senha vazia deve manter o hash atual.");
    }

    @Test
    void atualizarComSenhaNulaMantemOHashAtual() throws Exception {
        dao.atualizarUsuario(sessaoAdmin, dto(2, "aluno", null, "ESTUDANTE"));

        assertEquals(HASH_ALUNO, db.porId(2).senha);
    }

    @Test
    void atualizarComSenhaNovaGravaNovoHash() throws Exception {
        dao.atualizarUsuario(sessaoAdmin, dto(2, "aluno", "novaSenha9", "ESTUDANTE"));

        String senhaGravada = db.porId(2).senha;
        assertNotEquals(HASH_ALUNO, senhaGravada);
        assertNotEquals("novaSenha9", senhaGravada);
        assertEquals(60, senhaGravada.length());
        assertTrue(PasswordHasher.verify("novaSenha9", senhaGravada));
        assertFalse(PasswordHasher.verify("aluno-teste-1", senhaGravada));
    }

    @Test
    void atualizarComSenhaNovaInvalidaEhRejeitadoESemEfeito() {
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.atualizarUsuario(sessaoAdmin, dto(2, "outro", "abc", "ESTUDANTE")));

        assertEquals("aluno", db.porId(2).username);
        assertEquals(HASH_ALUNO, db.porId(2).senha);
    }

    @Test
    void adminPodePromoverEstudante() throws Exception {
        dao.atualizarUsuario(sessaoAdmin, dto(2, "aluno", "", "ADMIN"));

        assertEquals("ADMIN", db.porId(2).perfil);
    }

    @Test
    void atualizarMantendoOProprioUsernameEhPermitidoMasDuplicarOutroNao() throws Exception {
        dao.atualizarUsuario(sessaoAdmin, dto(2, "aluno", "", "ESTUDANTE"));

        assertThrows(OperacaoUsuarioException.class,
                () -> dao.atualizarUsuario(sessaoAdmin, dto(2, "admin", "", "ESTUDANTE")));
        assertEquals("aluno", db.porId(2).username);
    }

    @Test
    void naoRebaixaOUltimoAdministrador() {
        OperacaoUsuarioException ex = assertThrows(OperacaoUsuarioException.class,
                () -> dao.atualizarUsuario(sessaoAdmin, dto(1, "admin", "", "ESTUDANTE")));

        assertTrue(ex.getMessage().contains("último administrador"));
        assertEquals("ADMIN", db.porId(1).perfil);
    }

    @Test
    void rebaixaAdminQuandoExisteOutro() throws Exception {
        FakeBanco.Row segundo = db.add("chefe", HASH_ADMIN, "ADMIN"); // id 3

        dao.atualizarUsuario(sessaoAdmin, dto(segundo.id, "chefe", "", "ESTUDANTE"));

        assertEquals("ESTUDANTE", db.porId(segundo.id).perfil);
        assertEquals("ADMIN", db.porId(1).perfil);
    }

    @Test
    void atualizarUsuarioInexistenteEhRejeitado() {
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.atualizarUsuario(sessaoAdmin, dto(77, "x", "", "ESTUDANTE")));
    }

    // ------------------------------------------------------------------
    // excluirUsuario
    // ------------------------------------------------------------------

    @Test
    void naoExcluiAPropriaConta() {
        OperacaoUsuarioException ex = assertThrows(OperacaoUsuarioException.class,
                () -> dao.excluirUsuario(sessaoAdmin, 1));

        assertTrue(ex.getMessage().contains("própria conta"));
        assertNotNull(db.porId(1));
    }

    @Test
    void naoExcluiAPropriaContaMesmoHavendoOutroAdmin() {
        db.add("chefe", HASH_ADMIN, "ADMIN");

        assertThrows(OperacaoUsuarioException.class, () -> dao.excluirUsuario(sessaoAdmin, 1));
        assertNotNull(db.porId(1));
    }

    @Test
    void adminExcluiEstudante() throws Exception {
        dao.excluirUsuario(sessaoAdmin, 2);

        assertNull(db.porId(2));
        assertEquals(1, db.rows.size());
    }

    @Test
    void adminExcluiOutroAdminQuandoHaMaisDeUm() throws Exception {
        FakeBanco.Row segundo = db.add("chefe", HASH_ADMIN, "ADMIN");

        dao.excluirUsuario(sessaoAdmin, segundo.id);

        assertNull(db.porId(segundo.id));
        assertEquals("ADMIN", db.porId(1).perfil);
    }

    @Test
    void excluirUsuarioInexistenteEhRejeitado() {
        assertThrows(OperacaoUsuarioException.class, () -> dao.excluirUsuario(sessaoAdmin, 77));
        assertEquals(2, db.rows.size());
    }

    // ------------------------------------------------------------------
    // Recursos
    // ------------------------------------------------------------------

    @Test
    void conexoesSaoSempreFechadasInclusiveEmCasosDeErro() throws Exception {
        dao.listarUsuarios(sessaoAdmin);
        dao.cadastrarEstudante(dto(0, "rodrigo", "rodrigo-teste-1", null));
        assertThrows(OperacaoUsuarioException.class,
                () -> dao.cadastrarEstudante(dto(0, "rodrigo", "rodrigo-teste-1", null)));
        assertThrows(OperacaoUsuarioException.class, () -> dao.excluirUsuario(sessaoAdmin, 1));
        assertThrows(AcessoNegadoException.class, () -> dao.listarUsuarios(sessao(2, "aluno", "ADMIN")));

        assertTrue(db.abertas > 0);
        assertEquals(db.abertas, db.fechadas, "Toda conexao aberta precisa ser fechada.");
    }

    // ------------------------------------------------------------------
    // autenticar
    // ------------------------------------------------------------------

    @Test
    void autenticaComSenhaCorretaSemExporAsenha() throws Exception {
        UsuarioDTO ok = dao.autenticar("admin", "admin-teste-1");

        assertNotNull(ok);
        assertEquals(1, ok.getId_usuario());
        assertEquals("admin", ok.getUsername());
        assertEquals("ADMIN", ok.getPerfil());
        assertNull(ok.getSenha(), "O usuario autenticado nao pode carregar senha/hash.");
    }

    @Test
    void autenticaEstudanteComSeuPerfil() throws Exception {
        UsuarioDTO ok = dao.autenticar("aluno", "aluno-teste-1");

        assertNotNull(ok);
        assertEquals("ESTUDANTE", ok.getPerfil());
    }

    @Test
    void autenticaIgnorandoCaixaEEspacosDoUsername() throws Exception {
        assertNotNull(dao.autenticar("  ADMIN ", "admin-teste-1"));
    }

    @Test
    void rejeitaSenhaIncorretaUsuarioInexistenteEEntradasVazias() throws Exception {
        assertNull(dao.autenticar("admin", "errada"));
        assertNull(dao.autenticar("admin", ""));
        assertNull(dao.autenticar("admin", null));
        assertNull(dao.autenticar("", "admin-teste-1"));
        assertNull(dao.autenticar(null, "admin-teste-1"));
        assertNull(dao.autenticar("fantasma", "admin-teste-1"));
    }

    @Test
    void usuarioLegadoComSenhaEmTextoPuroNaoAutentica() throws Exception {
        db.add("legado", "123", "ESTUDANTE");

        assertNull(dao.autenticar("legado", "123"));
    }

    @Test
    void autenticacaoSemBancoLancaExcecaoAmigavel() {
        UsuarioDAO semBanco = new UsuarioDAO(() -> null);

        OperacaoUsuarioException ex = assertThrows(OperacaoUsuarioException.class,
                () -> semBanco.autenticar("admin", "admin-teste-1"));
        assertTrue(ex.getMessage().contains("banco de dados"));
    }

    // ------------------------------------------------------------------
    // Auxiliares de teste
    // ------------------------------------------------------------------

    private static void assertTodasNegadas(UsuarioDAO alvo, SessaoUsuario sessao) {
        assertThrows(AcessoNegadoException.class, () -> alvo.listarUsuarios(sessao));
        assertThrows(AcessoNegadoException.class,
                () -> alvo.cadastrarUsuario(sessao, dto(0, "novo", "senha123", "ESTUDANTE")));
        assertThrows(AcessoNegadoException.class,
                () -> alvo.cadastrarUsuario(sessao, dto(0, "novoadmin", "senha123", "ADMIN")));
        assertThrows(AcessoNegadoException.class,
                () -> alvo.atualizarUsuario(sessao, dto(2, "aluno", "", "ADMIN")));
        assertThrows(AcessoNegadoException.class, () -> alvo.excluirUsuario(sessao, 2));
    }

    private static UsuarioDTO dto(int id, String username, String senha, String perfil) {
        UsuarioDTO d = new UsuarioDTO();
        d.setId_usuario(id);
        d.setUsername(username);
        d.setSenha(senha);
        d.setPerfil(perfil);
        return d;
    }

    private static SessaoUsuario sessao(int id, String username, String perfil) {
        UsuarioDTO d = new UsuarioDTO();
        d.setId_usuario(id);
        d.setUsername(username);
        d.setPerfil(perfil);
        return new SessaoUsuario(d);
    }
}
