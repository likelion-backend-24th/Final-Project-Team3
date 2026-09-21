package com.example.memberservice.member.service;

import com.example.memberservice.common.exception.BusinessException;
import com.example.memberservice.member.dto.OrganizerSignupRequest;
import com.example.memberservice.member.dto.OrganizerSignupResponse;
import com.example.memberservice.member.exception.MemberErrorCode;
import com.example.memberservice.member.verifier.BusinessStatus;
import com.example.memberservice.member.verifier.BusinessVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 국세청 상태조회는 최대 수 초가 걸리는 외부 호출이라, 가입 트랜잭션(MemberService.signupOrganizer) 밖에서 먼저 끝낸다.
// open-in-view가 켜져 있으면 DB를 한 번이라도 건드린 뒤에는 커넥션이 응답까지 잡혀 있을 수 있어서,
// 공개 엔드포인트인 가입 API가 외부 장애 때 커넥션 풀을 고갈시키지 않도록 DB 접근 전에 호출한다.
@Service
@RequiredArgsConstructor
public class OrganizerSignupFacade {

    private final BusinessNoValidator businessNoValidator;
    private final BusinessVerifier businessVerifier;
    private final MemberService memberService;

    public OrganizerSignupResponse signup(OrganizerSignupRequest request) {
        if (!businessNoValidator.isValidFormat(request.businessNo())) {
            throw new BusinessException(MemberErrorCode.INVALID_BUSINESS_NO);
        }

        BusinessStatus status = businessVerifier.checkStatus(request.businessNo());
        if (status == BusinessStatus.NOT_REGISTERED) {
            throw new BusinessException(MemberErrorCode.BUSINESS_NOT_REGISTERED);
        }
        if (status != BusinessStatus.ACTIVE) {
            throw new BusinessException(MemberErrorCode.BUSINESS_NOT_ACTIVE);
        }

        return memberService.signupOrganizer(request);
    }
}
