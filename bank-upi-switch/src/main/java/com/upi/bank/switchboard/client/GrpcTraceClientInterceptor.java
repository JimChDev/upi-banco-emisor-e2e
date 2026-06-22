package com.upi.bank.switchboard.client;

import io.grpc.*;
import io.grpc.Metadata;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.slf4j.MDC;

import java.util.UUID;

@GrpcGlobalClientInterceptor //la magia: se aplcia a TODAS las llamadas
public class GrpcTraceClientInterceptor implements ClientInterceptor {

    //Definimos la llave exacta de la cebecera
    private static final Metadata.Key<String> TRACE_ID_HEADER =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                      CallOptions callOptions,
                                                      Channel next) {
        //Crear un nuevo ClientCall que envuelva el original
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                //1. Generamos o recuperamos el TraceID
                String traceId = MDC.get("traceId");
                if (traceId == null) {
                    traceId = UUID.randomUUID().toString();
                    MDC.put("traceId", traceId);//Lo guardamos nuestros propios logs
                }

                //2. Agregamos el TraceID a las cabeceras gRPC
                headers.put(TRACE_ID_HEADER, traceId);
                super.start(responseListener, headers);
            }
        };
    }
}
