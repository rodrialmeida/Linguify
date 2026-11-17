package VIEW;

import DAO.UsuarioDAO;
import DTO.UsuarioDTO;
import auth.OperacaoUsuarioException;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import ui.Avatar;
import ui.BotaoModerno;
import ui.CardPanel;
import ui.Dialogos;
import ui.Icones;
import ui.Tarefa;
import ui.Tema;
import ui.Ui;

/**
 * Secao "Usuarios" (somente ADMIN): lista com ID, username e perfil (nunca senha
 * ou hash) e cadastro, edicao e exclusao. As regras ficam no {@link UsuarioDAO}.
 */
public class UsuariosPanel extends JPanel implements Secao {

    private final SessaoUsuario sessao;
    private final UsuarioDAO dao;
    private final Runnable aoNegarAcesso;

    final BotaoModerno btnNovo = Ui.botao("Novo usuário", Icones.Tipo.MAIS, BotaoModerno.Variante.PRIMARIO);
    final BotaoModerno btnEditar = Ui.botao("Editar", Icones.Tipo.EDITAR, BotaoModerno.Variante.SECUNDARIO);
    final BotaoModerno btnExcluir = Ui.botao("Excluir", Icones.Tipo.LIXEIRA, BotaoModerno.Variante.PERIGO);
    private final BotaoModerno btnTentar = Ui.botao("Tentar novamente", null, BotaoModerno.Variante.SECUNDARIO);
    final DefaultTableModel modelo = Ui.modeloSomenteLeitura("ID", "Usuário", "Perfil");
    final JTable tabela = Ui.tabela(modelo);
    private final JLabel lblContagem = Ui.rotulo("", java.awt.Font.BOLD, 13f, Tema.CINZA);
    private final JLabel lblVazio = Ui.rotulo("", java.awt.Font.PLAIN, 14f, Tema.CINZA);

    private final CardLayout cartoesTabela = new CardLayout();
    private final JPanel areaTabela = new JPanel(cartoesTabela);
    private final CardLayout cartoesDetalhe = new CardLayout();
    private final JPanel areaDetalhe = new JPanel(cartoesDetalhe);
    private final JLabel lblNome = Ui.rotulo("", java.awt.Font.BOLD, 20f, Tema.NAVY);
    private final JPanel chipPerfil = Ui.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel avatarArea = Ui.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel lblObs = Ui.rotulo("", java.awt.Font.PLAIN, 13f, Tema.CINZA);

    private List<UsuarioDTO> usuarios = new ArrayList<>();

    /**
     * @throws AcessoNegadoException se a sessao nao for ADMIN: a secao nem e criada
     */
    public UsuariosPanel(SessaoUsuario sessao, UsuarioDAO dao, Runnable aoNegarAcesso)
            throws AcessoNegadoException {
        super(new BorderLayout(0, 18));
        SessaoUsuario.exigirAdmin(sessao);
        this.sessao = sessao;
        this.dao = dao;
        this.aoNegarAcesso = aoNegarAcesso;
        setOpaque(false);
        montar();
        configurarAcoes();
    }

    private void montar() {
        JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
        cabecalho.setOpaque(false);
        JPanel textos = Ui.pilha();
        Ui.empilhar(textos, Ui.titulo("Usuários"), 0);
        Ui.empilhar(textos, Ui.subtitulo("Gerencie quem acessa o Linguify e com qual perfil."), 4);
        cabecalho.add(textos, BorderLayout.CENTER);
        JPanel acoes = Ui.transparente(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acoes.add(lblContagem);
        acoes.add(btnNovo);
        cabecalho.add(acoes, BorderLayout.EAST);
        add(cabecalho, BorderLayout.NORTH);

        tabela.getColumnModel().getColumn(0).setPreferredWidth(60);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(240);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(140);

        JPanel vazio = new JPanel(new GridBagLayout());
        vazio.setOpaque(false);
        JPanel vazioPilha = Ui.pilha();
        lblVazio.setHorizontalAlignment(JLabel.CENTER);
        Ui.empilhar(vazioPilha, lblVazio, 0);
        btnTentar.setVisible(false);
        JPanel tentar = Ui.transparente(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tentar.add(btnTentar);
        Ui.empilhar(vazioPilha, tentar, 12);
        vazio.add(vazioPilha, new GridBagConstraints());

        areaTabela.setOpaque(false);
        areaTabela.add(Ui.rolagem(tabela), "tabela");
        areaTabela.add(vazio, "vazio");
        CardPanel cartaoTabela = new CardPanel(new BorderLayout(), 20, 8);
        cartaoTabela.add(areaTabela, BorderLayout.CENTER);

        areaDetalhe.setOpaque(false);
        areaDetalhe.add(painelDetalheVazio(), "vazio");
        areaDetalhe.add(painelDetalhe(), "detalhe");
        CardPanel cartaoDetalhe = new CardPanel(new BorderLayout(), 20, 22);
        cartaoDetalhe.setPreferredSize(new Dimension(340, 100));
        cartaoDetalhe.add(areaDetalhe, BorderLayout.CENTER);

        JPanel corpo = new JPanel(new BorderLayout(18, 0));
        corpo.setOpaque(false);
        corpo.add(cartaoTabela, BorderLayout.CENTER);
        corpo.add(cartaoDetalhe, BorderLayout.EAST);
        add(corpo, BorderLayout.CENTER);
        mostrarDetalhe(null);
    }

    private JPanel painelDetalheVazio() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JPanel pilha = Ui.pilha();
        JPanel selo = Ui.transparente(new FlowLayout(FlowLayout.CENTER, 0, 0));
        selo.add(Ui.seloIcone(Icones.Tipo.USUARIOS, Tema.ROXO_SUAVE, Tema.ROXO, 64));
        Ui.empilhar(pilha, selo, 0);
        JLabel msg = Ui.rotulo("<html><div style='text-align:center;width:220px'>Selecione um usuário na lista "
                + "para editar ou excluir.</div></html>", java.awt.Font.PLAIN, 14f, Tema.CINZA);
        msg.setHorizontalAlignment(JLabel.CENTER);
        Ui.empilhar(pilha, msg, 14);
        p.add(pilha, new GridBagConstraints());
        return p;
    }

    private JPanel painelDetalhe() {
        JPanel p = Ui.pilha();
        Ui.empilhar(p, avatarArea, 0);
        Ui.empilhar(p, lblNome, 14);
        Ui.empilhar(p, chipPerfil, 8);
        Ui.empilhar(p, lblObs, 14);
        Ui.empilhar(p, Ui.linha(10, btnEditar, btnExcluir), 22);
        Ui.finalizarPilha(p);
        return p;
    }

    private void configurarAcoes() {
        btnNovo.addActionListener(e -> novoUsuario());
        btnEditar.addActionListener(e -> editarSelecionado());
        btnExcluir.addActionListener(e -> excluirSelecionado());
        btnTentar.addActionListener(e -> recarregar());
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalhe(selecionado());
            }
        });
    }

    // ------------------------------------------------------------------

    @Override
    public void aoExibir() {
        recarregar();
    }

    void recarregar() {
        Integer idSelecionado = selecionado() == null ? null : selecionado().getId_usuario();
        Tarefa.executar(() -> dao.listarUsuarios(sessao), lista -> {
            usuarios = new ArrayList<>(lista);
            modelo.setRowCount(0);
            for (UsuarioDTO u : usuarios) {
                // Somente ID, username e perfil: a senha nunca chega a esta tela.
                modelo.addRow(new Object[] {u.getId_usuario(), u.getUsername(), u.getPerfil()});
            }
            lblContagem.setText(usuarios.size() == 1 ? "1 usuário" : usuarios.size() + " usuários");
            btnTentar.setVisible(false);
            if (usuarios.isEmpty()) {
                lblVazio.setText("Nenhum usuário cadastrado.");
                cartoesTabela.show(areaTabela, "vazio");
            } else {
                cartoesTabela.show(areaTabela, "tabela");
            }
            tabela.clearSelection();
            if (idSelecionado != null) {
                for (int i = 0; i < usuarios.size(); i++) {
                    if (usuarios.get(i).getId_usuario() == idSelecionado) {
                        tabela.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
            mostrarDetalhe(selecionado());
        }, erro -> {
            if (erro instanceof AcessoNegadoException) {
                negarAcesso((AcessoNegadoException) erro);
                return;
            }
            usuarios = new ArrayList<>();
            modelo.setRowCount(0);
            lblContagem.setText("");
            lblVazio.setText(erro instanceof OperacaoUsuarioException
                    ? erro.getMessage() : "Não foi possível carregar os usuários.");
            btnTentar.setVisible(true);
            cartoesTabela.show(areaTabela, "vazio");
            mostrarDetalhe(null);
        });
    }

    /** Usuario selecionado na tabela, ou {@code null}. */
    UsuarioDTO selecionado() {
        int linha = tabela.getSelectedRow();
        return linha >= 0 && linha < usuarios.size() ? usuarios.get(linha) : null;
    }

    private boolean eContaAtual(UsuarioDTO u) {
        return u.getId_usuario() == sessao.getUsuario().getId_usuario();
    }

    private void mostrarDetalhe(UsuarioDTO u) {
        if (u == null) {
            cartoesDetalhe.show(areaDetalhe, "vazio");
            return;
        }
        boolean admin = SessaoUsuario.PERFIL_ADMIN.equalsIgnoreCase(u.getPerfil());
        avatarArea.removeAll();
        avatarArea.add(new Avatar(u.getUsername(), 64, admin ? Tema.ROXO : Tema.NAVY_CLARO));
        lblNome.setText(u.getUsername());
        chipPerfil.removeAll();
        chipPerfil.add(Ui.chip(u.getPerfil(), admin ? Tema.ROXO_SUAVE : new java.awt.Color(0xE8EAF5),
                admin ? Tema.ROXO_ESCURO : Tema.NAVY));
        boolean propria = eContaAtual(u);
        lblObs.setText(propria ? "<html><body style='width:260px'>Esta é a sua conta. Ela não pode ser excluída.</body></html>"
                : " ");
        btnExcluir.setEnabled(!propria);
        btnExcluir.setToolTipText(propria ? "Você não pode excluir a própria conta." : null);
        avatarArea.revalidate();
        cartoesDetalhe.show(areaDetalhe, "detalhe");
    }

    // ------------------------------------------------------------------

    /** Abre o formulario de novo usuario. */
    public void novoUsuario() {
        abrirFormulario(null);
    }

    void editarSelecionado() {
        UsuarioDTO u = selecionado();
        if (u == null) {
            Dialogos.aviso(this, "Selecione um usuário na lista para editar.");
            return;
        }
        UsuarioDTO copia = new UsuarioDTO();
        copia.setId_usuario(u.getId_usuario());
        copia.setUsername(u.getUsername());
        copia.setPerfil(u.getPerfil());
        abrirFormulario(copia);
    }

    void excluirSelecionado() {
        UsuarioDTO u = selecionado();
        if (u == null) {
            Dialogos.aviso(this, "Selecione um usuário na lista para excluir.");
            return;
        }
        boolean confirmou = Dialogos.confirmar(this, "Excluir usuário",
                "Excluir o usuário \"" + u.getUsername() + "\"? Esta ação não pode ser desfeita.", "Excluir", true);
        if (!confirmou) {
            return;
        }
        try {
            dao.excluirUsuario(sessao, u.getId_usuario());
        } catch (AcessoNegadoException e) {
            negarAcesso(e);
            return;
        } catch (OperacaoUsuarioException e) {
            // Inclui os bloqueios: propria conta e ultimo administrador.
            Dialogos.erro(this, e.getMessage());
            return;
        }
        Dialogos.sucesso(this, "Usuário excluído com sucesso.");
        recarregar();
    }

    private void abrirFormulario(UsuarioDTO usuario) {
        UsuarioFormDialog dialogo = new UsuarioFormDialog(SwingUtilities.getWindowAncestor(this), sessao, dao,
                usuario, this::recarregar, this::negarAcesso);
        dialogo.setVisible(true);
    }

    private void negarAcesso(AcessoNegadoException e) {
        Dialogos.aviso(this, e.getMessage());
        aoNegarAcesso.run();
    }
}
