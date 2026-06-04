package com.insurance.backend.claim.enums;

public enum ClaimStatus
{
    DRAFT, //musteri butun belgeleri yükleyip gönderene kadar
    PENDING,//gonderdiginde
    IN_REVIEW, //staff incelemeye alınca
    APPROVED, //manager onaylanınca
    REJECTED //manager red
}
