#!/usr/bin/env bash
# =============================================================================
# seed-teste.sh — popula o AcervoLiber com dados de teste via API REST.
# Idempotente o suficiente para rodar com o sistema já no ar (porta 8080).
# Uso: bash seed-teste.sh
# =============================================================================
set -u
API="${API:-http://localhost:8080/api/v1}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@liber.local}"
ADMIN_PASS="${ADMIN_PASS:-@Admin2026}"

# Extrai o valor de uma chave string de um JSON simples (1º match).
val() { grep -o "\"$1\":[^,}]*" | head -1 | sed "s/\"$1\"://; s/^\"//; s/\"$//"; }

POST() { curl -s -X POST "$API$1" -H 'Content-Type: application/json' -H "Authorization: Bearer $2" -d "$3"; }
GET()  { curl -s "$API$1" -H "Authorization: Bearer $2"; }

echo "==> Login admin"
TOKEN=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"senha\":\"$ADMIN_PASS\"}" | val accessToken)
[ -z "$TOKEN" ] && { echo "FALHA no login admin"; exit 1; }
echo "    OK"

# ---------------------------------------------------------------- Bibliotecários
echo "==> Criando bibliotecários"
for u in \
  "Carla Bibliotecária|carla.bib@liber.local|BIBLIOTECARIO" \
  "Roberto Acervo|roberto.bib@liber.local|BIBLIOTECARIO" \
  "Fernanda Leitura|fernanda.bib@liber.local|BIBLIOTECARIO" \
  "Diretor Admin|diretor@liber.local|ADMIN" ; do
  IFS='|' read -r nome email role <<< "$u"
  r=$(POST /usuarios "$TOKEN" "{\"nome\":\"$nome\",\"email\":\"$email\",\"senha\":\"Liber2026@bib\",\"role\":\"$role\"}")
  echo "    $email -> $(echo "$r" | val id || echo "$r" | head -c 80)"
done

# ----------------------------------------------------------------------- Livros
echo "==> Criando livros"
BOOK_IDS=()
while IFS='|' read -r titulo autor isbn ano qtd; do
  [ -z "$titulo" ] && continue
  r=$(POST /livros "$TOKEN" "{\"titulo\":\"$titulo\",\"autor\":\"$autor\",\"isbn\":\"$isbn\",\"ano\":$ano,\"quantidadeExemplares\":$qtd}")
  id=$(echo "$r" | val id)
  [ -n "$id" ] && BOOK_IDS+=("$id")
  echo "    $titulo -> ${id:-$(echo "$r" | head -c 80)}"
done <<'EOF'
O Senhor dos Aneis|J.R.R. Tolkien|9788595084759|1954|3
Crime e Castigo|Fiodor Dostoievski|9788573264326|1866|2
A Moreninha|Joaquim Manuel de Macedo|9788508177516|1844|4
Senhora|Jose de Alencar|9788508162474|1875|3
O Guarani|Jose de Alencar|9788526017351|1857|3
Grande Sertao Veredas|Joao Guimaraes Rosa|9788535914863|1956|2
Macunaima|Mario de Andrade|9788535921113|1928|3
Os Lusiadas|Luis de Camoes|9788508084234|1572|2
A Moreninha 2|Autor Teste|9788500000017|2020|5
O Alienista|Machado de Assis|9788594318600|1882|4
Triste Fim de Policarpo Quaresma|Lima Barreto|9788508137497|1915|3
O Cortico Anotado|Aluisio Azevedo|9788500000024|2019|2
Sagarana|Joao Guimaraes Rosa|9788501114013|1946|2
A Cidade e as Serras|Eca de Queiros|9788525410313|1901|3
O Primo Basilio|Eca de Queiros|9788508162467|1878|3
Helena|Machado de Assis|9788594318617|1876|3
Til|Jose de Alencar|9788500000031|1872|2
Ubirajara|Jose de Alencar|9788500000048|1874|2
A Escrava Isaura|Bernardo Guimaraes|9788508084241|1875|4
Inocencia|Visconde de Taunay|9788508084258|1872|3
Esau e Jaco|Machado de Assis|9788594318624|1904|2
Memorial de Aires|Machado de Assis|9788594318631|1908|2
Lucio Flavio|Autor Teste|9788500000055|2021|5
Diario de um Banana|Jeff Kinney|9788598078182|2007|6
EOF
echo "    total de livros novos: ${#BOOK_IDS[@]}"

# ----------------------------------------------------------------------- Alunos
echo "==> Criando alunos"
STU_IDS=(); STU_MAT=()
i=100
for nome in \
  "Bruno Teixeira Alves" "Camila Nunes Dias" "Daniel Rocha Pinto" "Elisa Moraes Cunha" \
  "Felipe Barros Castro" "Gabriela Pires Melo" "Heitor Campos Reis" "Isabela Freitas Lopes" \
  "Joao Vitor Mendes" "Karina Ramos Teles" "Leonardo Cardoso Sa" "Mariana Vieira Brito" \
  "Nicolas Araujo Pena" "Olivia Tavares Maia" "Paulo Cesar Fonseca" "Rafaela Duarte Nobre" \
  "Samuel Antunes Goes" "Tatiana Borges Lira" "Vitor Hugo Macedo" "Yasmin Correia Paz" ; do
  mat="202700$i"; turma=$(( (i % 4) + 6 ))"$( [ $((i%2)) -eq 0 ] && echo A || echo B )"
  r=$(POST /alunos "$TOKEN" "{\"matricula\":\"$mat\",\"nome\":\"$nome\",\"turma\":\"$turma\"}")
  id=$(echo "$r" | val id)
  if [ -n "$id" ]; then STU_IDS+=("$id"); STU_MAT+=("$mat"); fi
  echo "    $mat $nome ($turma) -> ${id:-$(echo "$r" | head -c 80)}"
  i=$((i+1))
done
echo "    total de alunos novos: ${#STU_IDS[@]}"

# ------------------------------------------------ Acessos de login para 6 alunos
echo "==> Criando acessos de login para alunos"
STU_TOKENS=()
PROV="Provisoria2026@x"; FINAL="AlunoLiber2026@"
for n in 0 1 2 3 4 5; do
  id="${STU_IDS[$n]}"; mat="${STU_MAT[$n]}"
  POST "/alunos/$id/acesso" "$TOKEN" "{\"senhaInicial\":\"$PROV\"}" >/dev/null
  # login com senha provisória
  prov_tok=$(curl -s -X POST "$API/auth/login-aluno" -H 'Content-Type: application/json' \
    -d "{\"matricula\":\"$mat\",\"senha\":\"$PROV\"}" | val accessToken)
  # troca de senha obrigatória
  curl -s -X POST "$API/auth/change-password" -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $prov_tok" \
    -d "{\"senhaAtual\":\"$PROV\",\"senhaNova\":\"$FINAL$mat\"}" >/dev/null
  # login final
  fin_tok=$(curl -s -X POST "$API/auth/login-aluno" -H 'Content-Type: application/json' \
    -d "{\"matricula\":\"$mat\",\"senha\":\"$FINAL$mat\"}" | val accessToken)
  STU_TOKENS+=("$fin_tok")
  echo "    aluno $mat -> login ok (senha: $FINAL$mat)"
done

# ------------------------------------------------------------------ Empréstimos
echo "==> Criando empréstimos (via admin)"
for n in 0 1 2 3 4 5 6 7; do
  liv="${BOOK_IDS[$n]}"; alu="${STU_IDS[$((n+6))]}"; prazo=$(( (n % 3) * 7 + 7 ))
  r=$(POST /emprestimos "$TOKEN" "{\"livroId\":$liv,\"alunoId\":$alu,\"prazoDias\":$prazo}")
  echo "    livro $liv -> aluno $alu (prazo $prazo) -> ${r:0:60}"
done

# --------------------------------------------------------------------- Reservas
echo "==> Criando reservas (alunos reservam livros)"
for n in 0 1 2 3 4 5; do
  tok="${STU_TOKENS[$n]}"; liv="${BOOK_IDS[$((n+10))]}"
  [ -z "$tok" ] && continue
  r=$(POST /reservas "$tok" "{\"livroId\":$liv}")
  echo "    aluno[$n] reservou livro $liv -> ${r:0:60}"
done

echo ""
echo "==> Concluído. Credenciais de teste:"
echo "    Bibliotecário: carla.bib@liber.local / Liber2026@bib"
echo "    Admin extra  : diretor@liber.local   / Liber2026@bib"
echo "    Alunos       : matrícula 2027000100..2027000105 / AlunoLiber2026@<matricula>"
