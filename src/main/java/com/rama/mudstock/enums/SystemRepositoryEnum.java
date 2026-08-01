package com.rama.mudstock.enums;

public final class SystemRepositoryEnum {

    private SystemRepositoryEnum() {
    }

    public enum OptionIntervalAnalyseStatusEnum {
        CREATE_CONTRACT,
        ACTIVE,
        API_COMPLETED,
        FLAT_FILE_COMPLETED,
        PARTIALLY_COMPLETED,
        CLOSE,
        COMPLETED;

        public static OptionIntervalAnalyseStatusEnum fromValue(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim().toUpperCase();
            for (OptionIntervalAnalyseStatusEnum status : values()) {
                if (status.name().equals(normalized)) {
                    return status;
                }
            }
            return null;
        }
    }

    public enum OptionSourceEnum {
        API,
        FLAT_FILE,
        BOTH;

        public static OptionSourceEnum fromValue(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim().toUpperCase();
            for (OptionSourceEnum source : values()) {
                if (source.name().equals(normalized)) {
                    return source;
                }
            }
            return null;
        }
    }

    public enum OptionContractStatusEnum {
        ACTIVE,
        API_COMPLETED,
        FLAT_FILE_COMPLETED,
        COMPLETED;

        public static OptionContractStatusEnum fromValue(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim().toUpperCase();
            for (OptionContractStatusEnum status : values()) {
                if (status.name().equals(normalized)) {
                    return status;
                }
            }
            return null;
        }
    }
}
