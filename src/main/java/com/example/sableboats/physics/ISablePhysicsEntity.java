package com.example.sableboats.physics;

import org.joml.Quaternionf;

public interface ISablePhysicsEntity {
    BoatPhysicsHandler sable$getPhysicsHandler();
    Quaternionf sable$getInterpolatedOrientation(float partialTicks);
}
