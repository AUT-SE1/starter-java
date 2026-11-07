package edu.softwarelab.starter.controller;

import edu.softwarelab.starter.dto.RtgsRequest;
import edu.softwarelab.starter.entity.RtgsStatus;
import edu.softwarelab.starter.entity.RtgsTransaction;
import edu.softwarelab.starter.service.RtgsProcessorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rtgs")
public class RtgsController {

    private final RtgsProcessorService processorService;

    public RtgsController(RtgsProcessorService processorService) {
        this.processorService = processorService;
    }

    @PostMapping("/process")
    public ResponseEntity<RtgsTransaction> processTransaction(@RequestBody RtgsRequest request) {
        RtgsTransaction result = processorService.process(request);

        if (result.getStatus() == RtgsStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.ok(result);
    }
}