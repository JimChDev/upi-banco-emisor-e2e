package com.upi.bank.accountmapper.model;


import java.io.Serializable;

// Al implementar Serializable, permitimos que
// Spring convierta este objeto a bytes para Redis
public record AccountInfo(String vpa, String accountNumber, String Status) implements Serializable {

}