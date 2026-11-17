package DAO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abre conexoes com o MySQL.
 *
 * <p>A configuracao NAO fica no codigo. Ordem de prioridade:</p>
 * <ol>
 *   <li>variaveis de ambiente {@code LINGUIFY_DB_URL}, {@code LINGUIFY_DB_USER}
 *       e {@code LINGUIFY_DB_PASSWORD};</li>
 *   <li>arquivo {@code config.properties} na pasta de execucao (ou o caminho indicado
 *       em {@code LINGUIFY_CONFIG}) com as chaves {@code db.url}, {@code db.user} e
 *       {@code db.password};</li>
 *   <li>valores padrao para instalacao local: MySQL em localhost:3306, banco
 *       {@code login}, usuario {@code root} sem senha.</li>
 * </ol>
 */
public class ConexaoDAO {

    private static final Logger LOG = Logger.getLogger(ConexaoDAO.class.getName());

    static final String URL_PADRAO = "jdbc:mysql://localhost:3306/login?characterEncoding=UTF-8";
    static final String USUARIO_PADRAO = "root";
    static final String SENHA_PADRAO = "";

    /**
     * @return uma nova conexao, ou {@code null} se o banco estiver indisponivel
     *         (o motivo e registrado no log, sem a senha).
     */
    public Connection conectaBD() {
        try {
            Properties cfg = carregarConfiguracao(
                    Path.of(System.getenv().getOrDefault("LINGUIFY_CONFIG", "config.properties")),
                    System.getenv());
            return DriverManager.getConnection(
                    cfg.getProperty("db.url"), cfg.getProperty("db.user"), cfg.getProperty("db.password"));
        } catch (Exception erro) {
            LOG.log(Level.WARNING, "Nao foi possivel conectar ao banco de dados: {0}", erro.getMessage());
            return null;
        }
    }

    /**
     * Monta a configuracao final (padrao, depois arquivo, depois variaveis de ambiente).
     * Separado para poder ser testado sem banco.
     */
    static Properties carregarConfiguracao(Path arquivo, Map<String, String> ambiente) {
        Properties cfg = new Properties();
        cfg.setProperty("db.url", URL_PADRAO);
        cfg.setProperty("db.user", USUARIO_PADRAO);
        cfg.setProperty("db.password", SENHA_PADRAO);

        if (arquivo != null && Files.isRegularFile(arquivo)) {
            try (InputStream in = Files.newInputStream(arquivo)) {
                Properties doArquivo = new Properties();
                doArquivo.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                cfg.putAll(doArquivo);
            } catch (IOException erro) {
                LOG.log(Level.WARNING, "Nao foi possivel ler {0}: {1}", new Object[] {arquivo, erro.getMessage()});
            }
        }

        sobrescrever(cfg, "db.url", ambiente.get("LINGUIFY_DB_URL"));
        sobrescrever(cfg, "db.user", ambiente.get("LINGUIFY_DB_USER"));
        sobrescrever(cfg, "db.password", ambiente.get("LINGUIFY_DB_PASSWORD"));
        return cfg;
    }

    private static void sobrescrever(Properties cfg, String chave, String valor) {
        if (valor != null) {
            cfg.setProperty(chave, valor);
        }
    }
}
