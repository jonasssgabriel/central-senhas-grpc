package com.mycompany.servidoratendimentorpc;

import com.mycompany.atendimentorpc.AtendimentoServiceGrpc;
import com.mycompany.atendimentorpc.PedidoSenha;
import com.mycompany.atendimentorpc.Senha;
import com.mycompany.atendimentorpc.TipoAtendimento;
import io.grpc.Context;             // Trabalho 5: Context.current() dá acesso ao isCancelled() da chamada
import io.grpc.Status;               // Trabalho 5: Status.INVALID_ARGUMENT monta o erro de validação
import io.grpc.stub.StreamObserver;

/**
 * Implementação do serviço da Central de Senhas.
 *
 * Trabalho 5 (Jonas, Lucas e Luis) — Falhas, deadline e validação:
 *  - valida o pedido antes de numerar (nome vazio -&gt; INVALID_ARGUMENT);
 *  - simula um atendimento que leva um tempo, verificando a cada passo se
 *    o totem já desistiu (prazo esgotado do lado do cliente ou chamada
 *    cancelada), para a Central não numerar nem responder em vão.
 */
public class AtendimentoServiceImpl
        extends AtendimentoServiceGrpc.AtendimentoServiceImplBase {

    // Trabalho 5: tempo de "atendimento" simulado, em passos pequenos. Serve
    // para testar DEADLINE_EXCEEDED e o isCancelled(): aumente este valor
    // (ou reduza o prazo do totem em TelaTotem.java) para reproduzir a
    // sequência de testes.
    private static final long TEMPO_ATENDIMENTO_MS = 1_000; // Trabalho 5: duração total simulada (1s)
    private static final long PASSO_VERIFICACAO_MS = 200;   // Trabalho 5: de quanto em quanto tempo verifica isCancelled()

    // Estado compartilhado: todos os totens usam o mesmo contador
    private int proximoNumero = 1;

    // synchronized: dois totens nunca recebem o mesmo número
    private synchronized int gerarNumero() {
        return proximoNumero++;
    }

    @Override
    public void pegarSenha(PedidoSenha req, StreamObserver<Senha> observador) {
        // ===== Jonas explica este trecho na apresentação (validação do nome) =====
        // Trabalho 5 — validação: nome vazio é recusado antes de qualquer processamento
        if (req.getNome() == null || req.getNome().trim().isEmpty()) {
            // ===== Lucas explica a partir daqui (o erro de validação) =====
            // Trabalho 5: null acontece só em teoria (protobuf nunca devolve null em string),
            // mas trim().isEmpty() cobre nome vazio ou só com espaços
            observador.onError(Status.INVALID_ARGUMENT
                    // Trabalho 5: descrição explica o motivo da recusa pro totem mostrar
                    .withDescription("O nome não pode ser vazio.")
                    // Trabalho 5: onError (não onNext) é como se "lança uma exceção" pro cliente
                    .asRuntimeException());
            return; // Trabalho 5: sai sem numerar, sem onCompleted — a chamada já terminou no onError
        }

        // ===== Lucas explica este trecho também (deadline e cancelamento) =====
        // Trabalho 5 — deadline/cancelamento: só numera se o totem ainda estiver esperando
        if (!atenderComVerificacaoDeCancelamento(req)) {
            // Trabalho 5: o totem já desistiu (prazo esgotado ou chamada cancelada):
            // a Central para, sem numerar e sem chamar onNext/onCompleted.
            System.out.println("Totem desistiu (" + req.getNome()
                    + "); atendimento interrompido antes de numerar.");
            return; // Trabalho 5: encerra o método aqui — não gasta um número à toa
        }

        int numero = gerarNumero();
        String prefixo = req.getTipo() == TipoAtendimento.PRIORITARIO
                ? "P" : "N";
        String codigo = String.format("%s-%03d", prefixo, numero);
        String mensagem = req.getNome() + ", sua senha é " + codigo
                + " (totem: " + req.getLocal() + ")";
        System.out.println("Senha " + codigo + " para " + req.getNome()
                + " no totem " + req.getLocal());

        Senha resposta = Senha.newBuilder()
                .setNumero(numero)
                .setCodigo(codigo)
                .setMensagem(mensagem)
                .build();
        observador.onNext(resposta);   // envia a resposta ao totem
        observador.onCompleted();      // encerra a chamada
    }

    /**
     * Simula o tempo de atendimento em passos pequenos, verificando a cada
     * passo se o contexto da chamada já foi cancelado (o totem desistiu,
     * seja por prazo esgotado, seja por ter sido fechado). Retorna false
     * assim que percebe o cancelamento, para a Central deixar de trabalhar
     * numa chamada que já não tem ninguém do outro lado esperando.
     */
    private boolean atenderComVerificacaoDeCancelamento(PedidoSenha req) {
        long restante = TEMPO_ATENDIMENTO_MS;        // Trabalho 5: quanto ainda falta "atender", em ms
        while (restante > 0) {                        // Trabalho 5: repete até zerar o tempo simulado
            if (Context.current().isCancelled()) {    // Trabalho 5: pergunta ao gRPC se o totem já desistiu
                return false;                          // Trabalho 5: desistiu -> avisa o chamador e para aqui
            }
            long passo = Math.min(PASSO_VERIFICACAO_MS, restante); // Trabalho 5: não dorme mais que o restante
            try {
                Thread.sleep(passo);                   // Trabalho 5: simula trabalho real acontecendo
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();    // Trabalho 5: boa prática ao capturar InterruptedException
                return false;                          // Trabalho 5: thread interrompida -> trata como desistência
            }
            restante -= passo;                         // Trabalho 5: desconta o passo do tempo que falta
        }
        return !Context.current().isCancelled();        // Trabalho 5: checagem final, já no fim do tempo simulado
    }
}
