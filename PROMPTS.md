# PROMPTS.md — Central de Senhas com gRPC (Grupo 5)

Grupo 5 — Trabalho 5: *Falhas, deadline e validação* (Jonas, Lucas e Luis).

Ferramenta usada: Claude (Claude Code), operando nos arquivos do projeto
dentro do NetBeans do Jonas, a partir dos slides da Aula 10 (PPTX) e de uma
tela do totem (`TelaTotem.form`/`.java`) que o grupo já tinha desenhado.

**Como trabalhamos:** cada resposta da IA foi lida linha por linha pelo
grupo antes de entrar no projeto — não copiamos nada sem entender. Quando
uma explicação não batia com a documentação oficial (ver seção de
referências, no final deste arquivo), ou quando a IA sugeria algo mais
complicado do que o material da aula pedia, pedíamos para simplificar ou
justificar com a fonte oficial antes de aceitar. O teste do Prompt 4, por
exemplo, só foi aceito depois que o próprio grupo conferiu o log da Central
rodando de verdade, não só a explicação em texto.

## Prompt 1 — montar o projeto base (passos 1 a 8 do PPTX)

> Sou estudante de Sistemas Distribuídos. Anexei os slides da aula
> (Aula10 - SPD - TRABALHO GRPC) e a tela do totem que já desenhei no
> NetBeans (`TelaTotem.form`/`.java`). Monta os dois projetos exatamente
> como os slides pedem — o `.proto`, os dois `pom.xml`, a classe da
> Central e o totem ligado ao gRPC — sem inventar nada que não esteja
> nos slides. Depois quero acrescentar o Trabalho 5 do nosso grupo
> (falhas, deadline e validação) da forma mais simples possível, no
> mesmo estilo, sem bibliotecas extras.

**O que foi pedido:** ler o PPTX "Aula10 - SPD - TRABALHO GRPC" para extrair
o conteúdo de todos os slides (contrato `.proto`, os três `pom.xml`, o
serviço da Central, o `main` do servidor e o código do totem), identificar
que o grupo é o Grupo 5 ("Falhas, deadline e validação") e montar os dois
projetos Maven/NetBeans (`ServidorAtendimentoRPC` e `ClienteAtendimentoRPC`)
exatamente como descrito, reaproveitando o `TelaTotem.form`/`.java` que já
existia, dentro da pasta do NetBeans do aluno (`SPD`).

**O que a IA respondeu/fez:**
- Extraiu o texto de todos os 35 slides do `.pptx` (via zip + regex no XML,
  já que não há leitor de `.pptx` binário disponível).
- Criou `ServidorAtendimentoRPC/` e `ClienteAtendimentoRPC/` com `pom.xml`
  (copiados dos slides 8–10, com `grpc.version=1.75.0`,
  `protobuf.version=3.25.5`), `src/main/proto/atendimento.proto` idêntico
  nos dois projetos (slide 4), `AtendimentoServiceImpl`/`ServidorAtendimentoRPC`
  (slides 13–15) e o `TelaTotem.java` ligado ao gRPC (slides 18–21), mantendo
  o `initComponents()` gerado pelo NetBeans intocado.
- Usou `maven.compiler.release=26`, lido de um `pom.xml` de um trabalho
  anterior do mesmo aluno no NetBeans (em vez do `25` do slide, que era só
  um exemplo) — é exatamente a orientação do próprio slide 8 ("troque pelo
  número que você anotou").
- Abriu os dois projetos no NetBeans e rodou "Clean and Build" nos dois
  (gera as classes do `.proto`; baixa as 4 dependências do gRPC de verdade).

**O que o grupo aceitou/mudou e por quê:**
- Aceito como está: é a transcrição direta dos slides, sem invenção de
  API — cada import e cada chamada (`ServerBuilder`, `ManagedChannelBuilder`,
  `BlockingStub`, etc.) está no PPTX da aula.
- Mudado: o pacote principal do cliente ganhou uma classe extra
  `ClienteAtendimentoRPC.java` (só com um `main` que abre o `TelaTotem`), para
  que o botão "Run" do projeto (que usa `exec.mainClass`) abra a tela sem
  precisar sempre dar "Run File" em `TelaTotem.java`. Isso não está no PPTX
  explicitamente, mas é consequência direta de como o NetBeans cria o
  projeto no passo 1 (slide 6) — decidimos manter os dois pontos de entrada.

## Prompt 2 — validação do pedido

> Só a validação por enquanto, do jeito mais simples possível: nome
> vazio tem que ser recusado pela Central antes de numerar, com o
> código `INVALID_ARGUMENT`. Mostra onde entra dentro de `pegarSenha` e
> explica cada linha nova em português, principalmente por que é
> `onError` e não `onNext`.

**O que foi pedido:** a verificação de nome vazio descrita acima.

## Prompt 3 — deadline e cancelamento

> Agora a parte de deadline e cancelamento, que é a nossa do Trabalho
> 5. Quero que a Central perceba quando o totem já desistiu de esperar
> — prazo esgotado ou conexão fechada — e pare de processar, sem
> terminar de numerar a senha. Usa o jeito mais simples e mais oficial
> do gRPC pra isso. Explica por que precisa simular um tempo de
> atendimento pra isso fazer sentido, e confirma na documentação
> oficial do gRPC, não só na sua resposta.

**O que foi pedido (resumo do slide 31, aplicado "da forma mais simples
possível, no mesmo estilo, sem bibliotecas extras"):** acrescentar, no lado
da Central, validação de nome vazio (`INVALID_ARGUMENT`) e uma verificação
de que o totem desistiu da chamada (deadline estourado), parando o
atendimento nesse caso; e no lado do totem, diferenciar os códigos de status
na mensagem de erro.

**O que a IA respondeu/fez:**
- Em `AtendimentoServiceImpl.pegarSenha`: antes de numerar, recusa nome
  vazio com `Status.INVALID_ARGUMENT.withDescription(...).asRuntimeException()`.
- Acrescentou `atenderComVerificacaoDeCancelamento`, que simula o tempo de
  atendimento em passos de 200 ms (constante `TEMPO_ATENDIMENTO_MS`) e, a
  cada passo, verifica `Context.current().isCancelled()`; se a chamada já
  foi cancelada (prazo do totem estourado, ou totem fechado), a Central para
  sem numerar e sem chamar `onNext`/`onCompleted`.
- Em `TelaTotem`: `PRAZO_SEGUNDOS` isolado como constante e um método
  `mensagemDeErro` que distingue `INVALID_ARGUMENT`, `DEADLINE_EXCEEDED` e
  `UNAVAILABLE`.
- Testou de ponta a ponta: subiu a Central de verdade (processo Java em
  segundo plano) e rodou um cliente de teste descartável chamando
  `PegarSenha` quatro vezes — nome normal, nome prioritário, nome vazio e
  prazo de 50 ms. Confirmou os 4 resultados (ver `README.md`) e no log da
  Central apareceu exatamente `Totem desistiu (Carla); atendimento
  interrompido antes de numerar.`, provando que o `isCancelled()` funcionou.

**O que o grupo recusou:** a primeira versão sugeria um
`ScheduledExecutorService` para repetir a verificação de cancelamento em
intervalos fixos. Recusamos — não está no material da aula, exigiria
entender *thread pool* e agendamento, e um laço simples com `Thread.sleep`
faz exatamente a mesma coisa sem precisar de nenhuma classe nova. Pedimos a
versão mais simples de novo, e foi essa que ficou no código.

**O que o grupo aceitou/mudou e por quê:**
- Aceito, depois de conferir no [grpc.io](https://grpc.io/docs/guides/cancellation/)
  que não era invenção da IA: a verificação por polling com
  `Context.current().isCancelled()` (em vez de callback) é o padrão oficial
  de cancelamento do gRPC para um handler síncrono (ver citação no final
  deste arquivo), e é a opção mais simples para um método unário como
  `PegarSenha`.
- Aceito: o atendimento simulado com `Thread.sleep` em passos pequenos — sem
  ele não haveria nada para cancelar antes do `onNext`, e não dava pra testar
  deadline/cancelamento num serviço que responde instantaneamente. O valor
  (1 segundo, passos de 200 ms) foi escolhido pequeno o bastante para não
  atrapalhar o uso normal do totem (prazo padrão de 5 s).
- Decisão do grupo: documentar no `README.md` a diferença entre "o totem
  desistiu durante a simulação de atendimento" (pego pelo `isCancelled()`,
  sem gerar senha) e "repetir o pedido depois do timeout gera duas senhas"
  (explicado abaixo, em Idempotência) — para deixar claro na apresentação
  que são dois efeitos relacionados, mas diferentes.

## Prompt 4 — testar de verdade, não confiar de olho

> Não quero só o código, quero ver rodando. Sobe a Central de verdade
> e chama `pegarSenha` com nome vazio, com nome preenchido e com um
> prazo bem curto. Me mostra a saída de cada chamada e confirma se bate
> com o que a documentação do gRPC descreve pra cada código de status,
> antes da gente aceitar qualquer linha.

**O que a IA respondeu/fez:** subiu a Central como processo Java real em
segundo plano (sem mock) e chamou `PegarSenha` quatro vezes a partir de
um cliente de teste descartável: nome normal, nome prioritário, nome
vazio e prazo de 50 ms. Os quatro resultados batem com a documentação
(ver tabela em `README.md`) e o log da Central mostrou exatamente
`Totem desistiu (Carla); atendimento interrompido antes de numerar.`,
confirmando que o `isCancelled()` funciona antes de aceitar o código.

**O que o grupo aceitou:** só aceitamos o trecho de deadline/cancelamento
depois de ver esse log — sem ele não tinha como confirmar que a Central
realmente parava de processar, e não só que o totem recebia o erro.

## Prompt 5 — a apresentação da aula

> Preciso de uma apresentação pra turma, de 30 a 40 minutos, no formato
> de aula mesmo — não um pitch de produto. Tem que ensinar a turma a
> fazer essa parte: contexto do sistema, o código passo a passo (onde
> mexer, o que cada linha faz), como testar, e por que isso importa de
> verdade (os códigos de status, por que repetir pode gerar duas
> senhas). Nada de visual com cara de IA — sem card genérico, sem ícone
> decorativo, sem gradiente sem função. Quero um design sério, de
> engenharia, com diagrama de verdade, não caixinha com seta de texto.

**O que a IA respondeu/fez:** pesquisou referências reais de design de
apresentação técnica (Tahta, Slidev, reveal.js, Marp) antes de montar
qualquer coisa, definiu uma paleta e uma escala tipográfica próprias
(serifada Fraunces + IBM Plex Sans, um único acento, cantos quase
retos) e reconstruiu as 20 telas com diagrama de sequência (SVG com
setas de verdade), linha do tempo técnica para o teste de deadline, e
os prints reais do NetBeans ampliados com destaque no que olhar. Depois
renderizou a apresentação inteira num navegador de verdade e revisou
tela por tela antes de considerar pronta.

**O que o grupo aceitou/mudou:** pedimos pra tirar o verde (lia como
"IA genérica") e trocar por uma paleta mais sóbria; pedimos telas
maiores e mais bem compostas quando a primeira versão ainda deixava
espaço vazio; aceitamos a reconstrução completa quando ficou claro que
ajustar tamanho e cor, sozinho, não resolvia o problema de fundo.

## Referências oficiais (confirmando cada explicação)

**Deadlines** — https://grpc.io/docs/guides/deadlines/
> "A deadline is used to specify a point in time past which a client is
> unwilling to wait for a response from a server."
Confirma: quem define e "é dono" do prazo é o **cliente** (o totem, via
`withDeadlineAfter`), não a Central.

**Cancelamento** — https://grpc.io/docs/guides/cancellation/
> "its server handler must periodically check if the RPC it is servicing
> has been cancelled and if it has, cease processing."
> "Deadline expiration and I/O errors also trigger cancellation."
Confirma exatamente o que `atenderComVerificacaoDeCancelamento` faz: checar
periodicamente e parar; e que o estouro do prazo é tratado como um
cancelamento (por isso o mesmo `isCancelled()` cobre os dois casos: prazo
esgotado e totem fechado no meio).

**Códigos de status** — https://grpc.io/docs/guides/status-codes/
> `INVALID_ARGUMENT` — "The client specified an invalid argument."
> `DEADLINE_EXCEEDED` — "The deadline expired before the operation could complete."
> `UNAVAILABLE` — "The service is currently unavailable."
Confirma o uso de cada código: `INVALID_ARGUMENT` no nome vazio,
`DEADLINE_EXCEEDED` quando o prazo do totem estoura, `UNAVAILABLE` quando a
Central está fora do ar.

**Básico do gRPC em Java (stubs, mensagens)** —
https://grpc.io/docs/languages/java/basics/ — usado para confirmar a forma
de criar o canal (`ManagedChannelBuilder`), o stub bloqueante
(`newBlockingStub`) e o padrão `newBuilder()...build()` das mensagens do
protobuf, usados sem alteração nos slides 18–21.
