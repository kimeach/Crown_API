package com.crown.member.dto;

import com.google.firebase.auth.FirebaseToken;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FirebaseAttributes {

    private String googleId;
    private String nickname;
    private String email;
    private String profileImg;

    public static FirebaseAttributes of(FirebaseToken token) {
        return FirebaseAttributes.builder()
                .googleId(token.getUid())
                .nickname(token.getName())
                .email(token.getEmail())
                .profileImg(token.getPicture())
                .build();
    }
}
