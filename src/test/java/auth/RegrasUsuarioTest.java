package auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegrasUsuarioTest {

    // ---------- perfil ----------

    @Test
    void perfilValidoEhNormalizado() throws Exception {
        assertEquals("ADMIN", RegrasUsuario.normalizarPerfil("admin"));
        assertEquals("ESTUDANTE", RegrasUsuario.normalizarPerfil("Estudante"));
        assertEquals("ADMIN", RegrasUsuario.normalizarPerfil(" ADMIN "));
    }

    @Test
    void perfilInvalidoNuloOuVazioEhRejeitado() {
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.normalizarPerfil("root"));
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.normalizarPerfil("SUPERADMIN"));
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.normalizarPerfil(null));
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.normalizarPerfil(""));
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.normalizarPerfil("   "));
    }

    // ---------- username ----------

    @Test
    void usernameEhAparado() throws Exception {
        assertEquals("raul", RegrasUsuario.validarUsername("  raul "));
    }

    @Test
    void usernameNuloOuVazioEhRejeitado() {
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.validarUsername(null));
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.validarUsername(""));
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.validarUsername("    "));
    }

    @Test
    void usernameRespeitaLimiteDe50Caracteres() throws Exception {
        assertEquals(50, RegrasUsuario.validarUsername("a".repeat(50)).length());
        assertThrows(OperacaoUsuarioException.class,
                () -> RegrasUsuario.validarUsername("a".repeat(51)));
    }

    @Test
    void limiteDoUsernameConsideraOTextoAparado() throws Exception {
        // 50 letras mais espacos nas pontas: o espaco nao conta.
        assertEquals(50, RegrasUsuario.validarUsername("  " + "a".repeat(50) + "  ").length());
    }

    // ---------- senha ----------

    @Test
    void senhaComMinimoDeSeisCaracteresEhAceita() {
        assertDoesNotThrow(() -> RegrasUsuario.validarSenhaNova("123456"));
        assertDoesNotThrow(() -> RegrasUsuario.validarSenhaNova("admin-teste-1"));
        assertDoesNotThrow(() -> RegrasUsuario.validarSenhaNova("aluno-teste-1"));
        assertDoesNotThrow(() -> RegrasUsuario.validarSenhaNova("rodrigo-teste-1"));
    }

    @Test
    void senhaNulaVaziaOuCurtaEhRejeitada() {
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.validarSenhaNova(null));
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.validarSenhaNova(""));
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.validarSenhaNova("12345"));
    }

    @Test
    void senhaDe72BytesEhAceitaE73BytesEhRejeitada() {
        assertDoesNotThrow(() -> RegrasUsuario.validarSenhaNova("a".repeat(72)));
        assertThrows(OperacaoUsuarioException.class,
                () -> RegrasUsuario.validarSenhaNova("a".repeat(73)));
    }

    @Test
    void limiteDeBytesConsideraCaracteresAcentuados() {
        // "é" ocupa 2 bytes em UTF-8: 36 caracteres = 72 bytes (ok); 37 = 74 bytes (rejeitada).
        assertDoesNotThrow(() -> RegrasUsuario.validarSenhaNova("é".repeat(36)));
        assertThrows(OperacaoUsuarioException.class,
                () -> RegrasUsuario.validarSenhaNova("é".repeat(37)));
    }

    @Test
    void minimoContaCaracteresENaoBytes() {
        // 5 caracteres acentuados ocupam 10 bytes, mas continuam sendo 5 caracteres.
        assertThrows(OperacaoUsuarioException.class,
                () -> RegrasUsuario.validarSenhaNova("ééééé"));
        assertDoesNotThrow(() -> RegrasUsuario.validarSenhaNova("éééééé"));
    }

    // ---------- senha opcional (edicao) ----------

    @Test
    void senhaOpcionalAusenteSignificaManterASenhaAtual() throws Exception {
        assertFalse(RegrasUsuario.validarSenhaOpcional(null));
        assertFalse(RegrasUsuario.validarSenhaOpcional(""));
    }

    @Test
    void senhaOpcionalInformadaEValida() throws Exception {
        assertTrue(RegrasUsuario.validarSenhaOpcional("novaSenha1"));
    }

    @Test
    void senhaOpcionalInformadaMasInvalidaEhRejeitada() {
        assertThrows(OperacaoUsuarioException.class, () -> RegrasUsuario.validarSenhaOpcional("abc"));
        assertThrows(OperacaoUsuarioException.class,
                () -> RegrasUsuario.validarSenhaOpcional("a".repeat(73)));
    }

    // ---------- ultimo administrador ----------

    @Test
    void rebaixarOUnicoAdminEhBloqueado() {
        OperacaoUsuarioException ex = assertThrows(OperacaoUsuarioException.class,
                () -> RegrasUsuario.validarMudancaDeAdmin(true, "ESTUDANTE", 1));
        assertTrue(ex.getMessage().contains("último administrador"));
    }

    @Test
    void rebaixarAdminQuandoHaOutroEhPermitido() {
        assertDoesNotThrow(() -> RegrasUsuario.validarMudancaDeAdmin(true, "ESTUDANTE", 2));
    }

    @Test
    void manterOUnicoAdminComoAdminEhPermitido() {
        assertDoesNotThrow(() -> RegrasUsuario.validarMudancaDeAdmin(true, "ADMIN", 1));
        assertDoesNotThrow(() -> RegrasUsuario.validarMudancaDeAdmin(true, " admin ", 1));
    }

    @Test
    void removerOUnicoAdminEhBloqueado() {
        assertThrows(OperacaoUsuarioException.class,
                () -> RegrasUsuario.validarMudancaDeAdmin(true, null, 1));
    }

    @Test
    void removerAdminQuandoHaOutroEhPermitido() {
        assertDoesNotThrow(() -> RegrasUsuario.validarMudancaDeAdmin(true, null, 2));
    }

    @Test
    void removerEstudanteEhSemprePermitido() {
        assertDoesNotThrow(() -> RegrasUsuario.validarMudancaDeAdmin(false, null, 1));
        assertDoesNotThrow(() -> RegrasUsuario.validarMudancaDeAdmin(false, null, 0));
    }

    @Test
    void promoverEstudanteAAdminEhPermitido() {
        assertDoesNotThrow(() -> RegrasUsuario.validarMudancaDeAdmin(false, "ADMIN", 0));
        assertDoesNotThrow(() -> RegrasUsuario.validarMudancaDeAdmin(false, "ADMIN", 1));
    }

    @Test
    void perfilDesconhecidoNoRebaixamentoEhTratadoComoPerdaDeAdmin() {
        // Conservador: qualquer valor diferente de ADMIN conta como rebaixamento.
        assertThrows(OperacaoUsuarioException.class,
                () -> RegrasUsuario.validarMudancaDeAdmin(true, "OUTRO", 1));
    }
}
