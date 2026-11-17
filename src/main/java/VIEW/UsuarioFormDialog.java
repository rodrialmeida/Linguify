package VIEW;

import DAO.UsuarioDAO;
import DTO.UsuarioDTO;
import auth.OperacaoUsuarioException;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import ui.BotaoModerno;
import ui.Dialogos;
import ui.Icones;
import ui.Tema;
import ui.Ui;

/** Formulario (modal) para cadastrar ou editar um usuario. A senha nunca e exibida. */
public class UsuarioFormDialog extends JDialog {

    private final SessaoUsuario sessao;
    private final UsuarioDAO dao;
    private final UsuarioDTO original;            // null = novo usuario
    private final Runnable aoSalvar;
    private final Consumer<AcessoNegadoException> aoNegarAcesso;

    final JTextField txtUsername = Ui.campo("Nome de usuário", Icones.Tipo.USUARIO);
    final JComboBox<String> cmbPerfil =
            Ui.combo(new String[] {SessaoUsuario.PERFIL_ESTUDANTE, SessaoUsuario.PERFIL_ADMIN});
    final JPasswordField txtSenha;
    private final JLabel lblErro = Ui.rotulo(" ", java.awt.Font.PLAIN, 12.5f, Tema.ERRO);
    final BotaoModerno btnSalvar = Ui.botao("Salvar", null, BotaoModerno.Variante.PRIMARIO);
    final BotaoModerno btnCancelar = Ui.botao("Cancelar", null, BotaoModerno.Variante.SECUNDARIO);
    private String ultimoErro = "";

    /** @param inicial usuario a editar, ou {@code null} para cadastrar um novo */
    public UsuarioFormDialog(Window dono, SessaoUsuario sessao, UsuarioDAO dao, UsuarioDTO inicial,
            Runnable aoSalvar, Consumer<AcessoNegadoException> aoNegarAcesso) {
        super(dono, inicial == null ? "Novo usuário" : "Editar usuário", Dialog.ModalityType.APPLICATION_MODAL);
        this.sessao = sessao;
        this.dao = dao;
        this.original = inicial;
        this.aoSalvar = aoSalvar;
        this.aoNegarAcesso = aoNegarAcesso;
        this.txtSenha = Ui.campoSenha(inicial == null
                ? "Mínimo de 6 caracteres" : "Deixe em branco para manter a senha atual");

        montar();
        if (inicial != null) {
            txtUsername.setText(inicial.getUsername());
            cmbPerfil.setSelectedItem(SessaoUsuario.PERFIL_ADMIN.equalsIgnoreCase(inicial.getPerfil())
                    ? SessaoUsuario.PERFIL_ADMIN : SessaoUsuario.PERFIL_ESTUDANTE);
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private void montar() {
        JPanel p = Ui.pilhaComLargura(420);
        p.setBorder(Ui.margem(24, 28, 22, 28));
        p.setOpaque(true);
        p.setBackground(Tema.BRANCO);

        Ui.empilhar(p, Ui.secao(original == null ? "Novo usuário" : "Editar usuário"), 0);
        Ui.empilhar(p, Ui.subtitulo(original == null
                ? "Crie uma conta e escolha o perfil de acesso."
                : "Altere o nome, o perfil ou a senha desta conta."), 2);
        Ui.empilhar(p, Ui.rotuloCampo("Usuário"), 18);
        Ui.empilhar(p, txtUsername, 6);
        Ui.empilhar(p, Ui.rotuloCampo("Perfil"), 12);
        Ui.empilhar(p, cmbPerfil, 6);
        Ui.empilhar(p, Ui.rotuloCampo(original == null ? "Senha" : "Nova senha (opcional)"), 12);
        Ui.empilhar(p, txtSenha, 6);
        Ui.empilhar(p, lblErro, 10);

        Ui.empilhar(p, Ui.linhaADireita(10, btnCancelar, btnSalvar), 8);
        Ui.finalizarPilha(p);

        setContentPane(p);
        getRootPane().setDefaultButton(btnSalvar);
        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());
    }

    /** Valida e grava pelo DAO; problemas aparecem no proprio formulario. */
    void salvar() {
        char[] senhaChars = txtSenha.getPassword();
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId_usuario(original == null ? 0 : original.getId_usuario());
        dto.setUsername(txtUsername.getText());
        dto.setPerfil((String) cmbPerfil.getSelectedItem());
        dto.setSenha(new String(senhaChars));
        Arrays.fill(senhaChars, '\0');

        try {
            if (original == null) {
                dao.cadastrarUsuario(sessao, dto);
            } else {
                dao.atualizarUsuario(sessao, dto);
            }
        } catch (AcessoNegadoException e) {
            dispose();
            aoNegarAcesso.accept(e);
            return;
        } catch (OperacaoUsuarioException e) {
            ultimoErro = e.getMessage();
            lblErro.setText("<html><body style='width:360px'>" + Ui.html(e.getMessage()) + "</body></html>");
            return;
        }
        dispose();
        Dialogos.sucesso(getOwner(), original == null
                ? "Usuário cadastrado com sucesso." : "Usuário atualizado com sucesso.");
        aoSalvar.run();
    }

    /** Ultima mensagem de erro mostrada no formulario (vazia se nao houver). */
    String erro() {
        return ultimoErro;
    }
}
