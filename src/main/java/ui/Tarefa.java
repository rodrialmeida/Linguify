package ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/**
 * Executa um trabalho demorado (banco de dados, BCrypt) fora da thread de
 * interface e devolve o resultado nela, para a tela nao travar.
 */
public final class Tarefa {

    private static boolean sincrono;

    private Tarefa() {
    }

    /** Nos testes, executa tudo na thread atual para tornar o resultado previsivel. */
    public static void modoSincrono(boolean valor) {
        sincrono = valor;
    }

    public static <T> void executar(Callable<T> trabalho, Consumer<T> sucesso, Consumer<Exception> falha) {
        if (sincrono) {
            T resultado;
            try {
                resultado = trabalho.call();
            } catch (Exception e) {
                falha.accept(e);
                return;
            }
            sucesso.accept(resultado);
            return;
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabalho.call();
            }

            @Override
            protected void done() {
                T resultado;
                try {
                    resultado = get();
                } catch (ExecutionException e) {
                    falha.accept(e.getCause() instanceof Exception ? (Exception) e.getCause() : e);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    falha.accept(e);
                    return;
                }
                sucesso.accept(resultado);
            }
        }.execute();
    }
}
