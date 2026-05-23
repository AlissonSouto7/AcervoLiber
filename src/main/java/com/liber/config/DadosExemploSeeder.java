package com.liber.config;

import com.liber.entity.Aluno;
import com.liber.entity.Emprestimo;
import com.liber.entity.Livro;
import com.liber.entity.SituacaoEmprestimo;
import com.liber.repository.AlunoRepository;
import com.liber.repository.EmprestimoRepository;
import com.liber.repository.LivroRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Popula o banco com dados de exemplo realistas (livros classicos, alunos e
 * emprestimos) para desenvolvimento e testes.
 *
 * <p>Roda apenas se {@code app.seed.dados-exemplo=true} E as tabelas estiverem
 * vazias — idempotente e seguro para producao (desligado por padrao).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DadosExemploSeeder implements ApplicationRunner {

    private final LivroRepository livroRepository;
    private final AlunoRepository alunoRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final Clock clock;

    @Value("${app.seed.dados-exemplo:false}")
    private boolean habilitado;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            return;
        }
        if (livroRepository.count() > 0 || alunoRepository.count() > 0) {
            log.info("DadosExemploSeeder: ja existem dados — seeder ignorado.");
            return;
        }

        List<Livro> livros = livroRepository.saveAll(livrosExemplo());
        List<Aluno> alunos = alunoRepository.saveAll(alunosExemplo());
        List<Emprestimo> emprestimos = emprestimosExemplo(livros, alunos);
        emprestimoRepository.saveAll(emprestimos);
        livroRepository.saveAll(livros); // persiste o estoque decrementado pelos emprestimos ativos

        log.info("DadosExemploSeeder: {} livros, {} alunos e {} emprestimos criados.",
            livros.size(), alunos.size(), emprestimos.size());
    }

    private static List<Livro> livrosExemplo() {
        return List.of(
            livro("Dom Casmurro", "Machado de Assis", "9788535910663", 1899, 4),
            livro("O Cortico", "Aluisio Azevedo", "9788508133024", 1890, 3),
            livro("Capitaes da Areia", "Jorge Amado", "9788535914078", 1937, 5),
            livro("Vidas Secas", "Graciliano Ramos", "9788503012670", 1938, 3),
            livro("O Pequeno Principe", "Antoine de Saint-Exupery", "9788595081413", 1943, 6),
            livro("1984", "George Orwell", "9788535914849", 1949, 4),
            livro("A Revolucao dos Bichos", "George Orwell", "9788535909553", 1945, 3),
            livro("Harry Potter e a Pedra Filosofal", "J.K. Rowling", "9788532530783", 2000, 5),
            livro("A Menina que Roubava Livros", "Markus Zusak", "9788598078175", 2007, 3),
            livro("O Hobbit", "J.R.R. Tolkien", "9788595084742", 1937, 4),
            livro("Memorias Postumas de Bras Cubas", "Machado de Assis", "9788525406958", 1881, 2),
            livro("A Hora da Estrela", "Clarice Lispector", "9788532508119", 1977, 3),
            livro("Quarto de Despejo", "Carolina Maria de Jesus", "9788574801087", 1960, 3),
            livro("O Diario de Anne Frank", "Anne Frank", "9788501044457", 1947, 4),
            livro("Percy Jackson e o Ladrao de Raios", "Rick Riordan", "9788580573466", 2009, 5),
            livro("Iracema", "Jose de Alencar", "9788508162498", 1865, 2)
        );
    }

    private static List<Aluno> alunosExemplo() {
        return List.of(
            aluno("2026001", "Ana Beatriz Silva", "9A"),
            aluno("2026002", "Pedro Henrique Oliveira", "9A"),
            aluno("2026003", "Maria Eduarda Santos", "9B"),
            aluno("2026004", "Joao Gabriel Souza", "8A"),
            aluno("2026005", "Larissa Almeida Costa", "8B"),
            aluno("2026006", "Lucas Ferreira Lima", "7A"),
            aluno("2026007", "Julia Rodrigues", "7B"),
            aluno("2026008", "Gabriel Martins", "9A"),
            aluno("2026009", "Sophia Carvalho", "8A"),
            aluno("2026010", "Matheus Pereira", "7A"),
            aluno("2026011", "Beatriz Gomes", "6A"),
            aluno("2026012", "Enzo Ribeiro", "6B")
        );
    }

    private List<Emprestimo> emprestimosExemplo(List<Livro> livros, List<Aluno> alunos) {
        LocalDate hoje = LocalDate.now(clock);
        List<Emprestimo> lista = new ArrayList<>();
        // Atrasados (data de devolucao prevista no passado)
        lista.add(emprestimo(livros.get(0), alunos.get(0), hoje, 15, 7, false));
        lista.add(emprestimo(livros.get(2), alunos.get(3), hoje, 20, 10, false));
        lista.add(emprestimo(livros.get(5), alunos.get(6), hoje, 12, 7, false));
        // Devolucao proxima (0 a 2 dias)
        lista.add(emprestimo(livros.get(7), alunos.get(1), hoje, 6, 7, false));
        lista.add(emprestimo(livros.get(4), alunos.get(8), hoje, 7, 7, false));
        // Em dia (folga confortavel)
        lista.add(emprestimo(livros.get(9), alunos.get(2), hoje, 2, 14, false));
        lista.add(emprestimo(livros.get(14), alunos.get(4), hoje, 1, 10, false));
        lista.add(emprestimo(livros.get(8), alunos.get(5), hoje, 3, 10, false));
        lista.add(emprestimo(livros.get(2), alunos.get(7), hoje, 1, 14, false));
        // Ja devolvidos (compoem o historico)
        lista.add(emprestimo(livros.get(1), alunos.get(9), hoje, 30, 7, true));
        lista.add(emprestimo(livros.get(3), alunos.get(10), hoje, 25, 10, true));
        lista.add(emprestimo(livros.get(5), alunos.get(11), hoje, 40, 7, true));
        return lista;
    }

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

    private static Emprestimo emprestimo(Livro livro, Aluno aluno, LocalDate hoje,
                                         int diasAtras, int prazoDias, boolean devolvido) {
        LocalDate dataEmprestimo = hoje.minusDays(diasAtras);
        LocalDate prevista = dataEmprestimo.plusDays(prazoDias);
        if (!devolvido) {
            // Emprestimo ativo ocupa um exemplar
            livro.setQuantidadeDisponivel(livro.getQuantidadeDisponivel() - 1);
        }
        return Emprestimo.builder()
            .livro(livro)
            .aluno(aluno)
            .dataEmprestimo(dataEmprestimo)
            .prazoDias(prazoDias)
            .dataDevolucaoPrevista(prevista)
            .dataDevolucaoEfetiva(devolvido ? prevista.minusDays(1) : null)
            .situacao(devolvido ? SituacaoEmprestimo.DEVOLVIDO : SituacaoEmprestimo.ATIVO)
            .build();
    }
}
