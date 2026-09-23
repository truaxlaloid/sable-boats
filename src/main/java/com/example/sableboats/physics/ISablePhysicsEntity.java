package com.example.sableboats.physics;

public interface ISablePhysicsEntity {
    BoatPhysicsHandler sable$getPhysicsHandler();
    float sable$getPitch(float partialTicks);
    float sable$getRoll(float partialTicks);
}
