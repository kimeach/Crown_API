package com.crown.friend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendDto {
    private Long   memberId;
    private String nickname;
    private String profileImg;
    private int    score;
    private String status; // PENDING, ACCEPTED
}
