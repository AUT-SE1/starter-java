package edu.softwarelab.starter.service;

import edu.softwarelab.starter.dto.RtgsRequest;
import edu.softwarelab.starter.entity.RtgsStatus;
import edu.softwarelab.starter.entity.RtgsTransaction;
import edu.softwarelab.starter.repository.RtgsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RtgsProcessorService {

    private final RtgsValidationService validationService;
    private final RtgsRepository repository;

    public RtgsProcessorService(RtgsValidationService validationService, RtgsRepository repository) {
        this.validationService = validationService;
        this.repository = repository;
    }

    @Transactional
    public RtgsTransaction process(RtgsRequest request) {
        // ۱. ایجاد و ثبت اولیه تراکنش
        RtgsTransaction transaction = new RtgsTransaction(
                request.transactionId(),
                request.sourceIban(),
                request.destinationIban(),
                request.amount(),
                request.description()
        );
        repository.save(transaction);

        try {
            // ۲. اعتبارسنجی
            validationService.validate(request);
            transaction.setStatus(RtgsStatus.VALIDATED);

            // ۳. تغییر وضعیت به در حال پردازش
            transaction.setStatus(RtgsStatus.PROCESSING);

            // ۴. فراخوانی هسته معاملاتی / Core Banking (محل اعمال منطق کسر و واریز)
            executeCoreTransfer(transaction);

            // ۵. پایان موفقیت‌آمیز
            transaction.setStatus(RtgsStatus.COMPLETED);

        } catch (Exception e) {
            transaction.setStatus(RtgsStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
        }

        return repository.save(transaction);
    }

    private void executeCoreTransfer(RtgsTransaction transaction) {
        // منطق ارتباط با سرویس Core Banking برای انتقال وجه
        // در صورت بروز خطا، Exception پرتاب می‌شود
    }
}