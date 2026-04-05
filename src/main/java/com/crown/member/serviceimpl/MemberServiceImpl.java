package com.crown.member.serviceimpl;

import com.crown.member.dao.MemberDao;
import com.crown.member.dto.FirebaseAttributes;
import com.crown.member.dto.MemberDto;
import com.crown.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberDao memberDao;

    @Override
    public MemberDto saveOrUpdate(FirebaseAttributes attributes) {
        MemberDto existing = memberDao.findByGoogleId(attributes.getGoogleId());
        if (existing == null) {
            memberDao.insert(attributes);
        } else {
            memberDao.updateProfile(attributes);
        }
        return memberDao.findByGoogleId(attributes.getGoogleId());
    }

    @Override
    public MemberDto findByGoogleId(String googleId) {
        return memberDao.findByGoogleId(googleId);
    }

    @Override
    public MemberDto findById(Long memberId) {
        return memberDao.findById(memberId);
    }

    @Override
    public List<MemberDto> findRanking(int limit) {
        return memberDao.findRanking(limit);
    }

    @Override
    public List<MemberDto> searchByNickname(String nickname, Long excludeId) {
        return memberDao.searchByNickname(nickname, excludeId);
    }

    @Override
    public MemberDto updateNickname(Long memberId, String nickname) {
        memberDao.updateNickname(memberId, nickname);
        return memberDao.findById(memberId);
    }

    @Override
    public MemberDto updateProfileImg(Long memberId, String profileImg) {
        memberDao.updateProfileImg(memberId, profileImg);
        return memberDao.findById(memberId);
    }
}
