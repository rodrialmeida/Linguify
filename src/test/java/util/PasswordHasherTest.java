package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void hashTemFormatoBcryptDe60Caracteres() {
        String hash = PasswordHasher.hash("admin-teste-1");

        assertEquals(60, hash.length());
        assertTrue(hash.startsWith("$2a$"));
    }

    @Test
    void hashNuncaContemASenhaOriginal() {
        String hash = PasswordHasher.hash("admin-teste-1");

        assertFalse(hash.contains("admin-teste-1"));
    }

    @Test
    void doisHashesDaMesmaSenhaSaoDiferentes() {
        assertNotEquals(PasswordHasher.hash("aluno-teste-1"), PasswordHasher.hash("aluno-teste-1"));
    }

    @Test
    void verifyAceitaSenhaCorreta() {
        String hash = PasswordHasher.hash("rodrigo-teste-1");

        assertTrue(PasswordHasher.verify("rodrigo-teste-1", hash));
    }

    @Test
    void verifyRecusaSenhaIncorreta() {
        String hash = PasswordHasher.hash("rodrigo-teste-1");

        assertFalse(PasswordHasher.verify("rodrigo124", hash));
        assertFalse(PasswordHasher.verify("", hash));
    }

    @Test
    void verifyRecusaEntradasNulasOuVazias() {
        String hash = PasswordHasher.hash("x12345");

        assertFalse(PasswordHasher.verify(null, hash));
        assertFalse(PasswordHasher.verify("x12345", null));
        assertFalse(PasswordHasher.verify("x12345", ""));
    }

    @Test
    void verifyRecusaSenhaAntigaEmTextoPuroNoLugarDoHash() {
        // Usuarios legados guardam a senha em texto puro: nao devem autenticar.
        assertFalse(PasswordHasher.verify("123", "123"));
        assertFalse(PasswordHasher.verify("legado-teste-9", "legado-teste-9"));
    }

    @Test
    void hashRejeitaSenhaNula() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }
}
