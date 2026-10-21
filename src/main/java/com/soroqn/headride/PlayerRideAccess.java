package com.soroqn.headride;

public interface PlayerRideAccess {
    int headride$getMountCooldown();
    void headride$setMountCooldown(int cooldown);

    boolean headride$mustReleaseShift();
    void headride$setMustReleaseShift(boolean mustRelease);

    int headride$getCarrierCooldown();
    void headride$setCarrierCooldown(int cooldown);
}
