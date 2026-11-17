package auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DTO.MateriasDTO;
import org.junit.jupiter.api.Test;

class RegrasAulaTest {

    private static MateriasDTO valida() {
        MateriasDTO a = new MateriasDTO();
        a.setId_materias(7);
        a.setMateria("Inglês");
        a.setTema("Greetings");
        a.setProfessor("Linguify Demo");
        a.setData("09/11/2026");
        a.setConteudo("Cumprimentos.");
        return a;
    }

    @Test
    void aulaValidaVoltaComoCopiaAparada() throws Exception {
        MateriasDTO a = valida();
        a.setMateria("  Inglês ");
        a.setData(" 09/11/2026 ");

        MateriasDTO limpa = RegrasAula.validar(a);

        assertNotSame(a, limpa);
        assertEquals("Inglês", limpa.getMateria());
        assertEquals("09/11/2026", limpa.getData());
        assertEquals(7, limpa.getId_materias());
        assertEquals("Cumprimentos.", limpa.getConteudo());
    }

    @Test
    void camposObrigatorios() {
        for (int campo = 0; campo < 5; campo++) {
            MateriasDTO a = valida();
            switch (campo) {
                case 0 -> a.setMateria(" ");
                case 1 -> a.setTema(null);
                case 2 -> a.setProfessor("");
                case 3 -> a.setData(" ");
                default -> a.setConteudo("");
            }
            assertThrows(OperacaoUsuarioException.class, () -> RegrasAula.validar(a), "campo " + campo);
        }
        assertThrows(OperacaoUsuarioException.class, () -> RegrasAula.validar(null));
    }

    @Test
    void limitesDasColunas() throws Exception {
        MateriasDTO noLimite = valida();
        noLimite.setMateria("i".repeat(RegrasAula.IDIOMA_MAX));
        noLimite.setTema("t".repeat(RegrasAula.TEMA_MAX));
        noLimite.setProfessor("p".repeat(RegrasAula.PROFESSOR_MAX));
        noLimite.setConteudo("c".repeat(RegrasAula.CONTEUDO_MAX));
        RegrasAula.validar(noLimite);

        String[] excessos = {"idioma", "tema", "professor", "conteudo"};
        for (String qual : excessos) {
            MateriasDTO a = valida();
            switch (qual) {
                case "idioma" -> a.setMateria("i".repeat(RegrasAula.IDIOMA_MAX + 1));
                case "tema" -> a.setTema("t".repeat(RegrasAula.TEMA_MAX + 1));
                case "professor" -> a.setProfessor("p".repeat(RegrasAula.PROFESSOR_MAX + 1));
                default -> a.setConteudo("c".repeat(RegrasAula.CONTEUDO_MAX + 1));
            }
            OperacaoUsuarioException ex = assertThrows(OperacaoUsuarioException.class, () -> RegrasAula.validar(a));
            assertTrue(ex.getMessage().contains("no máximo"), qual);
        }
    }

    @Test
    void limiteContaCaracteresEAcentuadosNaoPassamDoLimite() throws Exception {
        MateriasDTO a = valida();
        a.setConteudo("é".repeat(RegrasAula.CONTEUDO_MAX));
        RegrasAula.validar(a);
    }

    @Test
    void dataPrecisaSerReal() throws Exception {
        assertEquals("29/02/2028", RegrasAula.validar(comData("29/02/2028")).getData());
        for (String ruim : new String[] {"31/02/2026", "29/02/2027", "2026-11-09", "9/11/2026", "09/13/2026",
            "texto", "09\\11\\26"}) {
            assertThrows(OperacaoUsuarioException.class, () -> RegrasAula.validar(comData(ruim)), ruim);
        }
    }

    @Test
    void hojeTemOFormatoEsperado() {
        assertTrue(RegrasAula.hoje().matches("\\d{2}/\\d{2}/\\d{4}"));
    }

    private static MateriasDTO comData(String data) {
        MateriasDTO a = valida();
        a.setData(data);
        return a;
    }
}
