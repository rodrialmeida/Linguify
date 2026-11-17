package VIEW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DAO.UsuarioDAO;
import DTO.UsuarioDTO;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.DadosDemo;
import testutil.FakeBanco;
import testutil.UiTeste;
import ui.Dialogos;
import util.PasswordHasher;

class UsuariosPanelTest extends UiTeste {

    private FakeBanco db;
    private UsuarioDAO dao;
    private AtomicInteger negados;
    private SessaoUsuario admin;      // id 1
    private SessaoUsuario aluno;      // id 2

    @BeforeEach
    void preparar() {
        db = DadosDemo.popular(new FakeBanco());
        dao = new UsuarioDAO(db::connection);
        negados = new AtomicInteger();
        admin = AulasPanelTest.sessao(1, "admin", "ADMIN");
        aluno = AulasPanelTest.sessao(2, "aluno", "ESTUDANTE");
    }

    private UsuariosPanel painelAdmin() throws Exception {
        UsuariosPanel p = new UsuariosPanel(admin, dao, negados::incrementAndGet);
        p.aoExibir();
        return p;
    }

    private void selecionar(UsuariosPanel p, String username) {
        for (int i = 0; i < p.modelo.getRowCount(); i++) {
            if (username.equals(p.modelo.getValueAt(i, 1))) {
                p.tabela.setRowSelectionInterval(i, i);
                return;
            }
        }
        throw new AssertionError("usuario nao esta na tabela: " + username);
    }

    // ---------------- acesso e conteudo ----------------

    @Test
    void estudanteNaoConsegueCriarASecaoDeUsuarios() {
        assertThrows(AcessoNegadoException.class, () -> new UsuariosPanel(aluno, dao, negados::incrementAndGet));
        assertThrows(AcessoNegadoException.class, () -> new UsuariosPanel(null, dao, negados::incrementAndGet));
    }

    @Test
    void tabelaMostraSomenteIdUsuarioEPerfil() throws Exception {
        UsuariosPanel p = painelAdmin();

        assertEquals(3, p.modelo.getColumnCount());
        assertEquals("ID", p.modelo.getColumnName(0));
        assertEquals("Usuário", p.modelo.getColumnName(1));
        assertEquals("Perfil", p.modelo.getColumnName(2));
        assertEquals(3, p.modelo.getRowCount());
        assertEquals("admin", p.modelo.getValueAt(0, 1));
        assertEquals("ADMIN", p.modelo.getValueAt(0, 2));
        assertEquals("ESTUDANTE", p.modelo.getValueAt(1, 2));
    }

    @Test
    void senhasEHashesNuncaAparecemNaTabela() throws Exception {
        UsuariosPanel p = painelAdmin();

        for (int l = 0; l < p.modelo.getRowCount(); l++) {
            for (int c = 0; c < p.modelo.getColumnCount(); c++) {
                String valor = String.valueOf(p.modelo.getValueAt(l, c));
                assertFalse(valor.contains("$2a$"), "hash na tabela: " + valor);
                assertFalse(valor.contains("admin-teste-1") || valor.contains("aluno-teste-1") || valor.contains("rodrigo-teste-2"));
            }
        }
    }

    // ---------------- exclusao ----------------

    @Test
    void contaPropriaFicaComBotaoExcluirDesabilitado() throws Exception {
        UsuariosPanel p = painelAdmin();

        selecionar(p, "admin");
        assertFalse(p.btnExcluir.isEnabled());

        selecionar(p, "aluno");
        assertTrue(p.btnExcluir.isEnabled());
    }

    @Test
    void naoExcluiAPropriaContaMesmoPorCodigo() throws Exception {
        UsuariosPanel p = painelAdmin();
        selecionar(p, "admin");
        dialogos.responder = true;

        p.excluirSelecionado();

        assertNotNull(db.porUsername("admin"));
        assertTrue(dialogos.teve(Dialogos.Tipo.ERRO));
        assertTrue(dialogos.ultima().contains("própria conta"));
    }

    @Test
    void adminExcluiOutroUsuarioDepoisDeConfirmar() throws Exception {
        UsuariosPanel p = painelAdmin();
        selecionar(p, "rodrigo");
        dialogos.responder = true;

        p.excluirSelecionado();

        assertNull(db.porUsername("rodrigo"));
        assertEquals(2, db.rows.size());
        assertEquals(2, p.modelo.getRowCount(), "A tabela e atualizada.");
        assertEquals(1, dialogos.confirmacoes.size());
        assertTrue(dialogos.teve(Dialogos.Tipo.SUCESSO));
    }

    @Test
    void cancelarAConfirmacaoNaoExclui() throws Exception {
        UsuariosPanel p = painelAdmin();
        selecionar(p, "rodrigo");
        dialogos.responder = false;

        p.excluirSelecionado();

        assertNotNull(db.porUsername("rodrigo"));
        assertFalse(dialogos.teve(Dialogos.Tipo.SUCESSO));
    }

    @Test
    void excluirSemSelecaoPedeParaSelecionar() throws Exception {
        UsuariosPanel p = painelAdmin();

        p.excluirSelecionado();

        assertTrue(dialogos.teve(Dialogos.Tipo.AVISO));
        assertEquals(3, db.rows.size());
    }

    @Test
    void administradorRebaixadoPorOutraContaPassaAPoderSerExcluido() throws Exception {
        db.add("chefe", "x", "ADMIN");
        UsuariosPanel p = painelAdmin();
        // O administrador "chefe" (id 4) e rebaixado por outra conta; o unico ADMIN restante e "admin".
        db.porUsername("chefe").perfil = "ESTUDANTE";
        p.recarregar();
        selecionar(p, "chefe");

        p.excluirSelecionado();     // chefe deixou de ser admin: pode ser excluido

        assertNull(db.porUsername("chefe"));
        assertNotNull(db.porUsername("admin"));
    }

    // ---------------- formulario ----------------

    private UsuarioFormDialog novo() {
        return new UsuarioFormDialog(null, admin, dao, null, () -> { }, e -> negados.incrementAndGet());
    }

    private UsuarioFormDialog editar(String username) {
        FakeBanco.Row r = db.porUsername(username);
        UsuarioDTO d = new UsuarioDTO();
        d.setId_usuario(r.id);
        d.setUsername(r.username);
        d.setPerfil(r.perfil);
        return new UsuarioFormDialog(null, admin, dao, d, () -> { }, e -> negados.incrementAndGet());
    }

    @Test
    void adminCadastraUsuarioEscolhendoOPerfilEGravaBcrypt() {
        UsuarioFormDialog f = novo();
        f.txtUsername.setText("novoadmin");
        f.cmbPerfil.setSelectedItem("ADMIN");
        f.txtSenha.setText("senha123");

        f.btnSalvar.doClick(0);

        FakeBanco.Row criado = db.porUsername("novoadmin");
        assertNotNull(criado);
        assertEquals("ADMIN", criado.perfil);
        assertTrue(criado.senha.startsWith("$2a$") && criado.senha.length() == 60);
        assertTrue(PasswordHasher.verify("senha123", criado.senha));
        assertTrue(dialogos.teve(Dialogos.Tipo.SUCESSO));
        assertFalse(f.isDisplayable());
    }

    @Test
    void cadastroPadraoEEstudante() {
        UsuarioFormDialog f = novo();
        f.txtUsername.setText("novoaluno");
        f.txtSenha.setText("senha123");

        f.btnSalvar.doClick(0);

        assertEquals("ESTUDANTE", db.porUsername("novoaluno").perfil);
    }

    @Test
    void cadastroInvalidoMostraErroNoFormulario() {
        UsuarioFormDialog f = novo();
        f.txtUsername.setText("novo");
        f.txtSenha.setText("123");

        f.btnSalvar.doClick(0);
        assertTrue(f.erro().contains("mínimo"));

        f.txtUsername.setText("aluno");
        f.txtSenha.setText("senha123");
        f.btnSalvar.doClick(0);
        assertTrue(f.erro().contains("Já existe"));

        assertEquals(3, db.rows.size());
        assertTrue(f.isDisplayable());
        f.dispose();
    }

    @Test
    void editarComSenhaVaziaMantemASenhaAtualEAlteraONome() {
        String hashAntes = db.porUsername("rodrigo").senha;
        UsuarioFormDialog f = editar("rodrigo");
        assertEquals("rodrigo", f.txtUsername.getText());
        assertEquals(0, f.txtSenha.getPassword().length, "A senha nunca e carregada no formulario.");

        f.txtUsername.setText("rodrigo.silva");
        f.btnSalvar.doClick(0);

        FakeBanco.Row r = db.porUsername("rodrigo.silva");
        assertNotNull(r);
        assertEquals(hashAntes, r.senha);
    }

    @Test
    void editarComSenhaNovaGravaNovoHash() {
        String hashAntes = db.porUsername("rodrigo").senha;
        UsuarioFormDialog f = editar("rodrigo");

        f.txtSenha.setText("novaSenha9");
        f.btnSalvar.doClick(0);

        String depois = db.porUsername("rodrigo").senha;
        assertNotEquals(hashAntes, depois);
        assertTrue(PasswordHasher.verify("novaSenha9", depois));
    }

    @Test
    void adminAlteraOPerfilDeUmEstudante() {
        UsuarioFormDialog f = editar("aluno");
        f.cmbPerfil.setSelectedItem("ADMIN");

        f.btnSalvar.doClick(0);

        assertEquals("ADMIN", db.porUsername("aluno").perfil);
    }

    @Test
    void naoRebaixaOUltimoAdministrador() {
        UsuarioFormDialog f = editar("admin");
        f.cmbPerfil.setSelectedItem("ESTUDANTE");

        f.btnSalvar.doClick(0);

        assertTrue(f.erro().contains("último administrador"));
        assertEquals("ADMIN", db.porUsername("admin").perfil);
        f.dispose();
    }

    @Test
    void formularioDeContaSemPermissaoAvisaOChamador() {
        db.porId(1).perfil = "ESTUDANTE";       // o ator foi rebaixado
        UsuarioFormDialog f = novo();
        f.txtUsername.setText("x1");
        f.txtSenha.setText("senha123");

        f.btnSalvar.doClick(0);

        assertEquals(1, negados.get());
        assertNull(db.porUsername("x1"));
    }

    @Test
    void bancoForaDoArNaoQuebraAPagina() throws Exception {
        UsuariosPanel p = new UsuariosPanel(admin, new UsuarioDAO(() -> null), negados::incrementAndGet);

        p.aoExibir();

        assertEquals(0, p.modelo.getRowCount());
        assertEquals(0, negados.get());
    }
}
