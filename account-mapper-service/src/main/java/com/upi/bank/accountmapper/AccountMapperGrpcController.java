package com.upi.bank.accountmapper;

import com.upi.bank.grpc.accountmapper.AccountMapperGrpcServiceGrpc;
import com.upi.bank.grpc.accountmapper.AccountResolveRequest;
import com.upi.bank.grpc.accountmapper.AccountResolveResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class AccountMapperGrpcController extends AccountMapperGrpcServiceGrpc.AccountMapperGrpcServiceImplBase {
    private final AccountMapperService logicService;

    // Inyección de dependencias por constructor. Conectamos la red con el cerebro.
    public AccountMapperGrpcController(AccountMapperService logicService) {
        this.logicService = logicService;
    }

    @Override
    public void mapAccount(AccountResolveRequest request, StreamObserver<AccountResolveResponse> responseObserver){
     try{
         //Extraemos el VPA del mensaje Protobuf entrante
         String vpa = request.getVpa();
         System.out.println("[gRPC] Petición recibida para resolver VPA: " + vpa);

         //2. delegamos el trabajo pesado a nuestra capa dominio
         AccountInfo accountInfo = logicService.resolveAccount(vpa);

         //3. Enviamos la respuesta binaria usando el patron builder de Protobuf
         AccountResolveResponse response = AccountResolveResponse.newBuilder()
                 .setAccountNumber(accountInfo.accountNumber())
                 .setStatus(accountInfo.Status())
                 .build();

         //4. Enviamos la respuesta exitosa a traves del flujo asincrono
         responseObserver.onNext(response);
         responseObserver.onCompleted();

     } catch(IllegalArgumentException e){
         //BUENA PRACTICA UPI: Jamas lanzamos un NullPointerException o RuntimeException crudo
         //Traducimos excepciones de negocio a ódigos de Estado gRPC universales
         responseObserver.onError(Status.NOT_FOUND
                 .withDescription(e.getMessage())
                 .asRuntimeException());
     } catch (IllegalStateException e){
         //Cuenta congelada = Permiso denegado
         responseObserver.onError(Status.PERMISSION_DENIED
                 .withDescription(e.getMessage())
                 .asRuntimeException());
     } catch (Exception e){
         //Errores inesperados = Error Interno
         //Fallback general para errores no controlados. Evitamos exponer detalles internos al cliente.
         responseObserver.onError(Status.INTERNAL
                 .withDescription("Error crítico interno en Account Mapper")
                 .asRuntimeException());
     }
    }
}
