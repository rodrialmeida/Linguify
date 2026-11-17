package VIEW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DAO.UsuarioDAO;
import DTO.UsuarioDTO;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.DadosDemo;
import testutil.FakeBanco;
import testutil.UiTeste;
import ui.Dialogos;
import util.PasswordHasher;

class LoginViewTest extends UiTeste {

    private FakeBanco db;
    private List<UsuarioDTO> entraram;
    private LoginView view;

    @BeforeEach
    void criar() {
        db = DadosDemo.popular(new FakeBanco());
        entraram = new ArrayList<>();
        view = new LoginView(new UsuarioDAO(db::connection), entraram::add);
        view.pack();   // torna a janela displayable (sem exibi-la)
    }

    @AfterEach
    void fechar() {
        view.dispose();
    }

    private void entrarCom(String usuario, String senha) {
        view.txtUsuario.setText(usuario);
        view.txtSenha.setText(senha);
        view.btnEntrar.doClick(0);
    }

    // ---------------- login ----------------

    @Test
    void adminAlunoERodrigoEntramComSeusPerfis() {
        entrarCom("admin", "admin-teste-1");
        entrarCom("aluno", "aluno-teste-1");
        entrarCom("rodrigo", "rodrigo-teste-2");

        assertEquals(3, entraram.size());
        assertEquals("ADMIN", entraram.get(0).getPerfil());
        assertEquals("ESTUDANTE", entraram.get(1).getPerfil());
        assertEquals("ESTUDANTE", entraram.get(2).getPerfil());
        assertEquals("rodrigo", entraram.get(2).getUsername());
        entraram.forEach(u -> assertNull(u.getSenha(), "O usuario entregue nao pode carregar senha."));
    }

    @Test
    void loginComSucessoFechaATelaDeLogin() {
        entrarCom("aluno", "aluno-teste-1");

        assertFalse(view.isDisplayable());
    }

    @Test
    void senhaIncorretaMostraErroLimpaASenhaENaoEntra() {
        entrarCom("admin", "errada");

        assertTrue(entraram.isEmpty());
        assertEquals("Usuário ou senha inválidos.", view.erroLogin());
        assertEquals(0, view.txtSenha.getPassword().length);
        assertTrue(view.isDisplayable());
        assertTrue(view.btnEntrar.isEnabled(), "O botao volta ao normal depois do erro.");
        assertEquals("Entrar", view.btnEntrar.getText());
    }

    @Test
    void usuarioInexistenteEhRejeitado() {
        entrarCom("fantasma", "qualquer");

        assertTrue(entraram.isEmpty());
        assertEquals("Usuário ou senha inválidos.", view.erroLogin());
    }

    @Test
    void camposVaziosMostramMensagemSemConsultarOBanco() {
        int antes = db.abertas;

        entrarCom("", "");

        assertEquals("Informe o usuário e a senha.", view.erroLogin());
        assertEquals(antes, db.abertas);
        assertTrue(entraram.isEmpty());
    }

    @Test
    void bancoIndisponivelMostraMensagemAmigavelSemTravar() {
        view.dispose();
        view = new LoginView(new UsuarioDAO(() -> null), entraram::add);
        view.pack();

        entrarCom("admin", "admin-teste-1");

        assertTrue(view.erroLogin().contains("banco de dados"));
        assertTrue(entraram.isEmpty());
        assertTrue(view.btnEntrar.isEnabled());
    }

    @Test
    void enterNoCampoDeSenhaEnviaOFormulario() {
        view.txtUsuario.setText("aluno");
        view.txtSenha.setText("aluno-teste-1");

        view.txtSenha.postActionEvent();

        assertEquals(1, entraram.size());
    }

    @Test
    void botaoPadraoDaJanelaEEntrar() {
        assertEquals(view.btnEntrar, view.getRootPane().getDefaultButton());
    }

    @Test
    void mostrarEOcultarSenhaAlterna() {
        JToggleButton olho = (JToggleButton) view.txtSenha.getClientProperty("JTextField.trailingComponent");
        assertNotNull(olho, "Deve haver um botao de mostrar/ocultar senha.");
        char oculto = view.txtSenha.getEchoChar();
        assertTrue(oculto != 0);

        olho.doClick(0);
        assertEquals(0, view.txtSenha.getEchoChar(), "Senha visivel.");

        olho.doClick(0);
        assertEquals(oculto, view.txtSenha.getEchoChar(), "Senha oculta novamente.");
    }

    // ---------------- cadastro ----------------

    private void cadastrar(String usuario, String senha, String confirmacao) {
        view.mostrarCadastro();
        view.cadUsuario.setText(usuario);
        view.cadSenha.setText(senha);
        view.cadConfirmacao.setText(confirmacao);
        view.btnCriar.doClick(0);
    }

    @Test
    void cadastroCriaEstudanteComBcryptEVoltaAoLoginComUsuarioPreenchido() {
        cadastrar("maria", "maria123", "maria123");

        FakeBanco.Row criada = db.porUsername("maria");
        assertNotNull(criada);
        assertEquals("ESTUDANTE", criada.perfil);
        assertTrue(criada.senha.startsWith("$2a$") && criada.senha.length() == 60);
        assertTrue(PasswordHasher.verify("maria123", criada.senha));
        assertTrue(dialogos.teve(Dialogos.Tipo.SUCESSO), "A confirmacao so aparece apos o cadastro.");
        assertEquals("maria", view.txtUsuario.getText());
        assertEquals("", view.erroCadastro());
    }

    @Test
    void cadastroNuncaCriaAdmin() {
        cadastrar("espertinho", "senha123", "senha123");

        assertEquals("ESTUDANTE", db.porUsername("espertinho").perfil);
        assertEquals(1, db.rows.stream().filter(r -> r.perfil.equals("ADMIN")).count());
    }

    @Test
    void cadastroComSenhasDiferentesNaoGravaENaoConfirma() {
        int antes = db.rows.size();

        cadastrar("maria", "maria123", "outra123");

        assertEquals("As senhas não coincidem.", view.erroCadastro());
        assertEquals(antes, db.rows.size());
        assertFalse(dialogos.teve(Dialogos.Tipo.SUCESSO));
    }

    @Test
    void cadastroValidaUsuarioESenhaVazios() {
        cadastrar("", "abc123", "abc123");
        assertEquals("Informe um nome de usuário.", view.erroCadastro());

        cadastrar("maria", "", "");
        assertEquals("Informe uma senha.", view.erroCadastro());
        assertEquals(3, db.rows.size());
    }

    @Test
    void cadastroMostraErroDasRegrasDoDaoSemConfirmar() {
        cadastrar("maria", "123", "123");
        assertTrue(view.erroCadastro().contains("mínimo"));

        cadastrar("ADMIN", "senha123", "senha123");
        assertTrue(view.erroCadastro().contains("Já existe"));

        assertEquals(3, db.rows.size());
        assertFalse(dialogos.teve(Dialogos.Tipo.SUCESSO));
    }

    @Test
    void voltarAoLoginLimpaOsErros() {
        cadastrar("", "", "");
        assertFalse(view.erroCadastro().isEmpty());

        view.btnVoltar.doClick(0);

        assertEquals("", view.erroCadastro());
        assertEquals(view.btnEntrar, view.getRootPane().getDefaultButton());
    }

    @Test
    void linkCriarContaAbreOCadastro() {
        view.btnIrCadastro.doClick(0);

        assertEquals(view.btnCriar, view.getRootPane().getDefaultButton());
    }
}
