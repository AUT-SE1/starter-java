package edu.softwarelab.starter.kafka;

public final class KafkaTopics {
    // S3 <-> S5 Topics
    public static final String CMD_EXECUTE_TRANSFER = "lab.s3.s5.command.transfer.external.v3";
    public static final String CMD_INQUIRE_TRANSFER = "lab.s3.s5.command.transfer.inquire.v3";
    public static final String EVT_OPERATION_RESULT = "lab.s5.s3.event.operation-result.v3";
