package com.crown.friend.dao;

import com.crown.friend.dto.FriendDto;
import com.crown.friend.mapper.FriendMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FriendDao {
    private final FriendMapper friendMapper;

    public void insertRequest(Long requesterId, Long receiverId) {
        friendMapper.insertRequest(requesterId, receiverId);
    }

    public void updateStatus(Long requesterId, Long receiverId, String status) {
        friendMapper.updateStatus(requesterId, receiverId, status);
    }

    public void deleteByPair(Long userId, Long friendId) {
        friendMapper.deleteByPair(userId, friendId);
    }

    public List<FriendDto> findFriends(Long memberId) {
        return friendMapper.findFriends(memberId);
    }

    public List<FriendDto> findPendingRequests(Long memberId) {
        return friendMapper.findPendingRequests(memberId);
    }

    public FriendDto findByPair(Long requesterId, Long receiverId) {
        return friendMapper.findByPair(requesterId, receiverId);
    }
}
