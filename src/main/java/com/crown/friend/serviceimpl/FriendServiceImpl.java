package com.crown.friend.serviceimpl;

import com.crown.friend.dao.FriendDao;
import com.crown.friend.dto.FriendDto;
import com.crown.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendDao friendDao;

    @Override
    public void sendRequest(Long requesterId, Long receiverId) {
        // 이미 관계가 있으면 무시
        FriendDto existing = friendDao.findByPair(requesterId, receiverId);
        FriendDto reverse  = friendDao.findByPair(receiverId, requesterId);
        if (existing == null && reverse == null) {
            friendDao.insertRequest(requesterId, receiverId);
        }
    }

    @Override
    public void accept(Long requesterId, Long receiverId) {
        friendDao.updateStatus(requesterId, receiverId, "ACCEPTED");
    }

    @Override
    public void reject(Long requesterId, Long receiverId) {
        friendDao.deleteByPair(requesterId, receiverId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        friendDao.deleteByPair(userId, friendId);
    }

    @Override
    public List<FriendDto> getFriends(Long memberId) {
        return friendDao.findFriends(memberId);
    }

    @Override
    public List<FriendDto> getPendingRequests(Long memberId) {
        return friendDao.findPendingRequests(memberId);
    }
}
