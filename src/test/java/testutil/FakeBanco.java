package testutil;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Banco falso em memoria (tabelas {@code usuario} e {@code materias}) que
 * reconhece EXATAMENTE os SQLs emitidos pelos DAOs. Qualquer outro SQL derruba
 * o teste, o que evita que um DAO passe a emitir comandos nao previstos sem que
 * os testes percebam. Nenhum MySQL real e usado.
 */
public final class FakeBanco {

    public static final class Row {
        public int id;
        public String username;
        public String senha;
        public String perfil;
    }

    public static final class Aula {
        public int id;
        public String materia;
        public String tema;
        public String professor;
        public String data;
        public String conteudo;
    }

    public final List<Row> rows = new ArrayList<>();
    public final List<Aula> aulas = new ArrayList<>();
    public int abertas;
    public int fechadas;
    private int proximoIdUsuario = 1;
    private int proximoIdAula = 1;

    // ----------------------------- dados -----------------------------

    public Row add(String username, String senha, String perfil) {
        Row r = new Row();
        r.id = proximoIdUsuario++;
        r.username = username;
        r.senha = senha;
        r.perfil = perfil;
        rows.add(r);
        return r;
    }

    public Aula addAula(String materia, String tema, String professor, String data, String conteudo) {
        Aula a = new Aula();
        a.id = proximoIdAula++;
        a.materia = materia;
        a.tema = tema;
        a.professor = professor;
        a.data = data;
        a.conteudo = conteudo;
        aulas.add(a);
        return a;
    }

    public Row porId(int id) {
        return rows.stream().filter(r -> r.id == id).findFirst().orElse(null);
    }

    public Row porUsername(String username) {
        return rows.stream().filter(r -> r.username.equals(username)).findFirst().orElse(null);
    }

    public Aula aulaPorId(int id) {
        return aulas.stream().filter(a -> a.id == id).findFirst().orElse(null);
    }

    public Aula aulaPorTema(String idioma, String tema) {
        return aulas.stream()
                .filter(a -> a.materia.equalsIgnoreCase(idioma) && a.tema.equalsIgnoreCase(tema))
                .findFirst().orElse(null);
    }

    // ----------------------------- JDBC falso -----------------------------

    public Connection connection() {
        abertas++;
        return (Connection) proxy(Connection.class, (p, m, a) -> {
            switch (m.getName()) {
                case "prepareStatement":
                    return statement((String) a[0]);
                case "close":
                    fechadas++;
                    return null;
                case "isClosed":
                    return false;
                default:
                    return valorPadrao(m.getReturnType());
            }
        });
    }

    private PreparedStatement statement(String sql) {
        Map<Integer, Object> params = new HashMap<>();
        return (PreparedStatement) proxy(PreparedStatement.class, (p, m, a) -> {
            switch (m.getName()) {
                case "setString":
                case "setInt":
                    params.put((Integer) a[0], a[1]);
                    return null;
                case "executeQuery":
                    return resultSet(consulta(sql, params));
                case "executeUpdate":
                    return atualizacao(sql, params);
                case "close":
                    return null;
                default:
                    return valorPadrao(m.getReturnType());
            }
        });
    }

    private List<Map<String, Object>> consulta(String sql, Map<Integer, Object> p) {
        List<Map<String, Object>> out = new ArrayList<>();
        switch (sql) {
            // ----- usuario -----
            case "select perfil from usuario where id_usuario = ?": {
                Row r = porId((Integer) p.get(1));
                if (r != null) {
                    out.add(linha("perfil", r.perfil));
                }
                return out;
            }
            case "select id_usuario, username, perfil from usuario order by id_usuario":
                for (Row r : rows) {
                    out.add(linha("id_usuario", r.id, "username", r.username, "perfil", r.perfil));
                }
                return out;
            case "select id_usuario, username, perfil from usuario where id_usuario = ?": {
                Row r = porId((Integer) p.get(1));
                if (r != null) {
                    out.add(linha("id_usuario", r.id, "username", r.username, "perfil", r.perfil));
                }
                return out;
            }
            case "select id_usuario, username, senha, perfil from usuario where username = ?": {
                String nome = (String) p.get(1);
                for (Row r : rows) {
                    // O MySQL usa collation case-insensitive (utf8mb4_0900_ai_ci).
                    if (r.username.equalsIgnoreCase(nome)) {
                        out.add(linha("id_usuario", r.id, "username", r.username, "senha", r.senha,
                                "perfil", r.perfil));
                    }
                }
                return out;
            }
            case "select 1 from usuario where username = ? and id_usuario <> ?": {
                String nome = (String) p.get(1);
                int ignorado = (Integer) p.get(2);
                for (Row r : rows) {
                    if (r.username.equalsIgnoreCase(nome) && r.id != ignorado) {
                        out.add(linha("1", 1));
                    }
                }
                return out;
            }
            case "select count(*) from usuario where username = ?": {
                String nome = (String) p.get(1);
                long n = rows.stream().filter(r -> r.username.equalsIgnoreCase(nome)).count();
                out.add(linha("count(*)", (int) n));
                return out;
            }
            case "select count(*) from materias":
                out.add(linha("count(*)", aulas.size()));
                return out;
            case "select count(*) from usuario where perfil = ?": {
                String perfil = (String) p.get(1);
                long n = rows.stream().filter(r -> r.perfil.equalsIgnoreCase(perfil)).count();
                out.add(linha("count(*)", (int) n));
                return out;
            }
            // ----- materias -----
            case "select id_materias, materia, tema, professor, data, conteudo "
                    + "from materias order by materia, id_materias": {
                List<Aula> ordenadas = new ArrayList<>(aulas);
                ordenadas.sort(Comparator.<Aula, String>comparing(a -> a.materia.toLowerCase(Locale.ROOT))
                        .thenComparingInt(a -> a.id));
                for (Aula a : ordenadas) {
                    out.add(linha("id_materias", a.id, "materia", a.materia, "tema", a.tema,
                            "professor", a.professor, "data", a.data, "conteudo", a.conteudo));
                }
                return out;
            }
            case "select 1 from materias where id_materias = ?":
                if (aulaPorId((Integer) p.get(1)) != null) {
                    out.add(linha("1", 1));
                }
                return out;
            case "select 1 from materias where materia = ? and tema = ? and id_materias <> ?": {
                String materia = (String) p.get(1);
                String tema = (String) p.get(2);
                int ignorado = (Integer) p.get(3);
                for (Aula a : aulas) {
                    if (a.materia.equalsIgnoreCase(materia) && a.tema.equalsIgnoreCase(tema) && a.id != ignorado) {
                        out.add(linha("1", 1));
                    }
                }
                return out;
            }
            default:
                throw new AssertionError("SQL de consulta nao previsto: " + sql);
        }
    }

    private int atualizacao(String sql, Map<Integer, Object> p) {
        switch (sql) {
            case "insert into usuario (username, senha, perfil) values (?, ?, ?)":
                add((String) p.get(1), (String) p.get(2), (String) p.get(3));
                return 1;
            case "update usuario set username = ?, perfil = ?, senha = ? where id_usuario = ?": {
                Row r = porId((Integer) p.get(4));
                r.username = (String) p.get(1);
                r.perfil = (String) p.get(2);
                r.senha = (String) p.get(3);
                return 1;
            }
            case "update usuario set username = ?, perfil = ? where id_usuario = ?": {
                Row r = porId((Integer) p.get(3));
                r.username = (String) p.get(1);
                r.perfil = (String) p.get(2);
                return 1;
            }
            case "delete from usuario where id_usuario = ?": {
                Row r = porId((Integer) p.get(1));
                return rows.remove(r) ? 1 : 0;
            }
            case "insert into materias (professor, materia, data, tema, conteudo) values (?, ?, ?, ?, ?)":
                addAula((String) p.get(2), (String) p.get(4), (String) p.get(1), (String) p.get(3),
                        (String) p.get(5));
                return 1;
            case "update materias set professor = ?, materia = ?, data = ?, tema = ?, conteudo = ? "
                    + "where id_materias = ?": {
                Aula a = aulaPorId((Integer) p.get(6));
                a.professor = (String) p.get(1);
                a.materia = (String) p.get(2);
                a.data = (String) p.get(3);
                a.tema = (String) p.get(4);
                a.conteudo = (String) p.get(5);
                return 1;
            }
            case "delete from materias where id_materias = ?": {
                Aula a = aulaPorId((Integer) p.get(1));
                return aulas.remove(a) ? 1 : 0;
            }
            default:
                throw new AssertionError("SQL de alteracao nao previsto: " + sql);
        }
    }

    private static Map<String, Object> linha(Object... paresChaveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < paresChaveValor.length; i += 2) {
            m.put((String) paresChaveValor[i], paresChaveValor[i + 1]);
        }
        return m;
    }

    private static ResultSet resultSet(List<Map<String, Object>> linhas) {
        int[] cursor = {-1};
        return (ResultSet) proxy(ResultSet.class, (p, m, a) -> {
            switch (m.getName()) {
                case "next":
                    return ++cursor[0] < linhas.size();
                case "getString":
                case "getInt": {
                    Map<String, Object> atual = linhas.get(cursor[0]);
                    Object v = a[0] instanceof Integer i
                            ? new ArrayList<>(atual.values()).get(i - 1)
                            : atual.get((String) a[0]);
                    if (m.getName().equals("getInt")) {
                        return v == null ? 0 : ((Number) v).intValue();
                    }
                    return v == null ? null : v.toString();
                }
                case "close":
                    return null;
                default:
                    return valorPadrao(m.getReturnType());
            }
        });
    }

    private interface Handler {
        Object invoke(Object proxy, Method method, Object[] args) throws Throwable;
    }

    private static Object proxy(Class<?> tipo, Handler handler) {
        return Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] {tipo}, (p, m, a) -> {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "equals":
                        return p == a[0];
                    case "hashCode":
                        return System.identityHashCode(p);
                    default:
                        return "FakeJdbc[" + tipo.getSimpleName() + "]";
                }
            }
            return handler.invoke(p, m, a);
        });
    }

    private static Object valorPadrao(Class<?> tipo) {
        if (tipo == boolean.class) {
            return false;
        }
        if (tipo == int.class) {
            return 0;
        }
        if (tipo == long.class) {
            return 0L;
        }
        return null;
    }
}
