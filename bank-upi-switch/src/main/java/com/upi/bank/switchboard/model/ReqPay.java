package com.upi.bank.switchboard.model;

public record ReqPay(
        String payerVpa, //quien envia el dinero - juan@sbi
        String payeeVpa, //quien recibe el dinero - comercio@hdfc
        double amount,   //cantidad
        String deviceId  //Para el futuro módulo device binding (antifraude)
){}
