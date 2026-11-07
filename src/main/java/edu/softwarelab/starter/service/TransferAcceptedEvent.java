package edu.softwarelab.starter.service;

import java.util.UUID;

/** بعد از ثبت تراکنش در DB منتشر می‌شود و فقط بعد از commit به موتور می‌رسد. */
public record TransferAcceptedEvent(UUID transferId, String settlementType) {
}
