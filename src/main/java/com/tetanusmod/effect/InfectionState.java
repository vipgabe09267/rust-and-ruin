package com.tetanusmod.effect;

public interface InfectionState {
    int tetanus$getElapsed();
    int tetanus$getTotal();
    int tetanus$getLockjaw();
    int tetanus$getFreeze();
    void tetanus$setInfection(int elapsed, int total);
    void tetanus$setLockjaw(int ticks);
    void tetanus$setFreeze(int ticks);
}
