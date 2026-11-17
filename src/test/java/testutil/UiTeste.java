package testutil;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import ui.Dialogos;
import ui.Tarefa;
import ui.Tema;

/** Base dos testes de interface: instala o tema, torna as tarefas sincronas e simula os dialogos. */
public abstract class UiTeste {

    protected DialogosFake dialogos;

    @BeforeAll
    static void prepararInterface() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Requer ambiente grafico.");
        Tema.instalar();
        Tarefa.modoSincrono(true);
    }

    @BeforeEach
    void instalarDialogosFalsos() {
        dialogos = new DialogosFake();
        Dialogos.usar(dialogos);
    }

    @AfterEach
    void restaurarDialogos() {
        Dialogos.usar(null);
    }
}
