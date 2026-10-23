package com.mycompany.servidoratendimentorpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServidorAtendimentoRPC {

    public static void main(String[] args) throws Exception {
        Server servidor = ServerBuilder.forPort(50051)
                .addService(new AtendimentoServiceImpl())
                .build()
                .start();
        System.out.println("Central de Senhas no ar (porta 50051).");
        servidor.awaitTermination();
    }
}
