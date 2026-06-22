package com.upi.bank.accountmapper.transport;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.MDC;

@GrpcGlobalServerInterceptor
public class GrpcTraceServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> TRACE_ID_HEADER =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall (
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        //1. Leemos la cabecera que nos mandó el switch
        String traceId = headers.get(TRACE_ID_HEADER);

        if (traceId == null) {
            //2. lo metemos en el MDC de nuestro hilo local
            MDC.put("traceId", traceId);
        }

        //3. Dejamps que la peticion continue (con un bloque try-finally para limpiar el MDC después)
        ServerCall.Listener<ReqT> serverCallListener = next.startCall(call, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(serverCallListener) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } finally {
                    //4. Limpiamos el MDC para evitar fugas de memoria
                    MDC.remove("traceId");
                }
            }
        };
    }
}
