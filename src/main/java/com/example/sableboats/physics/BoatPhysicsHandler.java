package com.example.sableboats.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BoatPhysicsHandler {
    private final Boat boat;
    private final Quaternionf orientation = new Quaternionf();
    private final Quaternionf prevOrientation = new Quaternionf();
    private Vec3 angularVelocity = Vec3.ZERO;

    private float pitch = 0.0f;
    private float prevPitch = 0.0f;
    private float roll = 0.0f;
    private float prevRoll = 0.0f;

    public BoatPhysicsHandler(Boat boat) {
        this.boat = boat;
        this.orientation.rotateY((float) Math.toRadians(-boat.getYRot()));
        this.prevOrientation.set(this.orientation);
    }

    public void tick() {
        this.prevOrientation.set(this.orientation);
        this.prevPitch = this.pitch;
        this.prevRoll = this.roll;

        BlockPos pos = boat.blockPosition();
        FluidState fluid = boat.level().getFluidState(pos);
        boolean inWater = fluid.is(FluidTags.WATER);
        boolean onIce = isContactingIce();

        Vec3 motion = boat.getDeltaMovement();

        // 1. Gravity & Buoyancy
        if (inWater) {
            // Calculate depth below water surface
            double waterSurfaceY = pos.getY() + fluid.getHeight(boat.level(), pos);
            double depth = waterSurfaceY - boat.getY();
            
            // Buoyant upward push proportional to submerged depth
            double buoyancy = Mth.clamp(depth * 0.05, -0.02, 0.06);
            motion = new Vec3(motion.x, motion.y * 0.8 + buoyancy, motion.z);
        } else if (!boat.onGround()) {
            // Apply standard Minecraft gravity when in air
            motion = motion.add(0.0, -0.04, 0.0);
        }

        // 2. Righting Torque (Metacentric stability pulling boat upright)
        Vector3f localUp = new Vector3f(0, 1, 0);
        this.orientation.transform(localUp);
        Vector3f targetUp = new Vector3f(0, 1, 0);
        Vector3f rightingTorque = new Vector3f();
        localUp.cross(targetUp, rightingTorque);

        float rightingStrength = inWater ? 0.08f : 0.03f;
        this.angularVelocity = this.angularVelocity.add(
                rightingTorque.x() * rightingStrength,
                0.0, // Don't tamper with yaw during righting
                rightingTorque.z() * rightingStrength
        );

        // 3. Linear & Angular Damping
        float linearDampingH = onIce ? 0.992f : (inWater ? 0.94f : 0.85f);
        float linearDampingV = inWater ? 0.85f : 0.98f;
        float angularDamping = inWater ? 0.70f : 0.85f;

        boat.setDeltaMovement(motion.x * linearDampingH, motion.y * linearDampingV, motion.z * linearDampingH);
        this.angularVelocity = this.angularVelocity.scale(angularDamping);

        // 4. Integrate Rotation
        Quaternionf deltaRot = new Quaternionf().rotateXYZ(
                (float) this.angularVelocity.x,
                (float) this.angularVelocity.y,
                (float) this.angularVelocity.z
        );
        this.orientation.premul(deltaRot);
        this.orientation.normalize();

        // 5. Compute Pitch, Roll, and synchronize Yaw with Minecraft Entity
        Vector3f forwardVec = new Vector3f(0, 0, 1);
        this.orientation.transform(forwardVec);

        Vector3f upVec = new Vector3f(0, 1, 0);
        this.orientation.transform(upVec);

        float newYaw = (float) Math.toDegrees(Math.atan2(-forwardVec.x, forwardVec.z));
        this.pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-forwardVec.y, -1.0f, 1.0f)));
        this.roll = (float) Math.toDegrees(Math.atan2(upVec.x, upVec.y));

        boat.setYRot(newYaw);
        boat.yRotO = newYaw;
    }

    public void applyPropulsion(boolean forward, boolean back, boolean left, boolean right) {
        boolean onIce = isContactingIce();
        
        // Tuned to match vanilla boat speed
        double thrust = onIce ? 0.025 : 0.008;
        double turnSpeed = 0.025; // Smooth ~2 deg/tick turning rate

        Vector3f forwardVec = new Vector3f(0, 0, 1);
        this.orientation.transform(forwardVec);
        Vec3 fwd = new Vec3(forwardVec.x(), 0.0, forwardVec.z()).normalize();

        if (forward) {
            boat.setDeltaMovement(boat.getDeltaMovement().add(fwd.scale(thrust)));
        } else if (back) {
            boat.setDeltaMovement(boat.getDeltaMovement().add(fwd.scale(-thrust * 0.4)));
        }

        if (left) {
            this.angularVelocity = this.angularVelocity.add(0, turnSpeed, 0);
        } else if (right) {
            this.angularVelocity = this.angularVelocity.add(0, -turnSpeed, 0);
        }
    }

    private boolean isContactingIce() {
        BlockPos below = boat.blockPosition().below();
        BlockState state = boat.level().getBlockState(below);
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    public float getPitch(float partialTicks) {
        return Mth.lerp(partialTicks, this.prevPitch, this.pitch);
    }

    public float getRoll(float partialTicks) {
        return Mth.lerp(partialTicks, this.prevRoll, this.roll);
    }
}
