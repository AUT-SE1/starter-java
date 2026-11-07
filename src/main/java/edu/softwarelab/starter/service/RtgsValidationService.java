package edu.softwarelab.starter.service;

import edu.softwarelab.starter.dto.RtgsRequest;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class RtgsValidationService {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("10000"); // ۱۰ هزار ریال
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("500000000"); // ۵۰۰ میلیون ریال

    public void validate(RtgsRequest request) {
        if (request.amount() == null || request.amount().compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("مبلغ تراکنش کمتر از حد مجاز است.");
        }

        if (request.amount().compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("مبلغ تراکنش بیشتر از سقف مجاز RTGS است.");
        }

        if (!isValidIban(request.sourceIban()) || !isValidIban(request.destinationIban())) {
            throw new IllegalArgumentException("شماره شبای مبدأ یا مقصد نامعتبر است.");
        }
    }

    private boolean isValidIban(String iban) {
        return iban != null && iban.matches("^IR\\d{24}$");
    }
}