package auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DTO.UsuarioDTO;
import org.junit.jupiter.api.Test;

class SessaoUsuarioTest {

    private static UsuarioDTO usuario(String username, String perfil) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId_usuario(1);
        dto.setUsername(username);
        dto.setPerfil(perfil);
        return dto;
    }

    @Test
    void adminEReconhecidoPeloPerfil() {
        SessaoUsuario sessao = new SessaoUsuario(usuario("admin", "ADMIN"));

        assertTrue(sessao.isAdmin());
        assertFalse(sessao.isEstudante());
    }

    @Test
    void estudanteNaoEAdmin() {
        SessaoUsuario sessao = new SessaoUsuario(usuario("aluno", "ESTUDANTE"));

        assertFalse(sessao.isAdmin());
        assertTrue(sessao.isEstudante());
    }

    @Test
    void perfilIgnoraCaixa() {
        assertTrue(new SessaoUsuario(usuario("x", "admin")).isAdmin());
    }

    @Test
    void permissaoNaoDependeDoNomeDeUsuario() {
        // Um estudante chamado "admin" continua sendo estudante.
        SessaoUsuario sessao = new SessaoUsuario(usuario("admin", "ESTUDANTE"));

        assertFalse(sessao.isAdmin());
    }

    @Test
    void perfilNuloOuDesconhecidoNaoEAdmin() {
        assertFalse(new SessaoUsuario(usuario("x", null)).isAdmin());
        assertFalse(new SessaoUsuario(usuario("x", "SUPERUSER")).isAdmin());
        assertFalse(new SessaoUsuario(usuario("x", "")).isAdmin());
    }

    @Test
    void exigirAdminLancaExcecaoParaEstudante() {
        SessaoUsuario sessao = new SessaoUsuario(usuario("aluno", "ESTUDANTE"));

        assertThrows(SessaoUsuario.AcessoNegadoException.class, sessao::exigirAdmin);
    }

    @Test
    void exigirAdminPermiteAdmin() {
        SessaoUsuario sessao = new SessaoUsuario(usuario("admin", "ADMIN"));

        assertDoesNotThrow(() -> sessao.exigirAdmin());
    }

    @Test
    void exigirAdminEstaticoNegaSessaoNula() {
        assertThrows(SessaoUsuario.AcessoNegadoException.class,
                () -> SessaoUsuario.exigirAdmin(null));
    }

    @Test
    void exigirAdminEstaticoNegaEstudanteEPermiteAdmin() {
        SessaoUsuario estudante = new SessaoUsuario(usuario("aluno", "ESTUDANTE"));
        SessaoUsuario admin = new SessaoUsuario(usuario("admin", "ADMIN"));

        assertThrows(SessaoUsuario.AcessoNegadoException.class,
                () -> SessaoUsuario.exigirAdmin(estudante));
        assertDoesNotThrow(() -> SessaoUsuario.exigirAdmin(admin));
    }

    @Test
    void sessaoExigeUsuarioAutenticado() {
        assertThrows(IllegalArgumentException.class, () -> new SessaoUsuario(null));
    }

    @Test
    void sessaoExpoeOUsuarioOriginal() {
        UsuarioDTO dto = usuario("aluno", "ESTUDANTE");

        assertSame(dto, new SessaoUsuario(dto).getUsuario());
    }
}
