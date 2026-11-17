package testutil;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import ui.Dialogos;

/** Provedor de dialogos para testes: registra as mensagens e responde as confirmacoes sem abrir janelas. */
public final class DialogosFake implements Dialogos.Provedor {

    public final List<String> mensagens = new ArrayList<>();
    public final List<String> confirmacoes = new ArrayList<>();
    public boolean responder = true;

    @Override
    public void mensagem(Component pai, Dialogos.Tipo tipo, String texto) {
        mensagens.add(tipo + ": " + texto);
    }

    @Override
    public boolean confirmar(Component pai, String titulo, String texto, String rotuloConfirmar, boolean perigo) {
        confirmacoes.add(titulo + ": " + texto);
        return responder;
    }

    public boolean teve(Dialogos.Tipo tipo) {
        return mensagens.stream().anyMatch(m -> m.startsWith(tipo + ":"));
    }

    public String ultima() {
        return mensagens.isEmpty() ? "" : mensagens.get(mensagens.size() - 1);
    }
}
