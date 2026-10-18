package io.github.xiangwenquan830.silversamurai;

import static io.github.xiangwenquan830.silversamurai.SamuraiEntity.*;

/** State permissions in one place; the existing integer IDs remain save/network compatible. */
final class ActionRules {
    enum MotionOwner {
        NAVIGATION,
        COMBAT,
        FOOTWORK,
        TRAVERSAL,
        NONE
    }

    static boolean attack(int state) {
        return combo(state) || state == SIDE_SWEEP;
    }

    static boolean combo(int state) {
        return state == ATTACK_A || state == ATTACK_B;
    }

    static boolean canBeginAttack(int state) {
        return state == IDLE
                || state == CHASE
                || state == BACK_WALK
                || state == FOOTWORK
                || state == SETTLE
                || state == CLOSE_GUARD;
    }

    static boolean retarget(int state) {
        return !attack(state)
                && state != TRANSITION
                && state != DEAD
                && state != STOMP
                && state != CLEAR
                && state != SHEATHE
                && state != FOOTWORK
                && state != WALL;
    }

    static boolean reaction(int state) {
        return state == IDLE
                || state == BACK_WALK
                || state == DRAW
                || state == CHASE
                || state == RECOVER
                || state == SETTLE
                || state == SHEATHE;
    }

    static MotionOwner motion(int state) {
        return switch (state) {
            case CHASE, BACK_WALK, DRAW -> MotionOwner.NAVIGATION;
            case ATTACK_A, ATTACK_B, SIDE_SWEEP -> MotionOwner.COMBAT;
            case FOOTWORK -> MotionOwner.FOOTWORK;
            case VAULT, CLIMB, WALL -> MotionOwner.TRAVERSAL;
            default -> MotionOwner.NONE;
        };
    }

    private ActionRules() {}
}
