package com.group16.aetherxmlbridge.service;

import org.springframework.stereotype.Service;

@Service
public class PhoneMaskingService {

    public String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "Not set";
        }

        String digits = phoneNumber.replaceAll("[^0-9+]", "");

        if (!digits.startsWith("+") || digits.length() < 5) {
            return phoneNumber;
        }

        String countryCode;
        String localDigits;

        if (digits.startsWith("+1") && digits.length() >= 12) {
            countryCode = "+1";
            localDigits = digits.substring(2);
        } else if (digits.startsWith("+44") && digits.length() >= 12) {
            countryCode = "+44";
            localDigits = digits.substring(3);
        } else if (digits.startsWith("+7") && digits.length() >= 12) {
            countryCode = "+7";
            localDigits = digits.substring(2);
        } else if (digits.startsWith("+49")) {
            countryCode = "+49";
            localDigits = digits.substring(3);
        } else if (digits.startsWith("+33")) {
            countryCode = "+33";
            localDigits = digits.substring(3);
        } else if (digits.startsWith("+34")) {
            countryCode = "+34";
            localDigits = digits.substring(3);
        } else if (digits.startsWith("+39")) {
            countryCode = "+39";
            localDigits = digits.substring(3);
        } else if (digits.startsWith("+91")) {
            countryCode = "+91";
            localDigits = digits.substring(3);
        } else if (digits.startsWith("+86")) {
            countryCode = "+86";
            localDigits = digits.substring(3);
        } else if (digits.startsWith("+972")) {
            countryCode = "+972";
            localDigits = digits.substring(4);
        } else {
            return phoneNumber;
        }

        if (localDigits.length() < 4) {
            return countryCode + " ****";
        }

        String lastFour = localDigits.substring(localDigits.length() - 4);
        return countryCode + " (***) ***-" + lastFour;
    }
}