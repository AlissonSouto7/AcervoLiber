package com.liber.config;

import com.liber.entity.Aluno;
import com.liber.entity.Livro;
import com.liber.repository.AlunoRepository;
import com.liber.repository.LivroRepository;
import java.util.List;
import java.util.Map;
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

        // Sinopses curadas: aplica a livros que existem no banco e estao com sinopse
        // nula. Nao sobrescreve sinopse que ja foi preenchida (manualmente pelo bib
        // ou automaticamente pelo Google Books) — mesma regra do LivroService.definirSinopse.
        int sinopsesAplicadas = 0;
        for (Map.Entry<String, String> e : SINOPSES.entrySet()) {
            Livro l = livroRepository.findByIsbn(e.getKey()).orElse(null);
            if (l != null && l.getSinopse() == null) {
                l.setSinopse(e.getValue());
                livroRepository.save(l);
                sinopsesAplicadas++;
            }
        }

        log.info("DadosExemploSeeder: livros criados={} pulados={}, alunos criados={} pulados={}, sinopses aplicadas={}",
            livrosCriados, livrosPulados, alunosCriados, alunosPulados, sinopsesAplicadas);
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

    // ============================================================================
    // 80 SINOPSES — texto curado em PT-BR pra aplicar como fallback quando o
    // Google Books nao retorna description. Keyed por ISBN (alinhado com a lista
    // LIVROS acima). Linguagem simples, adequada a estudantes de 6o a 9o ano.
    // ============================================================================
    private static final Map<String, String> SINOPSES = Map.ofEntries(
        // Classicos brasileiros
        Map.entry("9788999000001", "Bento Santiago narra sua paixao por Capitu desde a infancia e a desconfianca que o consome anos depois, ao suspeitar de uma traicao que talvez so exista em sua imaginacao. Um dos maiores classicos da literatura brasileira."),
        Map.entry("9788999000002", "Bras Cubas, ja morto, narra sua propria vida sem se preocupar com aparencias. Com ironia e humor, Machado de Assis cria o 'defunto-autor' e revoluciona a literatura brasileira."),
        Map.entry("9788999000003", "Em um cortico carioca do seculo XIX, dezenas de moradores convivem com miseria, ambicao e paixao. Aluisio Azevedo retrata a influencia do ambiente sobre o destino dos personagens."),
        Map.entry("9788999000004", "Um grupo de meninos abandonados sobrevive nas ruas de Salvador, formando uma familia entre roubos, brigas e sonhos. Jorge Amado mostra com ternura a dura realidade da infancia pobre na Bahia."),
        Map.entry("9788999000005", "Em Ilheus dos anos 1920, o arabe Nacib se apaixona pela cozinheira retirante Gabriela, em meio as transformacoes politicas e sociais de uma cidade movida pelo cacau."),
        Map.entry("9788999000006", "Fabiano, Sinha Vitoria e seus dois filhos vagam pelo sertao fugindo da seca. Sem palavras suficientes para se expressar, sentem a injustica do mundo e sonham com uma vida melhor."),
        Map.entry("9788999000007", "O fazendeiro Paulo Honorio conta a historia de como construiu sua riqueza as custas de seu proprio carater, e como destruiu o que mais amava: a esposa Madalena."),
        Map.entry("9788999000008", "A india tabajara Iracema se apaixona pelo guerreiro portugues Martim, traindo seu povo. Romance fundador da identidade brasileira, escrito com linguagem poetica por Jose de Alencar."),
        Map.entry("9788999000009", "O indio goitaca Peri protege a familia do nobre portugues Dom Antonio de Mariz e vive um amor impossivel com a jovem Ceci. Aventura no Brasil colonial cheia de simbolismo romantico."),
        Map.entry("9788999000010", "Aurelia e abandonada pelo namorado por causa do dinheiro. Anos depois, rica e poderosa, ela compra o casamento com ele e impoe sua propria vinganca. Critica social e psicologica de Alencar."),
        Map.entry("9788999000011", "As aventuras delirantes de Macunaima, 'heroi sem nenhum carater', que sai da Amazonia para Sao Paulo em busca de um amuleto. Mario de Andrade mistura mito indigena, folclore e modernismo."),
        Map.entry("9788999000012", "Um patriota ingenuo tenta salvar o Brasil propondo o tupi como lingua oficial e cultivando lavouras improvaveis. A ironia de Lima Barreto sobre o nacionalismo da Primeira Republica."),
        Map.entry("9788999000013", "Macabea, datilografa pobre e sonhadora, vive sem perceber o quanto e invisivel. Clarice Lispector retrata com sensibilidade a vida de uma jovem nordestina perdida no Rio de Janeiro."),
        Map.entry("9788999000014", "Apos esmagar uma barata, a personagem G.H. mergulha em uma crise existencial profunda. Romance filosofico e introspectivo de Clarice Lispector sobre identidade, repulsa e revelacao."),
        Map.entry("9788999000015", "Riobaldo, ex-jagunco, narra suas aventuras pelo sertao e o pacto que pode ter feito com o diabo. Linguagem inovadora e sertao como universo simbolico na obra-prima de Guimaraes Rosa."),
        Map.entry("9788999000016", "Coletanea de nove contos ambientados no sertao mineiro, onde Guimaraes Rosa cria uma linguagem poetica inedita para falar de vaqueiros, juncoes, bichos e travessias."),
        Map.entry("9788999000017", "Rubiao herda a fortuna do filosofo Quincas Borba e se muda para o Rio, onde e envolvido em armadilhas pelo casal Palha. Analise machadiana da loucura, da ambicao e da hipocrisia."),
        Map.entry("9788999000018", "Estudantes de medicina passam um fim de semana numa ilha, onde o protagonista descobre que a 'moreninha' travessa e justamente seu antigo amor de infancia. Primeiro grande romance brasileiro."),
        Map.entry("9788999000019", "A jovem Helena chega a casa do conselheiro Vale como filha bastarda recem-reconhecida e provoca paixoes e segredos na familia. Romance romantico de Machado de Assis."),
        Map.entry("9788999000020", "Euclides da Cunha narra a Guerra de Canudos no sertao da Bahia, combinando jornalismo, ciencia e literatura para retratar a tragedia dos seguidores de Antonio Conselheiro."),

        // Literatura internacional
        Map.entry("9788999000021", "Em um futuro distopico, o Grande Irmao vigia tudo. Winston Smith trabalha reescrevendo a historia e comeca a se rebelar contra o regime totalitario. Classico de George Orwell."),
        Map.entry("9788999000022", "Os animais de uma fazenda expulsam o dono humano e tentam criar uma sociedade igualitaria — mas os porcos logo se tornam tao tiranos quanto os antigos donos. Satira politica de Orwell."),
        Map.entry("9788999000023", "Um piloto cai no deserto e conhece um menino vindo de outro planeta. Em dialogos simples sobre amor, amizade e responsabilidade, Saint-Exupery escreve um livro para todas as idades."),
        Map.entry("9788999000024", "A saga da familia Buendia em Macondo, uma cidade ficticia da Colombia, marcada por guerras, paixoes e misterios. Obra-prima do realismo magico de Garcia Marquez."),
        Map.entry("9788999000025", "Florentino espera 51 anos para reencontrar Fermina Daza, seu amor de juventude. Romance de Garcia Marquez sobre paixao, paciencia, velhice e o tempo que tudo transforma."),
        Map.entry("9788999000026", "O estudante Raskolnikov mata uma velha agiota acreditando estar acima da moral comum. A culpa, porem, o consome. Dostoievski explora o limite entre razao e consciencia."),
        Map.entry("9788999000027", "Tres irmaos muito diferentes lidam com a morte do pai, o velho Karamazov. Dostoievski mergulha em fe, duvida, justica, amor fraterno e a natureza do bem e do mal."),
        Map.entry("9788999000028", "Tolstoi narra cinco familias russas durante as guerras napoleonicas, alternando batalhas grandiosas e dramas pessoais. Considerado um dos maiores romances ja escritos."),
        Map.entry("9788999000029", "Anna deixa o marido e o filho para viver um romance proibido com o conde Vronski, enfrentando o julgamento da sociedade russa. Tolstoi retrata paixao, culpa e tragedia."),
        Map.entry("9788999000030", "O velho pescador Santiago enfrenta sozinho um peixe gigantesco no mar de Cuba. Hemingway constroi uma fabula sobre coragem, fracasso, dignidade e a luta contra o impossivel."),
        Map.entry("9788999000031", "Elizabeth Bennet e o orgulhoso Sr. Darcy se desentendem repetidas vezes ate reconhecerem o amor um pelo outro. Jane Austen retrata a sociedade inglesa do seculo XIX com ironia e graca."),
        Map.entry("9788999000032", "Anne Elliot, persuadida pela familia anos antes a rejeitar Frederick Wentworth, reencontra-o agora capitao da Marinha. Ultima obra de Austen, mais madura e melancolica."),
        Map.entry("9788999000033", "Holden Caulfield, adolescente expulso da escola, vaga por Nova York por alguns dias antes de voltar para casa. Salinger captura a angustia da juventude com voz inesquecivel."),
        Map.entry("9788999000034", "O misterioso milionario Jay Gatsby da festas suntuosas em Long Island, sempre na esperanca de reconquistar Daisy. Fitzgerald retrata o vazio do sonho americano nos anos 1920."),
        Map.entry("9788999000035", "Num futuro em que os livros sao proibidos e queimados, o bombeiro Montag comeca a duvidar de sua missao. Bradbury imagina uma sociedade que renunciou ao pensamento critico."),

        // Infantojuvenil e fantasia
        Map.entry("9788999000036", "Aos onze anos, Harry descobre que e bruxo e entra para a escola de Hogwarts, onde faz amigos e comeca a desvendar o misterio de uma pedra capaz de ressuscitar o terrivel Voldemort."),
        Map.entry("9788999000037", "No segundo ano em Hogwarts, alunos comecam a ser petrificados e uma camara lendaria parece ter sido reaberta. Harry precisa descobrir quem esta por tras dos misteriosos ataques."),
        Map.entry("9788999000038", "Sirius Black, considerado o pior criminoso do mundo bruxo, escapa da prisao e parece estar atras de Harry. Misterios sobre o passado de seus pais comecam a vir a tona."),
        Map.entry("9788999000039", "Hogwarts sedia o perigoso Torneio Tribruxo e Harry e misteriosamente escolhido como quarto campeao. O retorno do Lorde das Trevas se aproxima com forca total."),
        Map.entry("9788999000040", "Bilbo Bolseiro, um pequeno hobbit, e arrastado para uma aventura com treze anoes e o mago Gandalf, em busca do tesouro guardado pelo dragao Smaug. Prelude ao Senhor dos Aneis."),
        Map.entry("9788999000041", "Frodo recebe a missao de destruir o Um Anel e parte de sua terra natal acompanhado de uma sociedade formada por homens, anoes, elfos e hobbits. Inicio da maior saga de fantasia."),
        Map.entry("9788999000042", "Quatro irmaos entram por um guarda-roupa num mundo magico chamado Narnia, onde precisam ajudar o leao Aslan a vencer a Feiticeira Branca. Classico de C.S. Lewis."),
        Map.entry("9788999000043", "Percy descobre ser filho do deus grego Poseidon e precisa devolver o raio roubado de Zeus antes que estoure uma guerra entre os deuses do Olimpo. Mitologia + aventura adolescente."),
        Map.entry("9788999000044", "Greg Heffley, garoto inseguro do ensino fundamental, conta em desenhos e textos suas trapalhadas diarias na escola, em casa e com os amigos. Humor que conquistou milhoes de leitores."),
        Map.entry("9788999000045", "Na Alemanha nazista, a pequena Liesel descobre o poder das palavras roubando livros, enquanto seus pais adotivos escondem um judeu no porao. Narrado pela propria Morte."),
        Map.entry("9788999000046", "Auggie Pullman nasceu com uma deformidade facial e vai pela primeira vez a escola comum. R.J. Palacio mostra como pequenas atitudes mudam vidas e ensina o valor da gentileza."),
        Map.entry("9788999000047", "Diario verdadeiro de uma adolescente judia que ficou escondida com a familia em Amsterda durante a ocupacao nazista. Documento historico comovente sobre esperanca, medo e crescimento."),
        Map.entry("9788999000048", "Alice persegue um coelho de colete e cai num mundo de personagens estranhos: a Rainha de Copas, o Gato de Cheshire, o Chapeleiro Maluco. Fantasia classica de Lewis Carroll."),
        Map.entry("9788999000049", "O boneco de madeira esculpido pelo carpinteiro Gepeto ganha vida e quer ser um menino de verdade — mas precisa aprender a nao mentir e a fazer escolhas certas. Classico italiano."),
        Map.entry("9788999000050", "A menina Dorothy e levada por um furacao para o mundo de Oz e segue pela estrada de tijolos amarelos em busca do magico que pode manda-la para casa, com novos amigos pelo caminho."),

        // Best-sellers contemporaneos
        Map.entry("9788999000051", "O jovem pastor andaluz Santiago atravessa o deserto em busca de um tesouro, descobrindo no caminho que o verdadeiro tesouro pode estar em outro lugar. Fabula de Paulo Coelho."),
        Map.entry("9788999000052", "O simbologista Robert Langdon investiga um assassinato no Louvre e descobre uma trama envolvendo a Igreja, sociedades secretas e os quadros de Leonardo da Vinci. Best-seller de Dan Brown."),
        Map.entry("9788999000053", "Em um futuro distopico, jovens sao sorteados para uma luta televisionada ate a morte. Katniss Everdeen se voluntaria no lugar da irma e vira simbolo de resistencia ao regime."),
        Map.entry("9788999000054", "Hazel e Augustus se conhecem em um grupo de apoio para adolescentes com cancer e vivem um romance intenso. John Green escreve sobre vida, morte e amor adolescente com sensibilidade."),
        Map.entry("9788999000055", "Apos a tragedia que destruiu sua familia, Mack recebe um misterioso convite para uma cabana onde reencontra a fe e enfrenta a propria dor. Romance espiritual de William P. Young."),
        Map.entry("9788999000056", "Yuval Noah Harari conta a historia da humanidade em linguagem acessivel, mostrando como os humanos passaram de uma especie irrelevante a dominantes do planeta. Best-seller global."),
        Map.entry("9788999000057", "Charles Duhigg explica como os habitos se formam no cerebro e como mudalos pode transformar vidas, empresas e sociedades inteiras. Ciencia e historias reais juntas."),
        Map.entry("9788999000058", "Carol Dweck mostra a diferenca entre mentalidade fixa e mentalidade de crescimento e como acreditar no proprio desenvolvimento muda a relacao com aprendizado e desafios."),
        Map.entry("9788999000059", "No dia do aniversario de casamento, Amy desaparece. Seu marido Nick vira o principal suspeito enquanto o leitor descobre que nada e o que parece. Thriller psicologico de Gillian Flynn."),
        Map.entry("9788999000060", "Numa sociedade futurista onde as pessoas sao produzidas em laboratorio e mantidas felizes com remedios e prazer, um homem do passado comeca a questionar tudo. Distopia de Aldous Huxley."),

        // Didaticos e referencia
        Map.entry("9788999000061", "Atlas oficial do IBGE com mapas fisicos, politicos, climaticos e demograficos do Brasil e do mundo. Material de referencia para os estudos de Geografia no ensino fundamental e medio."),
        Map.entry("9788999000062", "Dicionario compacto da lingua portuguesa com cerca de 30 mil verbetes, ideal para consulta rapida em sala de aula e em casa. Inclui novos termos e atualizacoes ortograficas."),
        Map.entry("9788999000063", "Manual de gramatica do professor Cegalla, com explicacoes claras e muitos exercicios sobre as regras da lingua portuguesa. Referencia para vestibular e ENEM."),
        Map.entry("9788999000064", "Visao geral da historia brasileira do periodo colonial a atualidade, escrita pelo historiador Boris Fausto em linguagem acessivel. Excelente para alunos e curiosos sobre o Brasil."),
        Map.entry("9788999000065", "Vocabulario ortografico oficial da Academia Brasileira de Letras, referencia para a grafia correta das palavras em portugues. Util para tirar duvidas rapidas de escrita."),
        Map.entry("9788999000066", "Colecao classica de Gelson Iezzi, cobrindo algebra, geometria, trigonometria e demais conteudos do ensino medio com teoria detalhada e muitos exercicios resolvidos."),
        Map.entry("9788999000067", "Livro didatico completo de Biologia, abordando citologia, genetica, evolucao, ecologia e os demais conteudos do ensino medio, alinhado as exigencias do ENEM."),
        Map.entry("9788999000068", "Paul Hewitt apresenta a fisica com foco em conceitos e exemplos do dia a dia, sem peso em equacoes. Excelente para uma introducao acessivel a materia."),
        Map.entry("9788999000069", "Colecao didatica de Usberco e Salvador atualizada para a BNCC, com quimica geral, fisico-quimica e organica em linguagem clara e exercicios para fixacao."),
        Map.entry("9788999000070", "Manual de filosofia para o ensino medio, com os principais filosofos e correntes em linguagem didatica. Inclui temas de etica, politica, estetica e teoria do conhecimento."),

        // HQ e graphic novels
        Map.entry("9788999000071", "Aventuras da Monica, Cebolinha, Magali e Cascao em historinhas classicas de Mauricio de Sousa. Ideais para alfabetizacao e primeiras leituras independentes."),
        Map.entry("9788999000072", "Almanaque tematico com gibis do Cebolinha, jogos, passatempos e curiosidades. Diversao garantida da equipe da Mauricio de Sousa Producoes."),
        Map.entry("9788999000073", "As aventuras do pequeno guerreiro Asterix e seu amigo Obelix na aldeia gaulesa que resiste aos romanos com a ajuda de uma pocao magica. Classico das HQs europeias."),
        Map.entry("9788999000074", "O reporter Tintim parte para o Himalaia para resgatar um amigo desaparecido apos um acidente de aviao. Uma das aventuras mais emocionantes e humanas criadas por Herge."),
        Map.entry("9788999000075", "Reuniao das tirinhas da menina argentina mais famosa: critica, esperta e preocupada com o mundo. Humor afiado de Quino que continua atual decadas depois."),
        Map.entry("9788999000076", "Marjane Satrapi conta em quadrinhos sua infancia no Ira durante a Revolucao Islamica e a guerra com o Iraque. Autobiografia em preto e branco que viajou o mundo."),
        Map.entry("9788999000077", "Art Spiegelman narra a experiencia de seu pai como judeu poloneses no Holocausto, retratando nazistas como gatos e judeus como ratos. HQ ganhadora do premio Pulitzer."),
        Map.entry("9788999000078", "Em um mundo alternativo dos anos 1980, super-herois aposentados investigam o assassinato de um ex-colega. Alan Moore reinventa o genero com profundidade e critica politica."),
        Map.entry("9788999000079", "Num futuro fascista britanico, o misterioso V luta contra o regime usando mascara, explosivos e teatralidade. Alan Moore escreve uma das HQs mais influentes sobre liberdade."),
        Map.entry("9788999000080", "Sonho, um dos sete Perpetuos, escapa de um carcere onde ficou preso por 70 anos e precisa reconstruir seu reino. Saga de Neil Gaiman que mistura mitologia, sonhos e literatura.")
    );
}
