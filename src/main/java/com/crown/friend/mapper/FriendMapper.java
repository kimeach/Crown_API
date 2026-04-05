package com.crown.friend.mapper;

import com.crown.friend.dto.FriendDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendMapper {
    void insertRequest(@Param("requesterId") Long requesterId,
                       @Param("receiverId")  Long receiverId);

    void updateStatus(@Param("requesterId") Long requesterId,
                      @Param("receiverId")  Long receiverId,
                      @Param("status")      String status);

    void deleteByPair(@Param("userId")   Long userId,
                      @Param("friendId") Long friendId);

    /** 내 수락된 친구 목록 */
    List<FriendDto> findFriends(@Param("memberId") Long memberId);

    /** 나에게 온 대기 중인 친구 요청 */
    List<FriendDto> findPendingRequests(@Param("memberId") Long memberId);

    /** 두 사람 사이의 친구 관계 조회 */
    FriendDto findByPair(@Param("requesterId") Long requesterId,
                         @Param("receiverId")  Long receiverId);
}
