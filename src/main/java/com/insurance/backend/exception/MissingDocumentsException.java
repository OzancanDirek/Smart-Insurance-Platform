package com.insurance.backend.exception;

import java.util.List;

public class MissingDocumentsException extends RuntimeException
{
    public MissingDocumentsException(List<String> missingDocs)
    {
        super("Eksik belgeler: " + String.join(", ", missingDocs));
    }
}