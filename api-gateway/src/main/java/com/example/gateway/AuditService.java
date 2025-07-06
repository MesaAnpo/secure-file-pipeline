package com.example.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger auditLogger = LoggerFactory.getLogger("audit");

    public void log(String message) {
        auditLogger.info(message);
    }
}
