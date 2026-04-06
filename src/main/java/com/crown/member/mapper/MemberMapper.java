package com.crown.member.mapper;

import com.crown.member.dto.FirebaseAttributes;
import com.crown.member.dto.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MemberMapper {
    MemberDto findByGoogleId(@Param("googleId") String googleId);
    MemberDto findByEmail(@Param("email") String email);
    void updateGoogleId(@Param("memberId") Long memberId, @Param("googleId") String googleId);
    MemberDto findById(@Param("memberId") Long memberId);
    void insert(FirebaseAttributes attributes);
    void updateProfile(FirebaseAttributes attributes);
    void updateFcmToken(@Param("memberId") Long memberId, @Param("fcmToken") String fcmToken);
    String getFcmToken(@Param("memberId") Long memberId);
    void updateNickname(@Param("memberId") Long memberId, @Param("nickname") String nickname);
    void updateProfileImg(@Param("memberId") Long memberId, @Param("profileImg") String profileImg);
    void updateScore(@Param("memberId") Long memberId,
                     @Param("scoreDelta") int scoreDelta,
                     @Param("result") String result);
    List<MemberDto> findRanking(@Param("limit") int limit);
    List<MemberDto> searchByNickname(@Param("nickname")   String nickname,
                                     @Param("excludeId") Long   excludeId);
}
