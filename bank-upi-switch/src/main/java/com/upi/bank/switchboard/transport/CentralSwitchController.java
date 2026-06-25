package com.upi.bank.switchboard.transport;

import com.upi.bank.switchboard.model.ReqPay;
import com.upi.bank.switchboard.service.OrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upi")
public class CentralSwitchController {
    private static final Logger log = LoggerFactory.getLogger(CentralSwitchController.class);
    private final OrchestratorService orchestratorService;

    public CentralSwitchController(OrchestratorService orchestratorService){
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/pay")
    public ResponseEntity<String> handlePaymentRequest(@RequestBody ReqPay reqPay){
        //1. Simulación de AWS APU GATEWAY: Generamos el TraceID semilla para toda la transacción
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        log.info("**NUEVA PETICIÓN RECIBIDA DESDE APP MOVIL**");
        log.info("Payload: {}", reqPay);

        try{
            //2. Delegamos el trabajo al orquestador
            String result = orchestratorService.processPayment(reqPay);
            return ResponseEntity.ok(result);
        }catch (Exception e){
            log.error("Transacción fallida en el Orchestrator: {}", e.getMessage());
            //En producción aqui devolveriamos un JSON de error ISO 20022
            return ResponseEntity.badRequest().body("Error: "+ e.getMessage());
        }finally {
            //3. REGLA DE ORO: Siempre limpiar el MDC para no contaminar otras peticiones
            MDC.clear();
            log.info("**FIN DE PETICIÓN**");
        }
    }
}
