package VIEW;

import static org.junit.jupiter.api.Assertions.assertTrue;

import DAO.MateriasDAO;
import DAO.UsuarioDAO;
import DTO.MateriasDTO;
import DTO.UsuarioDTO;
import auth.SessaoUsuario;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testutil.DadosDemo;
import testutil.FakeBanco;
import testutil.UiTeste;
import ui.Dialogos;

/**
 * Gera imagens PNG das principais telas em {@code target/preview} para conferencia visual.
 * Nada e exibido na tela; os dados vem do banco falso.
 */
class PreviewTest extends UiTeste {

    private static final File PASTA = new File("target/preview");

    private FakeBanco db;

    @BeforeEach
    void preparar() {
        PASTA.mkdirs();
        db = DadosDemo.popular(new FakeBanco());
    }

    private SessaoUsuario sessao(int id, String nome, String perfil) {
        UsuarioDTO u = new UsuarioDTO();
        u.setId_usuario(id);
        u.setUsername(nome);
        u.setPerfil(perfil);
        return new SessaoUsuario(u);
    }

    private JanelaPrincipal janela(SessaoUsuario s) {
        JanelaPrincipal j = new JanelaPrincipal(s, new UsuarioDAO(db::connection),
                new MateriasDAO(db::connection), () -> { });
        redimensionar(j, 1240, 780);
        return j;
    }

    private static void redimensionar(java.awt.Window w, int largura, int altura) {
        w.pack();
        w.setSize(largura, altura);
        w.validate();
    }

    private static void salvar(Container conteudo, String nome) throws Exception {
        BufferedImage img = new BufferedImage(conteudo.getWidth(), conteudo.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        conteudo.paint(g);   // paint (e nao printAll): so assim o Swing desenha a selecao das tabelas
        g.dispose();
        File arquivo = new File(PASTA, nome + ".png");
        ImageIO.write(img, "png", arquivo);
        assertTrue(arquivo.length() > 2000, "imagem vazia: " + nome);
    }

    @Test
    void login() throws Exception {
        LoginView v = new LoginView(new UsuarioDAO(db::connection), u -> { });
        redimensionar(v, 1000, 660);
        salvar(v.getContentPane(), "01-login");

        v.txtUsuario.setText("aluno");
        v.txtSenha.setText("senhaerrada");
        v.btnEntrar.doClick(0);
        salvar(v.getContentPane(), "02-login-erro");

        v.mostrarCadastro();
        v.validate();
        salvar(v.getContentPane(), "03-cadastro");
        v.dispose();
    }

    @Test
    void dashboardAdmin() throws Exception {
        JanelaPrincipal j = janela(sessao(1, "admin", "ADMIN"));
        salvar(j.getContentPane(), "10-dashboard-admin");
        j.dispose();
    }

    @Test
    void dashboardEstudante() throws Exception {
        JanelaPrincipal j = janela(sessao(2, "aluno", "ESTUDANTE"));
        salvar(j.getContentPane(), "11-dashboard-estudante");
        j.dispose();
    }

    @Test
    void aulasAdminEEstudante() throws Exception {
        JanelaPrincipal j = janela(sessao(1, "admin", "ADMIN"));
        j.navegar(JanelaPrincipal.SECAO_AULAS);
        j.painelAulas().tabela.setRowSelectionInterval(0, 0);
        j.validate();
        salvar(j.getContentPane(), "20-aulas-admin");

        j.painelAulas().txtBusca.setText("zzz");
        j.validate();
        salvar(j.getContentPane(), "21-aulas-sem-resultado");
        j.dispose();

        JanelaPrincipal e = janela(sessao(2, "aluno", "ESTUDANTE"));
        e.navegar(JanelaPrincipal.SECAO_AULAS);
        e.painelAulas().cmbIdioma.setSelectedItem("Francês");
        e.painelAulas().tabela.setRowSelectionInterval(1, 1);
        e.validate();
        salvar(e.getContentPane(), "22-aulas-estudante-filtrada");
        e.dispose();
    }

    @Test
    void usuarios() throws Exception {
        JanelaPrincipal j = janela(sessao(1, "admin", "ADMIN"));
        j.navegar(JanelaPrincipal.SECAO_USUARIOS);
        j.validate();
        salvar(j.getContentPane(), "30-usuarios");
        j.dispose();
    }

    @Test
    void formularios() throws Exception {
        SessaoUsuario admin = sessao(1, "admin", "ADMIN");
        AulaFormDialog a = new AulaFormDialog(null, admin, new MateriasDAO(db::connection), null,
                List.of(), x -> { }, x -> { });
        a.btnSalvar.doClick(0);
        salvar(a.getContentPane(), "40-form-aula-nova-erro");
        a.dispose();

        FakeBanco.Aula alvo = db.aulaPorTema("Inglês", "Greetings");
        MateriasDTO edicao = new MateriasDTO();
        edicao.setId_materias(alvo.id);
        edicao.setMateria(alvo.materia);
        edicao.setTema(alvo.tema);
        edicao.setProfessor(alvo.professor);
        edicao.setData(alvo.data);
        edicao.setConteudo(alvo.conteudo);
        AulaFormDialog e = new AulaFormDialog(null, admin, new MateriasDAO(db::connection), edicao,
                List.of(), x -> { }, x -> { });
        salvar(e.getContentPane(), "41-form-aula-editar");
        e.dispose();

        UsuarioFormDialog u = new UsuarioFormDialog(null, admin, new UsuarioDAO(db::connection), null,
                () -> { }, x -> { });
        u.txtUsername.setText("novo");
        u.txtSenha.setText("123");
        u.btnSalvar.doClick(0);
        salvar(u.getContentPane(), "42-form-usuario-erro");
        u.dispose();
    }

    @Test
    void dialogosDeMensagemReais() throws Exception {
        Dialogos.usar(null);      // usa os dialogos reais (visual), nao os falsos
        capturarDialogo(() -> Dialogos.sucesso(null, "Aula cadastrada com sucesso."), "50-dialogo-sucesso");
        capturarDialogo(() -> Dialogos.erro(null, "Você não pode excluir a sua própria conta."), "51-dialogo-erro");
        capturarDialogo(() -> Dialogos.confirmar(null, "Excluir aula",
                "Excluir a aula \"Greetings\" (Inglês)? Esta ação não pode ser desfeita.", "Excluir", true),
                "52-dialogo-confirmar");
    }

    /** Abre um dialogo modal real numa thread, fotografa a janela e a fecha. */
    private static void capturarDialogo(Runnable abrir, String nome) throws Exception {
        Thread t = new Thread(() -> javax.swing.SwingUtilities.invokeLater(abrir));
        t.start();
        JDialog dialogo = null;
        for (int i = 0; i < 100 && dialogo == null; i++) {
            Thread.sleep(50);
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isShowing()) {
                    dialogo = d;
                }
            }
        }
        assertTrue(dialogo != null, "o dialogo nao apareceu: " + nome);
        final JDialog alvo = dialogo;
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            try {
                salvar(alvo.getContentPane(), nome);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                alvo.dispose();
            }
        });
        t.join();
    }
}
