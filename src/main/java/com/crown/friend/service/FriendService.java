package com.crown.friend.service;

import com.crown.friend.dto.FriendDto;

import java.util.List;

public interface FriendService {
    void sendRequest(Long requesterId, Long receiverId);
    void accept(Long requesterId, Long receiverId);
    void reject(Long requesterId, Long receiverId);
    void removeFriend(Long userId, Long friendId);
    List<FriendDto> getFriends(Long memberId);
    List<FriendDto> getPendingRequests(Long memberId);
}
