package com.smartexpense.server.service;

import com.smartexpense.server.dto.SmsTemplateResponse;
import com.smartexpense.server.dto.SmsTransactionRequest;
import com.smartexpense.server.dto.SmsTransactionResponse;
import java.util.List;

public interface SmsService {
    List<SmsTemplateResponse> getActiveTemplates();
    List<SmsTransactionResponse> syncSmsTransactions(String userEmail, List<SmsTransactionRequest> requests);
}
