package com.crown.omok.service;

public interface EloService {
    /** 승자/패자 점수 변화량 계산 후 DB 반영 */
    void calculate(Long winnerId, Long loserId);

    /** 무승부 처리 */
    void calculateDraw(Long memberIdA, Long memberIdB);
}
