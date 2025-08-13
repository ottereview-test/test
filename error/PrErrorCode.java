package com.ssafy.ottereview.preparation.exception;

import com.ssafy.ottereview.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PreparationErrorCode implements ErrorCode {
    
    PREPARATION_NOT_FOUND("PRE001", "준비 작업을 찾을 수 없습니다", 404),
    PREPARATION_ALREADY_EXISTS("PRE002", "이미 존재하는 PR 입니다", 409),
    PREPARATION_NOT_AUTHORIZED("PRE003", "준비 작업에 대한 권한이 없습니다", 403),
    PREPARATION_INVALID_STATE("PRE004", "준비 작업의 상태가 유효하지 않습니다", 400),
    PREPARATION_VALIDATION_FAILED("PRE005", "준비 작업 검증에 실패했습니다.", 400),
    PREPARATION_CREATE_FAILED("PRE006", "준비 작업 생성에 실패했습니다", 500),
    PREPARATION_UPDATE_FAILED("PRE007", "준비 작업 업데이트에 실패했습니다", 500),
    PREPARATION_TRANSFORM_FAILED("PRE008", "준비 작업 변환에 실패했습니다", 500);
    
    private final String code;
    private final String message;
    private final int httpStatus;
    
}
