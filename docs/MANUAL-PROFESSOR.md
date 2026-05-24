# Manual do AcervoLiber

**Sistema de gestão da biblioteca escolar**

Versão 1.0 — para bibliotecários(as), administradores e professores

---

## Índice

1. [O que é o AcervoLiber](#1-o-que-é-o-acervoliber)
2. [Como acessar](#2-como-acessar)
3. [Primeiro acesso e troca de senha](#3-primeiro-acesso-e-troca-de-senha)
4. [Tela inicial — Dashboard](#4-tela-inicial--dashboard)
5. [Cadastro de livros](#5-cadastro-de-livros)
6. [Cadastro de alunos](#6-cadastro-de-alunos)
7. [Empréstimos](#7-empréstimos)
8. [Reservas](#8-reservas)
9. [Histórico de empréstimos](#9-histórico-de-empréstimos)
10. [Como o aluno usa o sistema](#10-como-o-aluno-usa-o-sistema)
11. [Configurações da conta](#11-configurações-da-conta)
12. [Gestão de usuários (somente administrador)](#12-gestão-de-usuários-somente-administrador)
13. [Perguntas frequentes](#13-perguntas-frequentes)
14. [Em caso de problema](#14-em-caso-de-problema)

---

## 1. O que é o AcervoLiber

O AcervoLiber é o sistema online da biblioteca da escola. Ele substitui a planilha e o caderno de empréstimos por uma tela simples acessível pelo celular ou pelo computador. Com ele você consegue:

- Cadastrar livros e alunos
- Registrar empréstimos e devoluções em poucos cliques
- Ver quem está em atraso
- Saber quais livros são os mais procurados
- Receber reservas online dos alunos
- Manter um histórico completo de tudo que aconteceu

O sistema roda na nuvem (na internet) e os dados ficam salvos em servidor seguro. Não é preciso instalar nada — só abrir o navegador.

### Quem usa o sistema

Existem **três tipos de usuário**:

| Tipo            | O que faz                                                                                     |
|-----------------|-----------------------------------------------------------------------------------------------|
| **Administrador**  | Tudo do bibliotecário + cria/desativa contas de outros funcionários                          |
| **Bibliotecário(a)** | Cadastra livros e alunos, registra empréstimos e devoluções, confirma reservas             |
| **Aluno(a)**       | Navega no catálogo, reserva livros, vê seus empréstimos ativos                                |

Este manual é voltado aos **bibliotecários e administradores**. A seção 10 explica brevemente como o aluno usa.

---

## 2. Como acessar

Abra o navegador (Chrome, Edge, Firefox, Safari) e digite:

> **https://acervoliber.duckdns.org**

Você verá a tela de login. Use o e-mail e a senha que receberam da direção/coordenação.

Se for **aluno**, ele entra por **matrícula**, não por e-mail (mais sobre isso na seção 10).

### Dica importante

- O sistema funciona muito bem no celular. Pode usar normalmente do telefone, é a mesma tela adaptada.
- Salve o link nos favoritos do navegador.

---

## 3. Primeiro acesso e troca de senha

Quando o administrador cria sua conta, ele define uma **senha provisória**. No primeiro login o sistema vai pedir que você troque por uma senha pessoal.

**Regras da senha:**

- Mínimo **10 caracteres**
- A nova senha não pode ser igual à atual
- Recomendamos: misture letras maiúsculas, minúsculas, números e algum símbolo

**Importante:** ao trocar a senha, **todas as suas sessões abertas serão encerradas** (no celular, no computador da escola, em qualquer lugar). Isso é proposital — é uma medida de segurança caso alguém tenha pego sua senha antiga.

Se você esquecer a senha, peça ao administrador para resetá-la. Ele consegue criar uma nova provisória pra você.

---

## 4. Tela inicial — Dashboard

Logo após o login você cai no **Dashboard**. Ele mostra um resumo da biblioteca:

- **Total de livros, alunos e empréstimos ativos** (cartões superiores)
- **Empréstimos atrasados** (em destaque vermelho — ação prioritária!)
- **Próximos a vencer** (lista amarela, pra você cobrar antes do prazo estourar)
- **Top livros mais emprestados** (ranking)

Use essa tela como ponto de partida do seu dia. Se houver alunos com livro atrasado, eles aparecem ali com nome, turma e quantos dias de atraso.

---

## 5. Cadastro de livros

### 5.1 Acessando a tela

No menu lateral, clique em **Livros**. Você vê todos os livros do acervo em formato de cards (com a capa, título, autor, ano e quantos exemplares estão disponíveis).

### 5.2 Buscar um livro

Use a barra **Buscar** no topo da tela. Pode buscar por:

- Título (ex: "Dom Casmurro")
- Autor (ex: "Machado de Assis")
- ISBN (código de barras do livro)

A busca é instantânea.

### 5.3 Adicionar um novo livro

Clique em **+ Novo livro** no topo direito. Preencha:

| Campo              | Obrigatório? | Detalhes                                                                                   |
|--------------------|:------------:|--------------------------------------------------------------------------------------------|
| **Título**         | Sim          | Como aparece na capa do livro.                                                              |
| **Autor**          | Sim          | Nome completo, ex: "Jorge Amado".                                                           |
| **ISBN**           | Não          | Código de 10 ou 13 dígitos atrás do livro. Ajuda muito a achar a capa correta.             |
| **Ano**            | Não          | Ano da publicação (1000 a 9999).                                                            |
| **Quantidade**     | Sim          | Quantos exemplares físicos a biblioteca tem.                                                |
| **Capa**           | Não          | O sistema tenta achar a capa automaticamente pela internet. Você pode subir uma também.    |
| **Sinopse**        | Não          | Texto curto descrevendo o livro. Se você não escrever, o sistema tenta buscar sozinho.      |

Clique em **Salvar** e pronto.

### 5.4 Sobre a capa do livro

O sistema busca a capa em duas fontes online: **Google Books** e **Open Library**. A maioria dos livros conhecidos aparece em alguns segundos. Mas pode acontecer de não achar:

- Se você tem o ISBN correto, a chance é altíssima.
- Para livros antigos, didáticos ou edições nacionais raras, às vezes nenhuma fonte tem capa. Nesse caso o sistema mostra uma **capa "estilizada"** colorida com o título escrito — fica bonita e funcional.
- Você pode subir uma imagem própria pelo botão **Enviar imagem própria** (JPG, PNG ou WEBP, até 2 MB). Quando você sobe manualmente, o sistema **não sobrescreve** depois.

### 5.5 Sobre a sinopse

A sinopse aparece quando o aluno clica em "Ver detalhes" no catálogo. O sistema tenta buscar uma do Google Books automaticamente, mas você pode escrever a sua. **A sinopse manual ganha** — uma vez que você escreveu, o automático nunca substitui.

Se preferir o resumo do Google Books mas o que veio não te agrada, basta apagar o campo e salvar — o sistema vai tentar de novo no próximo ciclo.

### 5.6 Editar ou remover um livro

- **Editar**: clique no ícone de lápis no card do livro. Mude o que precisar e salve.
- **Remover**: clique no ícone de lixeira. O sistema **não permite remover** livros que têm empréstimos no histórico (pra preservar o registro de quem pegou o quê). Também não remove se houver reservas pendentes.

### 5.7 Atenção: reduzir exemplares

Se você diminuir a **Quantidade** de exemplares de um livro, o sistema **não deixa** baixar abaixo do que está em uso (empréstimos ativos + reservas pendentes). Exemplo: se há 3 alunos com o livro emprestado e 1 reserva, o mínimo permitido é 4. Isso evita estoque negativo.

---

## 6. Cadastro de alunos

### 6.1 Acessando

Menu lateral → **Alunos**. Você vê todos os alunos cadastrados, com matrícula, nome, turma e quantos livros eles têm emprestados no momento.

### 6.2 Adicionar um aluno

Clique em **+ Novo aluno** e preencha:

| Campo          | Detalhes                                                                              |
|----------------|---------------------------------------------------------------------------------------|
| **Matrícula**  | Identificador único do aluno na escola. Não pode repetir.                              |
| **Nome**       | Nome completo do aluno (como deve aparecer no boletim).                                |
| **Turma**      | Ex: "6A", "9B". Use o padrão que a escola já adota.                                    |

### 6.3 Criar acesso de login pro aluno

Por padrão, alunos cadastrados **não conseguem entrar no sistema**. Isso é proposital — só os que a escola autorizar terão login. Para criar o acesso:

1. Na tela de Alunos, clique no aluno
2. Clique em **Criar acesso ao sistema**
3. Defina uma senha provisória (mínimo 10 caracteres)
4. Anote essa senha e entregue ao aluno

No primeiro login ele será obrigado a trocar pela senha pessoal dele.

### 6.4 Auto-cadastro pelos alunos

A escola pode escolher um modelo **mais aberto**: você cadastra a matrícula + nome do aluno na tela de Alunos, e o próprio aluno cria sua senha sozinho pela tela inicial do sistema (link **Sou aluno — primeiro acesso**). O sistema valida:

- A matrícula precisa existir no cadastro
- O nome digitado pelo aluno precisa bater com o nome cadastrado (ignora acentos e maiúsculas)
- O aluno ainda não pode ter acesso criado

É a forma mais prática se a escola tem muitos alunos. O risco é mínimo porque o nome funciona como "senha" do auto-cadastro.

### 6.5 Editar nome do aluno

Se o aluno reclamar que o nome está errado (acento faltando, sobrenome incompleto), **só o bibliotecário pode corrigir**. O aluno não consegue mudar o próprio nome dentro do sistema — é o nome oficial da escola.

### 6.6 Remover um aluno

Mesmo regra dos livros: alunos com empréstimos no histórico não podem ser removidos. Se precisar "desligar" um aluno, peça ao administrador para **desativar** a conta de usuário dele (seção 12).

---

## 7. Empréstimos

### 7.1 Registrar um empréstimo manual (no balcão)

Quando o aluno chega no balcão com o livro:

1. Menu lateral → **Empréstimos**
2. Clique em **+ Novo empréstimo**
3. **Livro**: comece a digitar o título ou autor; o sistema sugere
4. **Aluno**: comece a digitar nome, matrícula ou turma
5. **Prazo de devolução (dias)**: padrão 7, pode ajustar (1 a 30 dias)
6. Clique em **Registrar empréstimo**

O estoque do livro é descontado automaticamente. O sistema calcula a data prevista de devolução.

### 7.2 Regras automáticas

- **Aluno em atraso** com algum livro **não consegue pegar novo** (no balcão nem no portal). Ele precisa devolver o atrasado primeiro.
- **Limite de 3 livros simultâneos** por aluno: já valendo no portal de reservas do aluno. **No balcão (lançamento manual pelo bibliotecário/admin), esse limite NÃO bloqueia** — a direção pode liberar exceção pra alunos de confiança ou projetos especiais. Se você lançar um quarto livro pra um aluno, o sistema não reclama.

### 7.3 Registrar devolução

Na tela de **Empréstimos** (ou **Histórico**), encontre o empréstimo e clique em **Devolver**. O sistema:

- Marca o empréstimo como DEVOLVIDO
- Devolve o exemplar ao estoque
- Registra a data da devolução

### 7.4 Renovar empréstimo

Se o aluno quer ficar mais dias com o livro, na lista de empréstimos ativos clique em **Renovar**. Você escolhe quantos dias renovar (1 a 30). **Limite: 2 renovações por empréstimo**. Após isso, o aluno deve devolver e pegar de novo se quiser continuar.

A renovação **não é permitida** se há reservas pendentes para o mesmo livro — ou seja, se tem alguém na fila esperando, o aluno tem que devolver.

### 7.5 Editar um empréstimo (correção de erro)

Lançou empréstimo com data errada? Prazo errado? Clique em **Editar** no empréstimo. Você pode mudar:

- Data do empréstimo (não pode ser no futuro)
- Prazo em dias

A data prevista de devolução é recalculada automaticamente.

### 7.6 Cancelar um empréstimo

Se o lançamento foi totalmente errado (livro errado, aluno errado), clique em **Cancelar**. O sistema:

- Marca o empréstimo como CANCELADO (não volta a ser ATIVO)
- Devolve o exemplar ao estoque
- Fica preservado no histórico (não some)

Use cancelamento só para **erros de lançamento**. Para devoluções normais, use **Devolver**.

### 7.7 Cores de urgência

Na lista, cada empréstimo tem uma cor que indica a urgência:

| Cor          | O que significa                                        |
|--------------|--------------------------------------------------------|
| 🟢 Verde      | Tudo certo, falta tempo até o vencimento               |
| 🟡 Amarelo    | Faltam até 2 dias pro vencimento — pode cobrar         |
| 🔴 Vermelho   | Já passou do vencimento — cobrança urgente             |

---

## 8. Reservas

Reservas acontecem quando o **aluno** quer um livro que **está disponível** e prefere garantir a retirada na biblioteca.

### 8.1 Fluxo da reserva

1. Aluno entra no catálogo dele, escolhe o livro e clica em **Reservar**
2. O sistema **segura um exemplar** (desconta do estoque imediatamente)
3. Você (bibliotecário) vê a reserva na tela **Reservas**
4. Quando o aluno aparece para retirar, você **confirma** → vira empréstimo
5. Se o aluno não aparecer dentro do prazo (padrão **3 dias**), a reserva **expira automaticamente** e o exemplar volta pro estoque

### 8.2 Confirmar uma reserva (aluno retirou)

Menu lateral → **Reservas**. Clique em **Confirmar** ao lado da reserva.

- Defina o **prazo de devolução** (padrão 7 dias)
- Clique em **Confirmar e gerar empréstimo**

Pronto — o aluno está com o livro registrado normalmente.

### 8.3 Recusar uma reserva

Se por algum motivo a reserva não pode ser confirmada (livro danificado, aluno bloqueado, etc.), clique em **Recusar**. O exemplar volta ao estoque.

### 8.4 Reserva expira sozinha

O sistema verifica diariamente se há reservas vencidas e marca como EXPIRADAS, devolvendo o exemplar. Você não precisa fazer nada.

### 8.5 O aluno pode cancelar a própria reserva

Sim. Na tela "Minhas reservas" do portal do aluno (seção 10), ele pode cancelar reservas pendentes. O exemplar volta pro estoque automaticamente.

---

## 9. Histórico de empréstimos

Menu lateral → **Histórico**. Aqui você vê **todos os empréstimos passados** — ativos, devolvidos, cancelados.

Filtros disponíveis:
- Por situação (Ativo, Devolvido, Cancelado)
- Por aluno (busca por nome ou matrícula)
- Por livro

Útil pra:
- Conferir se um aluno realmente devolveu um livro semanas atrás
- Auditar empréstimos antes do recesso escolar
- Gerar relatórios manuais (no momento sem exportação PDF/CSV — em desenvolvimento)

---

## 10. Como o aluno usa o sistema

Vale você conhecer o que o aluno vê, pra orientar quando tiverem dúvida.

### 10.1 Login do aluno

Tela inicial → o aluno entra na aba **Aluno** e usa **matrícula + senha** (não e-mail).

Se ele nunca usou e a escola optou pelo auto-cadastro: ele clica em **Sou aluno — primeiro acesso**, digita matrícula + nome completo (precisa bater com o cadastro) + cria a senha dele.

### 10.2 Catálogo

Tela **Catálogo**: cards com a capa, título, autor e quantidade disponível de cada livro. O aluno pode:

- Buscar por título, autor ou ISBN
- Clicar em **Ver detalhes** → modal com capa grande, dados do livro, sinopse e botão de reservar
- Reservar diretamente do card (botão **Reservar**)

### 10.3 Minhas reservas

Tela onde o aluno vê suas reservas pendentes. Pode cancelar enquanto não foi confirmada.

### 10.4 Limites

O aluno pode ter no máximo **3 livros simultâneos** entre empréstimos ativos e reservas pendentes. Se ele tem 2 livros emprestados e 1 reservado, atingiu o teto. Para reservar outro, precisa devolver algum.

(Lembrete: o bibliotecário/admin **pode estourar esse limite no balcão**. É exceção, não rotina.)

### 10.5 Configurações do aluno

O aluno pode mudar a **própria senha**. Ele **não pode** mudar o próprio nome (é o nome oficial da escola).

---

## 11. Configurações da conta

Menu lateral inferior → **Configurações**.

### 11.1 Dados da conta

- **E-mail**: somente leitura — só o administrador altera.
- **Nome**: você (bibliotecário/admin) pode editar.

### 11.2 Alterar senha

Você precisa informar:
- **Senha atual**
- **Nova senha** (mínimo 10 caracteres, diferente da atual)
- **Confirmar nova senha**

Após salvar, **todas as suas sessões são encerradas** e você precisa entrar de novo com a nova senha.

---

## 12. Gestão de usuários (somente administrador)

Esta seção só aparece pra usuários do tipo **Administrador**. Menu lateral → **Usuários**.

### 12.1 Criar conta de bibliotecário

Clique em **+ Novo usuário**. Preencha e-mail, nome, senha provisória e o tipo (ADMIN ou BIBLIOTECÁRIO). Você não pode criar contas tipo ALUNO por aqui — alunos têm um caminho próprio (seção 6.3).

### 12.2 Desativar um usuário

Clique no usuário → **Desativar**. Efeito imediato:

- Todas as sessões dele são encerradas
- Ele não consegue mais entrar
- Os registros de empréstimos antigos continuam intactos

**Proteções automáticas:**
- Você **não pode** se desativar (evita lockout acidental)
- O sistema **não permite** desativar o **último administrador ativo** — sempre tem que sobrar pelo menos 1

### 12.3 Reativar

Clique em **Ativar** no usuário desativado. Ele volta a entrar normalmente (com a última senha que ele tinha).

---

## 13. Perguntas frequentes

**P: Esqueci minha senha. E agora?**
R: Peça ao administrador da escola para resetar. Ele entra na tela de Usuários, abre sua conta e cria uma senha provisória nova. Você troca no primeiro login.

**P: Posso usar pelo celular?**
R: Sim, tudo funciona. A tela se adapta automaticamente.

**P: O aluno reclama que não tem o livro X no catálogo, mas eu sei que cadastrei.**
R: Veja se o livro está ativo e se a quantidade disponível é > 0. Pode estar tudo emprestado. Confira na tela de **Livros**.

**P: Um livro está com a capa errada/feia.**
R: Edite o livro e use **Enviar imagem própria**. A capa manual nunca é substituída pela automática.

**P: A capa do livro não apareceu. E sobre a sinopse?**
R: Algumas obras antigas/regionais não estão indexadas no Google Books ou Open Library. O sistema mostra uma capa estilizada com o título. Você pode subir uma capa real ou aceitar a estilizada — fica bonita. Para sinopse, idem: pode escrever uma manualmente.

**P: Meu aluno pegou 3 livros e quer um quarto. O sistema não deixa pelo portal.**
R: Correto, esse é o limite normal. Mas você (bibliotecário) **pode lançar manualmente** o quarto pela tela de Empréstimos → Novo empréstimo. É uma exceção autorizada (alunos de projeto, leitura especial, etc.).

**P: Apaguei sem querer um livro/aluno. Como recuperar?**
R: Não é possível recuperar pela tela. Mas o sistema não permite apagar livros/alunos com histórico, então se foi apagado é porque não tinha movimento. Cadastre de novo.

**P: Como vejo todos os livros que um aluno já pegou?**
R: Tela **Histórico**, filtre pelo nome ou matrícula do aluno.

**P: O internet caiu, posso usar offline?**
R: Não. O sistema precisa de internet — fica numa nuvem. Caso a internet caia, registre os empréstimos no papel e lance assim que voltar.

**P: Quantos alunos e livros o sistema aguenta?**
R: Sem problema pra escolas pequenas e médias (até alguns milhares de alunos e livros). Foi pensado pra escola pública de bairro.

---

## 14. Em caso de problema

### O sistema não abre

1. Confirme que está digitando o endereço certo: `https://acervoliber.duckdns.org`
2. Tente em outro navegador (Chrome / Edge / Firefox)
3. Tente do celular — se abrir no celular e não no computador, é problema do computador
4. Se nada funciona, é provável que o servidor esteja fora do ar. Aguarde alguns minutos e tente de novo.

### Erro ao salvar / mensagem em vermelho

Leia a mensagem de erro — quase sempre ela explica o problema:
- "ISBN já cadastrado" → você está tentando criar livro duplicado
- "Aluno possui livros em atraso" → o aluno precisa devolver antes
- "Quantidade não pode ser menor que..." → você está reduzindo estoque abaixo do que está em uso

Se a mensagem for genérica ("Erro inesperado"), recarregue a página (F5) e tente de novo.

### Aluno reclama que não consegue entrar

1. Confirme que a matrícula está cadastrada
2. Confirme que ele tem **acesso ao sistema** criado (tela de Alunos)
3. Se ele esqueceu a senha, peça a um administrador para resetar
4. Verifique se a conta dele não está **desativada** (tela de Usuários)

### Cobrança de livro atrasado

A tela **Dashboard** mostra todos os atrasados. Você pode anotar nome/turma e cobrar pessoalmente, ou pedir ao professor da turma para avisar.

---

**Sistema desenvolvido especialmente para a Escola Gabriel José Pereira — Eunápolis/BA**

Em caso de dúvidas técnicas (problemas no servidor, erros que se repetem, sugestões de melhoria), entre em contato com o responsável técnico do projeto.

*Versão deste manual: 1.0*
