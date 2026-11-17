package VIEW;

import DAO.MateriasDAO;
import DTO.MateriasDTO;
import auth.OperacaoUsuarioException;
import auth.SessaoUsuario;
import auth.SessaoUsuario.AcessoNegadoException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import ui.BotaoModerno;
import ui.CardPanel;
import ui.Dialogos;
import ui.Icones;
import ui.Tarefa;
import ui.Tema;
import ui.Ui;

/**
 * Secao "Aulas e matérias": lista, busca por nome/idioma, filtro por idioma e
 * detalhes da aula. ADMIN tambem cadastra, edita e exclui.
 */
public class AulasPanel extends JPanel implements Secao {

    static final String TODOS = "Todos os idiomas";

    private final SessaoUsuario sessao;
    private final MateriasDAO dao;
    private final Runnable aoNegarAcesso;
    private final boolean admin;

    final JTextField txtBusca = Ui.campo("Buscar por aula ou idioma", Icones.Tipo.BUSCA);
    final JComboBox<String> cmbIdioma = Ui.combo(new String[] {TODOS});
    final BotaoModerno btnNova = Ui.botao("Nova aula", Icones.Tipo.MAIS, BotaoModerno.Variante.PRIMARIO);
    final BotaoModerno btnEditar = Ui.botao("Editar", Icones.Tipo.EDITAR, BotaoModerno.Variante.SECUNDARIO);
    final BotaoModerno btnExcluir = Ui.botao("Excluir", Icones.Tipo.LIXEIRA, BotaoModerno.Variante.PERIGO);
    private final BotaoModerno btnTentar = Ui.botao("Tentar novamente", null, BotaoModerno.Variante.SECUNDARIO);
    final DefaultTableModel modelo = Ui.modeloSomenteLeitura("Idioma", "Aula", "Professor", "Data");
    final JTable tabela = Ui.tabela(modelo);
    private final JLabel lblContagem = Ui.rotulo("", java.awt.Font.BOLD, 13f, Tema.CINZA);
    private final JLabel lblVazio = Ui.rotulo("", java.awt.Font.PLAIN, 14f, Tema.CINZA);

    private final CardLayout cartoesTabela = new CardLayout();
    private final JPanel areaTabela = new JPanel(cartoesTabela);
    private final CardLayout cartoesDetalhe = new CardLayout();
    private final JPanel areaDetalhe = new JPanel(cartoesDetalhe);
    private final JPanel chipIdioma = Ui.transparente(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel lblTitulo = Ui.rotulo("", java.awt.Font.BOLD, 20f, Tema.NAVY);
    private final JLabel lblMeta = Ui.rotulo("", java.awt.Font.PLAIN, 13f, Tema.CINZA);
    private final JTextArea txtConteudo = Ui.areaTexto(6);

    private List<MateriasDTO> todas = new ArrayList<>();
    private List<MateriasDTO> visiveis = new ArrayList<>();
    private boolean carregado;
    private boolean atualizandoCombo;
    private boolean temIdiomaPendente;
    private String idiomaPendente;

    public AulasPanel(SessaoUsuario sessao, MateriasDAO dao, Runnable aoNegarAcesso) {
        super(new BorderLayout(0, 18));
        this.sessao = sessao;
        this.dao = dao;
        this.aoNegarAcesso = aoNegarAcesso;
        this.admin = sessao.isAdmin();
        setOpaque(false);
        montar();
        configurarAcoes();
    }

    // ------------------------------------------------------------------
    // Interface
    // ------------------------------------------------------------------

    private void montar() {
        // Cabecalho
        JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
        cabecalho.setOpaque(false);
        JPanel textos = Ui.pilha();
        Ui.empilhar(textos, Ui.titulo("Aulas e matérias"), 0);
        Ui.empilhar(textos, Ui.subtitulo(admin
                ? "Consulte, cadastre e organize as aulas de cada idioma."
                : "Pesquise por idioma ou nome da aula e abra os detalhes."), 4);
        cabecalho.add(textos, BorderLayout.CENTER);
        JPanel acoes = Ui.transparente(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acoes.add(btnNova);
        btnNova.setVisible(admin);
        cabecalho.add(acoes, BorderLayout.EAST);

        // Barra de filtros
        CardPanel filtros = new CardPanel(new BorderLayout(14, 0), 18, 14);
        cmbIdioma.setPreferredSize(new Dimension(220, 42));
        filtros.add(txtBusca, BorderLayout.CENTER);
        JPanel direita = Ui.transparente(new BorderLayout(14, 0));
        direita.add(cmbIdioma, BorderLayout.WEST);
        direita.add(lblContagem, BorderLayout.EAST);
        filtros.add(direita, BorderLayout.EAST);

        JPanel topo = Ui.pilha();
        Ui.empilhar(topo, cabecalho, 0);
        Ui.empilhar(topo, filtros, 18);
        add(topo, BorderLayout.NORTH);

        // Tabela
        tabela.getColumnModel().getColumn(0).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(190);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(112);
        tabela.getColumnModel().getColumn(3).setMinWidth(112);

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

        // Detalhes
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
        selo.add(Ui.seloIcone(Icones.Tipo.LIVRO, Tema.ROXO_SUAVE, Tema.ROXO, 64));
        Ui.empilhar(pilha, selo, 0);
        JLabel msg = Ui.rotulo("<html><div style='text-align:center;width:220px'>Selecione uma aula na lista "
                + "para ver os detalhes.</div></html>", java.awt.Font.PLAIN, 14f, Tema.CINZA);
        msg.setHorizontalAlignment(JLabel.CENTER);
        Ui.empilhar(pilha, msg, 14);
        p.add(pilha, new GridBagConstraints());
        return p;
    }

    private JPanel painelDetalhe() {
        txtConteudo.setEditable(false);
        txtConteudo.setOpaque(false);
        txtConteudo.setBorder(Ui.margem(0, 0, 0, 0));
        txtConteudo.setFocusable(false);

        JPanel p = Ui.pilha();
        Ui.empilhar(p, chipIdioma, 0);
        Ui.empilhar(p, lblTitulo, 12);
        Ui.empilhar(p, lblMeta, 6);
        Ui.empilhar(p, Ui.rotuloCampo("CONTEÚDO"), 22);
        Ui.empilhar(p, txtConteudo, 8);

        JPanel botoes = Ui.linha(10, btnEditar, btnExcluir);
        btnEditar.setVisible(admin);
        btnExcluir.setVisible(admin);
        Ui.empilhar(p, botoes, 22);
        Ui.finalizarPilha(p);
        return p;
    }

    private void configurarAcoes() {
        btnNova.addActionListener(e -> novaAula());
        btnEditar.addActionListener(e -> editarSelecionada());
        btnExcluir.addActionListener(e -> excluirSelecionada());
        btnTentar.addActionListener(e -> recarregar(null, null));
        cmbIdioma.addActionListener(e -> {
            if (!atualizandoCombo) {
                aplicarFiltro();
            }
        });
        txtBusca.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltro();
            }
        });
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalhe(selecionada());
            }
        });
    }

    // ------------------------------------------------------------------
    // Dados e filtros
    // ------------------------------------------------------------------

    @Override
    public void aoExibir() {
        recarregar(null, null);
    }

    /** Seleciona um idioma no filtro ({@code null} = todos). Aplicado apos o carregamento, se necessario. */
    public void filtrarPorIdioma(String idioma) {
        temIdiomaPendente = true;
        idiomaPendente = idioma;
        if (carregado) {
            aplicarIdiomaPendente();
            aplicarFiltro();
        }
    }

    private void aplicarIdiomaPendente() {
        if (!temIdiomaPendente) {
            return;
        }
        temIdiomaPendente = false;
        atualizandoCombo = true;
        try {
            String alvo = idiomaPendente == null ? TODOS : idiomaPendente;
            boolean achou = false;
            for (int i = 0; i < cmbIdioma.getItemCount(); i++) {
                if (cmbIdioma.getItemAt(i).equalsIgnoreCase(alvo)) {
                    cmbIdioma.setSelectedIndex(i);
                    achou = true;
                    break;
                }
            }
            if (!achou) {
                cmbIdioma.setSelectedItem(TODOS);
            }
        } finally {
            atualizandoCombo = false;
        }
        txtBusca.setText("");
    }

    /**
     * Recarrega as aulas do banco.
     *
     * @param idiomaSelecionar idioma da aula a selecionar depois de carregar (opcional)
     * @param temaSelecionar   nome da aula a selecionar depois de carregar (opcional)
     */
    void recarregar(String idiomaSelecionar, String temaSelecionar) {
        Tarefa.executar(() -> dao.listarAulas(sessao), lista -> {
            todas = new ArrayList<>(lista);
            carregado = true;
            btnTentar.setVisible(false);
            recriarOpcoesDeIdioma();
            aplicarIdiomaPendente();
            aplicarFiltro();
            if (idiomaSelecionar != null && temaSelecionar != null) {
                selecionar(idiomaSelecionar, temaSelecionar);
            }
        }, erro -> {
            if (erro instanceof AcessoNegadoException) {
                negarAcesso((AcessoNegadoException) erro);
                return;
            }
            todas = new ArrayList<>();
            visiveis = new ArrayList<>();
            modelo.setRowCount(0);
            lblContagem.setText("");
            lblVazio.setText(erro instanceof OperacaoUsuarioException
                    ? erro.getMessage() : "Não foi possível carregar as aulas.");
            btnTentar.setVisible(true);
            cartoesTabela.show(areaTabela, "vazio");
            mostrarDetalhe(null);
        });
    }

    private void recriarOpcoesDeIdioma() {
        Object anterior = cmbIdioma.getSelectedItem();
        Set<String> idiomas = new LinkedHashSet<>();
        for (MateriasDTO a : todas) {
            if (!a.getMateria().isBlank()) {
                idiomas.add(a.getMateria());
            }
        }
        atualizandoCombo = true;
        try {
            cmbIdioma.removeAllItems();
            cmbIdioma.addItem(TODOS);
            for (String i : idiomas) {
                cmbIdioma.addItem(i);
            }
            cmbIdioma.setSelectedItem(anterior != null && idiomas.contains(anterior.toString())
                    ? anterior.toString() : TODOS);
        } finally {
            atualizandoCombo = false;
        }
    }

    private void aplicarFiltro() {
        if (atualizandoCombo) {
            return;
        }
        MateriasDTO anterior = selecionada();
        String termo = normalizar(txtBusca.getText().trim());
        Object escolhido = cmbIdioma.getSelectedItem();
        boolean todosIdiomas = escolhido == null || TODOS.equals(escolhido.toString());

        visiveis = new ArrayList<>();
        for (MateriasDTO a : todas) {
            boolean idiomaOk = todosIdiomas || a.getMateria().equalsIgnoreCase(escolhido.toString());
            boolean termoOk = termo.isEmpty()
                    || normalizar(a.getMateria()).contains(termo) || normalizar(a.getTema()).contains(termo);
            if (idiomaOk && termoOk) {
                visiveis.add(a);
            }
        }

        modelo.setRowCount(0);
        for (MateriasDTO a : visiveis) {
            modelo.addRow(new Object[] {a.getMateria(), a.getTema(), a.getProfessor(), a.getData()});
        }
        lblContagem.setText(visiveis.size() == 1 ? "1 aula" : visiveis.size() + " aulas");

        if (visiveis.isEmpty()) {
            lblVazio.setText(todas.isEmpty()
                    ? (admin ? "Ainda não há aulas cadastradas. Use \"Nova aula\" para começar."
                            : "Ainda não há aulas cadastradas.")
                    : "Nenhuma aula encontrada para esse filtro.");
            cartoesTabela.show(areaTabela, "vazio");
        } else {
            cartoesTabela.show(areaTabela, "tabela");
        }

        if (anterior != null) {
            for (int i = 0; i < visiveis.size(); i++) {
                if (visiveis.get(i).getId_materias() == anterior.getId_materias()) {
                    tabela.setRowSelectionInterval(i, i);
                    return;
                }
            }
        }
        tabela.clearSelection();
        mostrarDetalhe(null);
    }

    private void selecionar(String idioma, String tema) {
        for (int i = 0; i < visiveis.size(); i++) {
            MateriasDTO a = visiveis.get(i);
            if (a.getMateria().equalsIgnoreCase(idioma) && a.getTema().equalsIgnoreCase(tema)) {
                tabela.setRowSelectionInterval(i, i);
                tabela.scrollRectToVisible(tabela.getCellRect(i, 0, true));
                return;
            }
        }
    }

    /** Aula atualmente selecionada na tabela, ou {@code null}. */
    MateriasDTO selecionada() {
        int linha = tabela.getSelectedRow();
        return linha >= 0 && linha < visiveis.size() ? visiveis.get(linha) : null;
    }

    /** Aulas atualmente visiveis (depois do filtro), na ordem da tabela. */
    List<MateriasDTO> aulasVisiveis() {
        return new ArrayList<>(visiveis);
    }

    private void mostrarDetalhe(MateriasDTO aula) {
        if (aula == null) {
            cartoesDetalhe.show(areaDetalhe, "vazio");
            return;
        }
        chipIdioma.removeAll();
        chipIdioma.add(Ui.chip(aula.getMateria(), Tema.ROXO_SUAVE, Tema.ROXO_ESCURO));
        lblTitulo.setText("<html><body style='width:280px'>" + Ui.html(aula.getTema()) + "</body></html>");
        lblMeta.setText("<html><body style='width:280px'>Professor: " + Ui.html(valorOuTraco(aula.getProfessor()))
                + "<br>Data: " + Ui.html(valorOuTraco(aula.getData())) + "</body></html>");
        txtConteudo.setText(aula.getConteudo().isBlank() ? "Esta aula ainda não tem conteúdo." : aula.getConteudo());
        txtConteudo.setCaretPosition(0);
        chipIdioma.revalidate();
        cartoesDetalhe.show(areaDetalhe, "detalhe");
    }

    private static String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------
    // Acoes de ADMIN
    // ------------------------------------------------------------------

    /** Abre o formulario de nova aula (somente ADMIN). */
    public void novaAula() {
        if (!admin) {
            return;
        }
        abrirFormulario(null);
    }

    void editarSelecionada() {
        if (!admin) {
            return;
        }
        MateriasDTO aula = selecionada();
        if (aula == null) {
            Dialogos.aviso(this, "Selecione uma aula na lista para editar.");
            return;
        }
        abrirFormulario(aula);
    }

    void excluirSelecionada() {
        MateriasDTO aula = selecionada();
        if (aula == null) {
            Dialogos.aviso(this, "Selecione uma aula na lista para excluir.");
            return;
        }
        boolean confirmou = Dialogos.confirmar(this, "Excluir aula",
                "Excluir a aula \"" + aula.getTema() + "\" (" + aula.getMateria() + ")? "
                + "Esta ação não pode ser desfeita.", "Excluir", true);
        if (!confirmou) {
            return;
        }
        try {
            dao.excluirAula(sessao, aula.getId_materias());
        } catch (AcessoNegadoException e) {
            negarAcesso(e);
            return;
        } catch (OperacaoUsuarioException e) {
            Dialogos.erro(this, e.getMessage());
            return;
        }
        Dialogos.sucesso(this, "Aula excluída com sucesso.");
        recarregar(null, null);
    }

    private void abrirFormulario(MateriasDTO aula) {
        List<String> idiomas = new ArrayList<>();
        for (int i = 1; i < cmbIdioma.getItemCount(); i++) {
            idiomas.add(cmbIdioma.getItemAt(i));
        }
        MateriasDTO copia = null;
        if (aula != null) {
            copia = new MateriasDTO();
            copia.setId_materias(aula.getId_materias());
            copia.setMateria(aula.getMateria());
            copia.setTema(aula.getTema());
            copia.setProfessor(aula.getProfessor());
            copia.setData(aula.getData());
            copia.setConteudo(aula.getConteudo());
        }
        AulaFormDialog dialogo = new AulaFormDialog(SwingUtilities.getWindowAncestor(this), sessao, dao, copia,
                idiomas, salva -> recarregar(salva.getMateria(), salva.getTema()), this::negarAcesso);
        dialogo.setVisible(true);
    }

    private void negarAcesso(AcessoNegadoException e) {
        Dialogos.aviso(this, e.getMessage());
        aoNegarAcesso.run();
    }
}
