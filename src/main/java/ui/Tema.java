package ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.UIManager;

/**
 * Identidade visual do Linguify: cores, fontes e configuracao do FlatLaf.
 * Todas as telas usam estas constantes, para manter um unico estilo.
 */
public final class Tema {

    public static final Color NAVY = new Color(0x191B4B);
    public static final Color NAVY_CLARO = new Color(0x2A2D70);
    public static final Color ROXO = new Color(0x7565E8);
    public static final Color ROXO_ESCURO = new Color(0x5F4FD1);
    public static final Color ROXO_CLARO = new Color(0xA99BFF);
    public static final Color ROXO_SUAVE = new Color(0xECE9FF);
    public static final Color SELECAO = new Color(0xE2DDFF);
    public static final Color FUNDO = new Color(0xF5F6FA);
    public static final Color TEXTO = new Color(0x282B46);
    public static final Color CINZA = new Color(0x82869B);
    public static final Color BRANCO = Color.WHITE;
    public static final Color BORDA = new Color(0xE3E5EF);
    public static final Color ERRO = new Color(0xD64550);
    public static final Color ERRO_ESCURO = new Color(0xB5303B);
    public static final Color SUCESSO = new Color(0x2E9E6B);
    public static final Color AVISO = new Color(0xE0A100);

    private static final String FAMILIA = escolherFamilia();

    private Tema() {
    }

    public static Font fonte(int estilo, float tamanho) {
        return new Font(FAMILIA, estilo, 14).deriveFont(tamanho);
    }

    private static String escolherFamilia() {
        if (GraphicsEnvironment.isHeadless()) {
            return Font.SANS_SERIF;
        }
        String[] preferidas = {"Segoe UI", "Inter", "Roboto", "SF Pro Text", "Helvetica Neue"};
        java.util.Set<String> instaladas = new java.util.HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : preferidas) {
            if (instaladas.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    /** Instala o FlatLaf claro com o roxo do Linguify como cor de destaque. */
    public static void instalar() {
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#7565E8"));
        FlatLightLaf.setup();

        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("CheckBox.arc", 6);
        UIManager.put("ProgressBar.arc", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Component.borderColor", BORDA);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Panel.background", FUNDO);
        UIManager.put("TextField.background", BRANCO);
        UIManager.put("TextArea.background", BRANCO);
        UIManager.put("ComboBox.background", BRANCO);
        UIManager.put("TextField.foreground", TEXTO);
        UIManager.put("Label.foreground", TEXTO);

        UIManager.put("Table.background", BRANCO);
        UIManager.put("Table.foreground", TEXTO);
        UIManager.put("Table.rowHeight", 42);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.gridColor", new Color(0xEEF0F6));
        UIManager.put("Table.selectionBackground", SELECAO);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("Table.selectionInactiveBackground", SELECAO);
        UIManager.put("Table.selectionInactiveForeground", TEXTO);
        UIManager.put("TableHeader.background", new Color(0xF0F1F8));
        UIManager.put("TableHeader.foreground", NAVY);
        UIManager.put("TableHeader.cellMargins", new Insets(4, 14, 4, 14));
        UIManager.put("TableHeader.height", 38);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDA);
        UIManager.put("TableHeader.separatorColor", new Color(0xF0F1F8));
        UIManager.put("TableHeader.font", fonte(Font.BOLD, 12f));
    }
}
