package io.github.xiangwenquan830.silversamurai;

/** Authored motions are kept intact; traversal owns their world displacement. */
final class WallClips {
    static final String REACH = "攀爬_高位落手抓墙_末步踩墙",
            LOOP = "攀爬_抬头垂直循环",
            FIRST = "攀爬_首步连续抬膝_仅起爬使用",
            TOP = "攀爬_架膝踩稳蹬起翻顶",
            CATCH = "下坠_空手抢抓_左手先承重",
            HOLD = "下坠_抓稳检查_待命",
            RELEASE = "下坠_无路松手_蹬离墙面",
            STAB = "下坠_持刀80度猛刺_下滑制动",
            BRACE = "下坠_插刀稳身_待命",
            EXTRACT = "下坠_蹬墙拧腰_借力拔刀脱离",
            RED_STAB = "下坠_红刀80度猛刺_长距离削墙滑降",
            RED_BRACE = "下坠_红刀滑降稳身_待命",
            RED_EXTRACT = "红刀_下坠_蹬墙拧腰_借力拔刀脱离";

    static String sheathe(int p) {
        return (p == 1 ? "双刀" : "单刀") + "_收刀同步接近_至松柄";
    }

    static String draw(int p) {
        return (p == 1 ? "双刀" : "单刀") + "_攀爬登顶再拔刀";
    }

    static boolean grounded(String c) {
        return PursuitJump.clip(c)
                || c.equals(REACH)
                || c.contains("收刀同步接近")
                || c.contains("攀爬登顶再拔刀");
    }

    private WallClips() {}
}
