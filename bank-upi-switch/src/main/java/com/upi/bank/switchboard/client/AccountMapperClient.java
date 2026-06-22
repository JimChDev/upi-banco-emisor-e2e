package com.upi.bank.switchboard.client;

import com.upi.bank.grpc.accountmapper.AccountMapperGrpcServiceGrpc;
import com.upi.bank.grpc.accountmapper.AccountResolveRequest;
import com.upi.bank.grpc.accountmapper.AccountResolveResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountMapperClient {

    private static final Logger log = LoggerFactory.getLogger(AccountMapperClient.class);
    //Busca en el properties el cliente llamado 'account-mapper'
    //Y crea un Stub del servicio remoto
    @GrpcClient("account-mapper")
    private AccountMapperGrpcServiceGrpc.AccountMapperGrpcServiceBlockingStub mapperStub;

    public void verifyVpa(String vpaToVerify){
        //System.out.println("[Switch] Intentando resolver VPA: " + vpaToVerify);
        log.info("[Switch] Intentando resolver VPA: " + vpaToVerify);
        try{
            //1. Armano el mensaje protobuf usando el patron builder
            AccountResolveRequest request = AccountResolveRequest.newBuilder()
                    .setVpa(vpaToVerify)
                    .build();
            //2. Hacemos la llamada de red gRPC al puerto 9090
            AccountResolveResponse response = mapperStub.mapAccount(request);

//            //3. Imprimir el éxito
//            System.out.println("[Switch] ¡Éxito! Cuenta encontrada: " + response.getAccountNumber());
//            System.out.println("[Switch] Estado de la cuenta: " + response.getStatus());
            log.info("[Switch] ¡Éxito! Cuenta encontrada: " + response.getAccountNumber());
            log.info("[Switch] Estado de la cuenta: " + response.getStatus());
        } catch(Exception ex){
            //si el puerto 9090 lanza un error (ej. Status.NOT_FOUND)
            System.out.println("[Switch] Error al resolver VPA: " + ex.getMessage());
        }
    }
}
