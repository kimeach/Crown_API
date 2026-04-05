package com.crown.omok.serviceimpl;

import com.crown.member.dao.MemberDao;
import com.crown.member.dto.MemberDto;
import com.crown.omok.service.EloService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EloServiceImpl implements EloService {

    private static final int K = 32;

    private final MemberDao memberDao;

    @Override
    public void calculate(Long winnerId, Long loserId) {
        MemberDto winner = memberDao.findById(winnerId);
        MemberDto loser  = memberDao.findById(loserId);

        double expectedWin  = expected(winner.getScore(), loser.getScore());
        double expectedLose = expected(loser.getScore(), winner.getScore());

        int winnerDelta = (int) Math.round(K * (1 - expectedWin));
        int loserDelta  = (int) Math.round(K * (0 - expectedLose));

        memberDao.updateScore(winnerId, winnerDelta, "WIN");
        memberDao.updateScore(loserId,  loserDelta,  "LOSE");
    }

    @Override
    public void calculateDraw(Long memberIdA, Long memberIdB) {
        MemberDto memberA = memberDao.findById(memberIdA);
        MemberDto memberB = memberDao.findById(memberIdB);

        double expectedA = expected(memberA.getScore(), memberB.getScore());
        double expectedB = expected(memberB.getScore(), memberA.getScore());

        int deltaA = (int) Math.round(K * (0.5 - expectedA));
        int deltaB = (int) Math.round(K * (0.5 - expectedB));

        memberDao.updateScore(memberIdA, deltaA, "DRAW");
        memberDao.updateScore(memberIdB, deltaB, "DRAW");
    }

    private double expected(int scoreA, int scoreB) {
        return 1.0 / (1 + Math.pow(10, (scoreB - scoreA) / 400.0));
    }
}
