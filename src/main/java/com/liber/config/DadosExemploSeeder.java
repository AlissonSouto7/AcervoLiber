package com.liber.config;

import com.liber.entity.Aluno;
import com.liber.entity.Livro;
import com.liber.repository.AlunoRepository;
import com.liber.repository.LivroRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Popula o banco com 80 livros + 40 alunos de exemplo para testes em producao.
 *
 * <p>Roda apenas se {@code app.seed.dados-exemplo=true} (env {@code SEED_DADOS_EXEMPLO=true}).
 *
 * <p><b>Idempotente:</b> verifica por ISBN/matricula antes de inserir, entao pode rodar
 * varias vezes sem duplicar. Pra LIMPAR antes de entregar pra escola, execute
 * {@code scripts/seed-teste-cleanup.sql} no Neon SQL Editor.
 *
 * <p>NAO cria emprestimos pre-feitos — o objetivo e testar o fluxo end-to-end
 * usando a UI (criar emprestimos manualmente via tela).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DadosExemploSeeder implements ApplicationRunner {

    private final LivroRepository livroRepository;
    private final AlunoRepository alunoRepository;

    @Value("${app.seed.dados-exemplo:false}")
    private boolean habilitado;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            return;
        }
        int livrosCriados = 0;
        int livrosPulados = 0;
        for (Livro l : LIVROS) {
            if (livroRepository.existsByIsbn(l.getIsbn())) {
                livrosPulados++;
            } else {
                livroRepository.save(l);
                livrosCriados++;
            }
        }
        int alunosCriados = 0;
        int alunosPulados = 0;
        for (Aluno a : ALUNOS) {
            if (alunoRepository.existsByMatricula(a.getMatricula())) {
                alunosPulados++;
            } else {
                alunoRepository.save(a);
                alunosCriados++;
            }
        }
        log.info("DadosExemploSeeder: livros criados={} pulados={}, alunos criados={} pulados={}",
            livrosCriados, livrosPulados, alunosCriados, alunosPulados);
    }

    // ============================================================================
    // Helpers
    // ============================================================================

    private static Livro livro(String titulo, String autor, String isbn, int ano, int exemplares) {
        return Livro.builder()
            .titulo(titulo)
            .autor(autor)
            .isbn(isbn)
            .ano(ano)
            .quantidadeExemplares(exemplares)
            .quantidadeDisponivel(exemplares)
            .build();
    }

    private static Aluno aluno(String matricula, String nome, String turma) {
        return Aluno.builder()
            .matricula(matricula)
            .nome(nome)
            .turma(turma)
            .build();
    }

    // ============================================================================
    // 80 LIVROS — classicos BR + internacional + infantojuvenil + best-seller + didatico + HQ
    // ISBNs com prefixo 9788999 pra nao conflitar com cadastro real (978-8999 e bloco raro)
    // ============================================================================
    private static final List<Livro> LIVROS = List.of(
        // Classicos brasileiros (20)
        livro("Dom Casmurro", "Machado de Assis", "9788999000001", 1899, 6),
        livro("Memorias Postumas de Bras Cubas", "Machado de Assis", "9788999000002", 1881, 4),
        livro("O Cortico", "Aluisio Azevedo", "9788999000003", 1890, 5),
        livro("Capitaes da Areia", "Jorge Amado", "9788999000004", 1937, 6),
        livro("Gabriela Cravo e Canela", "Jorge Amado", "9788999000005", 1958, 3),
        livro("Vidas Secas", "Graciliano Ramos", "9788999000006", 1938, 5),
        livro("Sao Bernardo", "Graciliano Ramos", "9788999000007", 1934, 3),
        livro("Iracema", "Jose de Alencar", "9788999000008", 1865, 4),
        livro("O Guarani", "Jose de Alencar", "9788999000009", 1857, 3),
        livro("Senhora", "Jose de Alencar", "9788999000010", 1875, 3),
        livro("Macunaima", "Mario de Andrade", "9788999000011", 1928, 4),
        livro("Triste Fim de Policarpo Quaresma", "Lima Barreto", "9788999000012", 1915, 3),
        livro("A Hora da Estrela", "Clarice Lispector", "9788999000013", 1977, 5),
        livro("A Paixao Segundo G.H.", "Clarice Lispector", "9788999000014", 1964, 2),
        livro("Grande Sertao Veredas", "Joao Guimaraes Rosa", "9788999000015", 1956, 3),
        livro("Sagarana", "Joao Guimaraes Rosa", "9788999000016", 1946, 2),
        livro("Quincas Borba", "Machado de Assis", "9788999000017", 1891, 3),
        livro("A Moreninha", "Joaquim Manuel de Macedo", "9788999000018", 1844, 4),
        livro("Helena", "Machado de Assis", "9788999000019", 1876, 3),
        livro("Os Sertoes", "Euclides da Cunha", "9788999000020", 1902, 2),

        // Literatura internacional (15)
        livro("1984", "George Orwell", "9788999000021", 1949, 6),
        livro("A Revolucao dos Bichos", "George Orwell", "9788999000022", 1945, 5),
        livro("O Pequeno Principe", "Antoine de Saint-Exupery", "9788999000023", 1943, 8),
        livro("Cem Anos de Solidao", "Gabriel Garcia Marquez", "9788999000024", 1967, 4),
        livro("O Amor nos Tempos do Colera", "Gabriel Garcia Marquez", "9788999000025", 1985, 3),
        livro("Crime e Castigo", "Fiodor Dostoievski", "9788999000026", 1866, 3),
        livro("Os Irmaos Karamazov", "Fiodor Dostoievski", "9788999000027", 1880, 2),
        livro("Guerra e Paz", "Liev Tolstoi", "9788999000028", 1869, 2),
        livro("Anna Karenina", "Liev Tolstoi", "9788999000029", 1877, 3),
        livro("O Velho e o Mar", "Ernest Hemingway", "9788999000030", 1952, 4),
        livro("Orgulho e Preconceito", "Jane Austen", "9788999000031", 1813, 5),
        livro("Persuasao", "Jane Austen", "9788999000032", 1817, 3),
        livro("O Apanhador no Campo de Centeio", "J.D. Salinger", "9788999000033", 1951, 4),
        livro("O Grande Gatsby", "F. Scott Fitzgerald", "9788999000034", 1925, 4),
        livro("Fahrenheit 451", "Ray Bradbury", "9788999000035", 1953, 3),

        // Infantojuvenil e fantasia (15)
        livro("Harry Potter e a Pedra Filosofal", "J.K. Rowling", "9788999000036", 1997, 7),
        livro("Harry Potter e a Camara Secreta", "J.K. Rowling", "9788999000037", 1998, 6),
        livro("Harry Potter e o Prisioneiro de Azkaban", "J.K. Rowling", "9788999000038", 1999, 6),
        livro("Harry Potter e o Calice de Fogo", "J.K. Rowling", "9788999000039", 2000, 5),
        livro("O Hobbit", "J.R.R. Tolkien", "9788999000040", 1937, 5),
        livro("O Senhor dos Aneis A Sociedade do Anel", "J.R.R. Tolkien", "9788999000041", 1954, 4),
        livro("As Cronicas de Narnia", "C.S. Lewis", "9788999000042", 1950, 5),
        livro("Percy Jackson e o Ladrao de Raios", "Rick Riordan", "9788999000043", 2005, 6),
        livro("Diario de um Banana", "Jeff Kinney", "9788999000044", 2007, 8),
        livro("A Menina que Roubava Livros", "Markus Zusak", "9788999000045", 2005, 4),
        livro("Extraordinario", "R.J. Palacio", "9788999000046", 2012, 5),
        livro("O Diario de Anne Frank", "Anne Frank", "9788999000047", 1947, 4),
        livro("Alice no Pais das Maravilhas", "Lewis Carroll", "9788999000048", 1865, 5),
        livro("Pinoquio", "Carlo Collodi", "9788999000049", 1883, 3),
        livro("O Magico de Oz", "L. Frank Baum", "9788999000050", 1900, 3),

        // Best-sellers contemporaneos (10)
        livro("O Alquimista", "Paulo Coelho", "9788999000051", 1988, 5),
        livro("O Codigo Da Vinci", "Dan Brown", "9788999000052", 2003, 3),
        livro("Jogos Vorazes", "Suzanne Collins", "9788999000053", 2008, 6),
        livro("A Culpa e das Estrelas", "John Green", "9788999000054", 2012, 5),
        livro("A Cabana", "William P. Young", "9788999000055", 2007, 3),
        livro("Sapiens Uma Breve Historia da Humanidade", "Yuval Noah Harari", "9788999000056", 2011, 3),
        livro("O Poder do Habito", "Charles Duhigg", "9788999000057", 2012, 3),
        livro("Mindset A Nova Psicologia do Sucesso", "Carol S. Dweck", "9788999000058", 2006, 2),
        livro("Garota Exemplar", "Gillian Flynn", "9788999000059", 2012, 3),
        livro("Admiravel Mundo Novo", "Aldous Huxley", "9788999000060", 1932, 3),

        // Didaticos e referencia (10)
        livro("Atlas Geografico Escolar", "IBGE", "9788999000061", 2023, 5),
        livro("Minidicionario Aurelio", "Aurelio Buarque de Holanda", "9788999000062", 2024, 8),
        livro("Gramatica Pratica da Lingua Portuguesa", "Cegalla", "9788999000063", 2022, 6),
        livro("Historia do Brasil", "Boris Fausto", "9788999000064", 2019, 4),
        livro("Pequeno Vocabulario da Lingua Portuguesa", "Academia Brasileira de Letras", "9788999000065", 2021, 5),
        livro("Fundamentos de Matematica Elementar", "Gelson Iezzi", "9788999000066", 2020, 4),
        livro("Biologia para o Ensino Medio", "Cesar e Sezar", "9788999000067", 2022, 4),
        livro("Fisica Conceitual", "Paul G. Hewitt", "9788999000068", 2015, 3),
        livro("Quimica para o Novo Ensino Medio", "Usberco e Salvador", "9788999000069", 2021, 3),
        livro("Filosofia Vida e Saber", "Maria Lucia de Arruda Aranha", "9788999000070", 2020, 2),

        // HQ e graphic novels (10)
        livro("Turma da Monica em Quadrinhos", "Mauricio de Sousa", "9788999000071", 2023, 6),
        livro("Cebolinha Almanaque", "Mauricio de Sousa", "9788999000072", 2022, 5),
        livro("Asterix o Gaules", "Rene Goscinny", "9788999000073", 1961, 3),
        livro("Tintim no Tibete", "Herge", "9788999000074", 1960, 3),
        livro("Mafalda Toda", "Quino", "9788999000075", 1973, 4),
        livro("Persepolis", "Marjane Satrapi", "9788999000076", 2000, 2),
        livro("Maus a Historia de um Sobrevivente", "Art Spiegelman", "9788999000077", 1986, 2),
        livro("Watchmen", "Alan Moore", "9788999000078", 1986, 2),
        livro("V de Vinganca", "Alan Moore", "9788999000079", 1988, 2),
        livro("Sandman", "Neil Gaiman", "9788999000080", 1989, 2)
    );

    // ============================================================================
    // 40 ALUNOS — 10 por turma (6A, 7B, 8C, 9A), matriculas 2026100..2026139
    // (a partir de 100 pra nao conflitar com testes manuais que usam 2026001..099)
    // ============================================================================
    private static final List<Aluno> ALUNOS = List.of(
        // Turma 6A
        aluno("2026100", "Ana Beatriz Silva Pereira", "6A"),
        aluno("2026101", "Bruno Henrique Souza Lima", "6A"),
        aluno("2026102", "Carolina Mendes Ribeiro", "6A"),
        aluno("2026103", "Diego Almeida Costa", "6A"),
        aluno("2026104", "Eduarda Fernandes Castro", "6A"),
        aluno("2026105", "Felipe Augusto Martins", "6A"),
        aluno("2026106", "Gabriela Cunha Barros", "6A"),
        aluno("2026107", "Henrique Oliveira Santos", "6A"),
        aluno("2026108", "Isabela Rocha Pinto", "6A"),
        aluno("2026109", "Joao Pedro Vieira Souza", "6A"),

        // Turma 7B
        aluno("2026110", "Larissa Moraes Cardoso", "7B"),
        aluno("2026111", "Matheus Carvalho Nunes", "7B"),
        aluno("2026112", "Nathalia Pires Melo", "7B"),
        aluno("2026113", "Otavio Ferreira Lopes", "7B"),
        aluno("2026114", "Patricia Gomes Teixeira", "7B"),
        aluno("2026115", "Rafael Costa Andrade", "7B"),
        aluno("2026116", "Sofia Lima Cavalcante", "7B"),
        aluno("2026117", "Thiago Mendonca Ramos", "7B"),
        aluno("2026118", "Valentina Araujo Freitas", "7B"),
        aluno("2026119", "Vinicius Borges Tavares", "7B"),

        // Turma 8C
        aluno("2026120", "Amanda Cristina Macedo", "8C"),
        aluno("2026121", "Bernardo Lacerda Antunes", "8C"),
        aluno("2026122", "Camila Duarte Rezende", "8C"),
        aluno("2026123", "Daniel Pacheco Bittencourt", "8C"),
        aluno("2026124", "Elisa Tavares Monteiro", "8C"),
        aluno("2026125", "Fernando Aguiar Mello", "8C"),
        aluno("2026126", "Giovanna Brito Sales", "8C"),
        aluno("2026127", "Hugo Pereira Vasconcelos", "8C"),
        aluno("2026128", "Iris Magalhaes Albuquerque", "8C"),
        aluno("2026129", "Julia Ramalho Sampaio", "8C"),

        // Turma 9A
        aluno("2026130", "Kaua Dantas Figueiredo", "9A"),
        aluno("2026131", "Leticia Bezerra Quintanilha", "9A"),
        aluno("2026132", "Marcos Cordeiro Linhares", "9A"),
        aluno("2026133", "Natalia Siqueira Marinho", "9A"),
        aluno("2026134", "Otavio Quintela Lobato", "9A"),
        aluno("2026135", "Priscila Cardoso Esteves", "9A"),
        aluno("2026136", "Rogerio Faria Beltrao", "9A"),
        aluno("2026137", "Sabrina Lima Tavares", "9A"),
        aluno("2026138", "Tiago Severino Albuquerque", "9A"),
        aluno("2026139", "Ursula Caetano Rocha", "9A")
    );
}
