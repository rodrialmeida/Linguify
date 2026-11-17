package DTO;

/** Dados de uma aula (tabela {@code materias}): idioma, nome da aula, professor, data e conteudo. */
public class MateriasDTO {
    private int id_materias;
    private String professor, materia, tema, conteudo , data;
    
    
    public String getProfessor() {
        return professor;
    }

    public void setProfessor(String professor) {
        this.professor = professor;
    }

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getId_materias() {
        return id_materias;
    }

    public void setId_materias(int id_materias) {
        this.id_materias = id_materias;
    }

    

    

}
