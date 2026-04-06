package com.crown.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberDto {
    private Long   memberId;
    private String googleId;
    private String nickname;
    private String email;
    private String profileImg;
    private int    score;
    private int    winCount;
    private int    loseCount;
    private int    drawCount;
    private String plan;
}
