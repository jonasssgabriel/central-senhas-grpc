package com.mycompany.clienteatendimentorpc;

import java.awt.EventQueue;

/**
 * Ponto de entrada do projeto do totem. Dá "Run" no projeto (ou "Run File"
 * em TelaTotem.java) para abrir a tela.
 */
public class ClienteAtendimentoRPC {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new TelaTotem().setVisible(true));
    }
}
