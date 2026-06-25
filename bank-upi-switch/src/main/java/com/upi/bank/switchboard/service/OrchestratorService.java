package com.upi.bank.switchboard.service;

import com.upi.bank.switchboard.client.AccountMapperClient;
import com.upi.bank.switchboard.model.ReqPay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorService {
    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);
    private final AccountMapperClient accountMapperClient;

    //Inyectamos nuestro cliente gRPC
    public OrchestratorService(AccountMapperClient accountMapperClient) {
        this.accountMapperClient = accountMapperClient;
    }

    public String processPayment(ReqPay request){
        log.info("Iniciando flujo de orquestación UPI para pago de {} soles", request.amount());

        //1. Validar que el VPA del pagados existe y está activo
        log.info("Validando VPA del remitente: {}", request.payerVpa());
        accountMapperClient.verifyVpa(request.payerVpa());

        //2. Validar que el VPA del cobrador existe y está activo
        log.info("Validando VPA del remitente: {}", request.payeeVpa());
        accountMapperClient.verifyVpa(request.payeeVpa());

        //si gRPC lanza un error (NOT_FOUND o PERMISSION_DENIED), la ejecución se corta
        //y saltará al Controller, Si llega hasta aquí, ambos VPA son válidos

        log.info("Validación de VPAs completada. Listo para debitar fondos");
        return "Transacción aceptada preliminarmente (Fase de ruteo exitosa)";
    }
}
