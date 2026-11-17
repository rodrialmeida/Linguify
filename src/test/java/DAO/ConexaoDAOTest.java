package DAO;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConexaoDAOTest {

    @Test
    void semArquivoNemVariaveisUsaOPadraoLocal(@TempDir Path pasta) {
        Properties cfg = ConexaoDAO.carregarConfiguracao(pasta.resolve("nao-existe.properties"), Map.of());

        assertEquals(ConexaoDAO.URL_PADRAO, cfg.getProperty("db.url"));
        assertEquals("root", cfg.getProperty("db.user"));
        assertEquals("", cfg.getProperty("db.password"));
    }

    @Test
    void arquivoSobrescreveOPadrao(@TempDir Path pasta) throws Exception {
        Path arquivo = pasta.resolve("config.properties");
        Files.writeString(arquivo, "db.url=jdbc:mysql://servidor:3307/outro\ndb.user=app\ndb.password=segredo\n");

        Properties cfg = ConexaoDAO.carregarConfiguracao(arquivo, Map.of());

        assertEquals("jdbc:mysql://servidor:3307/outro", cfg.getProperty("db.url"));
        assertEquals("app", cfg.getProperty("db.user"));
        assertEquals("segredo", cfg.getProperty("db.password"));
    }

    @Test
    void variaveisDeAmbienteTemPrioridadeSobreOArquivo(@TempDir Path pasta) throws Exception {
        Path arquivo = pasta.resolve("config.properties");
        Files.writeString(arquivo, "db.user=app\ndb.password=segredo\n");

        Properties cfg = ConexaoDAO.carregarConfiguracao(arquivo,
                Map.of("LINGUIFY_DB_USER", "outro", "LINGUIFY_DB_PASSWORD", "env"));

        assertEquals("outro", cfg.getProperty("db.user"));
        assertEquals("env", cfg.getProperty("db.password"));
        assertEquals(ConexaoDAO.URL_PADRAO, cfg.getProperty("db.url"));
    }

    @Test
    void arquivoParcialMantemOsDemaisPadroes(@TempDir Path pasta) throws Exception {
        Path arquivo = pasta.resolve("config.properties");
        Files.writeString(arquivo, "db.password=so-a-senha\n");

        Properties cfg = ConexaoDAO.carregarConfiguracao(arquivo, Map.of());

        assertEquals("so-a-senha", cfg.getProperty("db.password"));
        assertEquals("root", cfg.getProperty("db.user"));
    }
}
