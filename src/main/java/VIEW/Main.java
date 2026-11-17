package VIEW;

import DAO.MateriasDAO;
import DAO.UsuarioDAO;
import DTO.UsuarioDTO;
import auth.SessaoUsuario;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import ui.Dialogos;
import ui.Tema;

/** Ponto de entrada do Linguify: instala o tema, abre o login e liga login e janela principal. */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, erro) -> {
            LOG.log(Level.SEVERE, "Erro nao tratado", erro);
            SwingUtilities.invokeLater(() ->
                    Dialogos.erro(null, "Ocorreu um erro inesperado. Tente repetir a ação."));
        });
        SwingUtilities.invokeLater(() -> {
            Tema.instalar();
            mostrarLogin();
        });
    }

    /** Abre a tela de login (inicio da aplicacao e depois de "Sair da conta"). */
    static void mostrarLogin() {
        new LoginView(new UsuarioDAO(), Main::abrirPrincipal).setVisible(true);
    }

    private static void abrirPrincipal(UsuarioDTO usuario) {
        new JanelaPrincipal(new SessaoUsuario(usuario), new UsuarioDAO(), new MateriasDAO(), Main::mostrarLogin)
                .setVisible(true);
    }
}
