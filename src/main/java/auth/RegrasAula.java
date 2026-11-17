package auth;

import DTO.MateriasDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Regras de negocio puras sobre aulas (tabela {@code materias}), sem banco nem
 * interface. Os limites espelham as colunas atuais da tabela.
 */
public final class RegrasAula {

    public static final int IDIOMA_MAX = 100;     // coluna materia
    public static final int TEMA_MAX = 100;       // coluna tema
    public static final int PROFESSOR_MAX = 50;   // coluna professor
    public static final int DATA_MAX = 30;        // coluna data
    public static final int CONTEUDO_MAX = 255;   // coluna conteudo

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private RegrasAula() {
    }

    /** Data de hoje no formato dd/MM/yyyy. */
    public static String hoje() {
        return LocalDate.now().format(FORMATO_DATA);
    }

    /**
     * Valida todos os campos (obrigatorios, limites e data dd/MM/yyyy) e devolve
     * uma COPIA com os textos sem espacos nas pontas.
     *
     * @throws OperacaoUsuarioException com mensagem amigavel para o primeiro problema
     */
    public static MateriasDTO validar(MateriasDTO dto) throws OperacaoUsuarioException {
        if (dto == null) {
            throw new OperacaoUsuarioException("Dados da aula não informados.");
        }
        MateriasDTO limpo = new MateriasDTO();
        limpo.setId_materias(dto.getId_materias());
        limpo.setMateria(texto(dto.getMateria(), "O idioma", IDIOMA_MAX));
        limpo.setTema(texto(dto.getTema(), "O nome da aula", TEMA_MAX));
        limpo.setProfessor(texto(dto.getProfessor(), "O professor", PROFESSOR_MAX));
        limpo.setData(data(dto.getData()));
        limpo.setConteudo(texto(dto.getConteudo(), "O conteúdo", CONTEUDO_MAX));
        return limpo;
    }

    private static String texto(String valor, String campo, int max) throws OperacaoUsuarioException {
        if (valor == null || valor.isBlank()) {
            throw new OperacaoUsuarioException(campo + " é obrigatório.");
        }
        String limpo = valor.trim();
        if (limpo.codePointCount(0, limpo.length()) > max) {
            throw new OperacaoUsuarioException(campo + " deve ter no máximo " + max + " caracteres.");
        }
        return limpo;
    }

    private static String data(String valor) throws OperacaoUsuarioException {
        if (valor == null || valor.isBlank()) {
            throw new OperacaoUsuarioException("A data é obrigatória.");
        }
        try {
            return LocalDate.parse(valor.trim(), FORMATO_DATA).format(FORMATO_DATA);
        } catch (DateTimeParseException e) {
            throw new OperacaoUsuarioException("Data inválida. Use o formato dd/MM/aaaa, por exemplo 25/12/2026.");
        }
    }
}
