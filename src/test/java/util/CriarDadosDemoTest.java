package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import testutil.FakeBanco;

class CriarDadosDemoTest {

    @Test
    void criaAsTresContasComBcryptEAsOitoAulasDeIdiomas() throws Exception {
        FakeBanco db = new FakeBanco();

        CriarDadosDemo.Resultado r = CriarDadosDemo.executar(db::connection, "senhaA1", "senhaB2", "senhaC3");

        assertEquals(3, r.usuariosCriados());
        assertEquals(0, r.usuariosJaExistiam());
        assertEquals(8, r.aulasCriadas());
        assertEquals("ADMIN", db.porUsername("admin").perfil);
        assertEquals("ESTUDANTE", db.porUsername("aluno").perfil);
        assertEquals("ESTUDANTE", db.porUsername("rodrigo").perfil);
        assertTrue(PasswordHasher.verify("senhaA1", db.porUsername("admin").senha));
        assertTrue(PasswordHasher.verify("senhaB2", db.porUsername("aluno").senha));
        assertTrue(PasswordHasher.verify("senhaC3", db.porUsername("rodrigo").senha));
        for (FakeBanco.Row u : db.rows) {
            assertTrue(u.senha.startsWith("$2a$") && u.senha.length() == 60, "senha sem hash BCrypt");
        }
        assertEquals(8, db.aulas.size());
        assertNotNull(db.aulaPorTema("Alemão", "Begrüßungen"));
        assertNotNull(db.aulaPorTema("Espanhol", "Verbos básicos"));
        for (FakeBanco.Aula a : db.aulas) {
            assertEquals("Linguify Demo", a.professor);
            assertTrue(a.data.matches("\\d{2}/\\d{2}/\\d{4}"));
            assertTrue(!a.conteudo.isBlank() && a.conteudo.length() <= 255, a.tema);
        }
        assertEquals(db.abertas, db.fechadas);
    }

    @Test
    void repetirACargaNaoDuplicaNemAlteraNada() throws Exception {
        FakeBanco db = new FakeBanco();
        CriarDadosDemo.executar(db::connection, "senhaA1", "senhaB2", "senhaC3");
        String hashAntes = db.porUsername("admin").senha;

        CriarDadosDemo.Resultado r = CriarDadosDemo.executar(db::connection, "OUTRA1", "OUTRA2", "OUTRA3");

        assertEquals(0, r.usuariosCriados());
        assertEquals(3, r.usuariosJaExistiam());
        assertEquals(0, r.aulasCriadas());
        assertEquals(3, db.rows.size());
        assertEquals(8, db.aulas.size());
        assertEquals(hashAntes, db.porUsername("admin").senha, "contas existentes nao sao alteradas");
    }

    @Test
    void aulasExistentesNaoSaoSubstituidas() throws Exception {
        FakeBanco db = new FakeBanco();
        db.addAula("Italiano", "Saluti", "Prof", "01/12/2026", "Conteúdo existente.");

        CriarDadosDemo.Resultado r = CriarDadosDemo.executar(db::connection, "a1", "b2", "c3");

        assertEquals(0, r.aulasCriadas());
        assertEquals(1, db.aulas.size());
    }

    @Test
    void senhasSaoObrigatoriasEBancoIndisponivelEInformado() {
        FakeBanco db = new FakeBanco();

        assertThrows(IllegalArgumentException.class,
                () -> CriarDadosDemo.executar(db::connection, "", "b2", "c3"));
        assertThrows(IllegalArgumentException.class,
                () -> CriarDadosDemo.executar(db::connection, "a1", null, "c3"));
        assertThrows(java.sql.SQLException.class,
                () -> CriarDadosDemo.executar(() -> null, "a1", "b2", "c3"));
        assertEquals(0, db.rows.size());
    }
}
