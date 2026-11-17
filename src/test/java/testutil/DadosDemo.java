package testutil;

import util.PasswordHasher;

/** Dados de demonstracao usados nos testes (mesmos usuarios e aulas do banco real). */
public final class DadosDemo {

    private static final String HASH_ADMIN = PasswordHasher.hash("admin-teste-1");
    private static final String HASH_ALUNO = PasswordHasher.hash("aluno-teste-1");
    private static final String HASH_RODRIGO = PasswordHasher.hash("rodrigo-teste-2");

    private DadosDemo() {
    }

    /** Cria admin (id 1), aluno (id 2) e rodrigo (id 3) e as 8 aulas de idiomas. */
    public static FakeBanco popular(FakeBanco db) {
        db.add("admin", HASH_ADMIN, "ADMIN");
        db.add("aluno", HASH_ALUNO, "ESTUDANTE");
        db.add("rodrigo", HASH_RODRIGO, "ESTUDANTE");

        db.addAula("Inglês", "Greetings", "Linguify Demo", "09/11/2026", "Cumprimentos em inglês.");
        db.addAula("Inglês", "Present Simple", "Linguify Demo", "10/11/2026", "Presente simples.");
        db.addAula("Espanhol", "Saludos", "Linguify Demo", "11/11/2026", "Saludos en español.");
        db.addAula("Espanhol", "Verbos básicos", "Linguify Demo", "12/11/2026", "Ser y estar.");
        db.addAula("Francês", "Salutations", "Linguify Demo", "13/11/2026", "Salutations en français.");
        db.addAula("Francês", "Verbes essentiels", "Linguify Demo", "16/11/2026", "Être et avoir.");
        db.addAula("Alemão", "Begrüßungen", "Linguify Demo", "17/11/2026", "Begrüßungen auf Deutsch.");
        db.addAula("Alemão", "Grundverben", "Linguify Demo", "18/11/2026", "Sein und haben.");
        return db;
    }
}
