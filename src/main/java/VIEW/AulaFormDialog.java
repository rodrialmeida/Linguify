package VIEW;

import DAO.MateriasDAO;
import DTO.MateriasDTO;
import auth.OperacaoUsuarioException;
import auth.RegrasAula;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import ui.BotaoModerno;
import ui.Dialogos;
import ui.Tema;
import ui.Ui;

/** Formulario (modal) para cadastrar ou editar uma aula. */
public class AulaFormDialog extends JDialog {

    private static final String[] IDIOMAS_PADRAO = {"Inglês", "Espanhol", "Francês", "Alemão"};

    private final SessaoUsuario sessao;
    private final MateriasDAO dao;
    private final MateriasDTO original;               // null = nova aula
    private final Consumer<MateriasDTO> aoSalvar;
    private final Consumer<AcessoNegadoException> aoNegarAcesso;

    final JComboBox<String> cmbIdioma;
    final JTextField txtTema = Ui.campo("Ex.: Greetings", null);
    final JTextField txtProfessor = Ui.campo("Nome do professor", null);
    final JTextField txtData = Ui.campo("dd/MM/aaaa", null);
    final JTextArea txtConteudo = Ui.areaTexto(5);
    private final JLabel lblContador = Ui.rotulo("", java.awt.Font.PLAIN, 12f, Tema.CINZA);
    private final JLabel lblErro = Ui.rotulo(" ", java.awt.Font.PLAIN, 12.5f, Tema.ERRO);
    final BotaoModerno btnSalvar = Ui.botao("Salvar", null, BotaoModerno.Variante.PRIMARIO);
    final BotaoModerno btnCancelar = Ui.botao("Cancelar", null, BotaoModerno.Variante.SECUNDARIO);
    private String ultimoErro = "";

    /**
     * @param inicial aula a editar, ou {@code null} para cadastrar uma nova
     * @param idiomasConhecidos idiomas ja cadastrados (sugeridos no campo Idioma)
     */
    public AulaFormDialog(Window dono, SessaoUsuario sessao, MateriasDAO dao, MateriasDTO inicial,
            List<String> idiomasConhecidos, Consumer<MateriasDTO> aoSalvar,
            Consumer<AcessoNegadoException> aoNegarAcesso) {
        super(dono, inicial == null ? "Nova aula" : "Editar aula", Dialog.ModalityType.APPLICATION_MODAL);
        this.sessao = sessao;
        this.dao = dao;
        this.original = inicial;
        this.aoSalvar = aoSalvar;
        this.aoNegarAcesso = aoNegarAcesso;

        Set<String> idiomas = new LinkedHashSet<>(java.util.Arrays.asList(IDIOMAS_PADRAO));
        idiomas.addAll(idiomasConhecidos);
        cmbIdioma = Ui.combo(idiomas.toArray(new String[0]));
        cmbIdioma.setEditable(true);

        montar();
        preencher();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private void montar() {
        JPanel p = Ui.pilhaComLargura(520);
        p.setBorder(Ui.margem(24, 28, 22, 28));
        p.setOpaque(true);
        p.setBackground(Tema.BRANCO);

        Ui.empilhar(p, Ui.secao(original == null ? "Nova aula" : "Editar aula"), 0);
        Ui.empilhar(p, Ui.subtitulo("Preencha os dados da aula de idiomas."), 2);

        Ui.empilhar(p, Ui.rotuloCampo("Idioma"), 18);
        Ui.empilhar(p, cmbIdioma, 6);
        Ui.empilhar(p, Ui.rotuloCampo("Nome da aula"), 12);
        Ui.empilhar(p, txtTema, 6);

        JPanel linha = new JPanel(new GridBagLayout());
        linha.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 0.6;
        g.gridx = 0;
        g.insets = new java.awt.Insets(0, 0, 0, 12);
        JPanel colProf = Ui.pilha();
        Ui.empilhar(colProf, Ui.rotuloCampo("Professor"), 0);
        Ui.empilhar(colProf, txtProfessor, 6);
        linha.add(colProf, g);
        g.gridx = 1;
        g.weightx = 0.4;
        g.insets = new java.awt.Insets(0, 0, 0, 0);
        JPanel colData = Ui.pilha();
        Ui.empilhar(colData, Ui.rotuloCampo("Data"), 0);
        Ui.empilhar(colData, txtData, 6);
        linha.add(colData, g);
        Ui.empilhar(p, linha, 12);

        Ui.empilhar(p, Ui.rotuloCampo("Conteúdo"), 12);
        JScrollPane moldura = Ui.areaComMoldura(txtConteudo);
        moldura.setPreferredSize(new java.awt.Dimension(100, 120));
        Ui.empilhar(p, moldura, 6);
        Ui.empilhar(p, lblContador, 4);
        Ui.empilhar(p, lblErro, 8);

        Ui.empilhar(p, Ui.linhaADireita(10, btnCancelar, btnSalvar), 8);
        Ui.finalizarPilha(p);

        setContentPane(p);
        getRootPane().setDefaultButton(btnSalvar);

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());
        txtConteudo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                atualizarContador();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                atualizarContador();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                atualizarContador();
            }
        });
    }

    private void preencher() {
        if (original == null) {
            cmbIdioma.setSelectedItem("Inglês");
            txtData.setText(RegrasAula.hoje());
        } else {
            cmbIdioma.setSelectedItem(original.getMateria());
            txtTema.setText(original.getTema());
            txtProfessor.setText(original.getProfessor());
            txtData.setText(original.getData());
            txtConteudo.setText(original.getConteudo());
            txtConteudo.setCaretPosition(0);
        }
        atualizarContador();
    }

    private void atualizarContador() {
        int n = txtConteudo.getText().codePointCount(0, txtConteudo.getText().length());
        lblContador.setText(n + " / " + RegrasAula.CONTEUDO_MAX + " caracteres");
        lblContador.setForeground(n > RegrasAula.CONTEUDO_MAX ? Tema.ERRO : Tema.CINZA);
    }

    private String idiomaDigitado() {
        Object item = cmbIdioma.getEditor().getItem();
        return item == null ? "" : item.toString();
    }

    /** Valida e grava; em caso de problema mostra a mensagem no proprio formulario. */
    void salvar() {
        MateriasDTO dto = new MateriasDTO();
        dto.setId_materias(original == null ? 0 : original.getId_materias());
        dto.setMateria(idiomaDigitado());
        dto.setTema(txtTema.getText());
        dto.setProfessor(txtProfessor.getText());
        dto.setData(txtData.getText());
        dto.setConteudo(txtConteudo.getText());

        try {
            if (original == null) {
                dao.cadastrarAula(sessao, dto);
            } else {
                dao.atualizarAula(sessao, dto);
            }
        } catch (AcessoNegadoException e) {
            dispose();
            aoNegarAcesso.accept(e);
            return;
        } catch (OperacaoUsuarioException e) {
            definirErro(e.getMessage());
            return;
        }

        MateriasDTO salva = new MateriasDTO();
        salva.setMateria(dto.getMateria().trim());
        salva.setTema(dto.getTema().trim());
        dispose();
        Dialogos.sucesso(getOwner(), original == null
                ? "Aula cadastrada com sucesso." : "Aula atualizada com sucesso.");
        aoSalvar.accept(salva);
    }

    private void definirErro(String mensagem) {
        ultimoErro = mensagem;
        lblErro.setText("<html><body style='width:460px'>" + Ui.html(mensagem) + "</body></html>");
    }

    /** Ultima mensagem de erro mostrada no formulario (vazia se nao houver). */
    String erro() {
        return ultimoErro;
    }
}
