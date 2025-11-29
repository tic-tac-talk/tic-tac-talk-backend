package com.khi.ragservice.enums;

/**
 * 대화 분석 보고서의 상태를 표현하는 enum
 */
public enum ReportState {
    /**
     * 생성 중 - 음성 파일 수신 후 전사/RAG 처리 전
     */
    PENDING,

    /**
     * 완료 - RAG 분석이 완료되어 보고서가 생성됨
     */
    COMPLETED,

    /**
     * 실패 - 처리 중 에러 발생
     */
    FAILED
}
