package com.example.sableboats.mixin.client;

import com.example.sableboats.physics.ISablePhysicsEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoatRenderer.class)
public class BoatRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/vehicle/Boat;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", 
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", shift = At.Shift.AFTER))
    private void sable$applyPitchAndRoll(Boat boat, float entityYaw, float partialTicks, 
                                         PoseStack poseStack, MultiBufferSource buffer, 
                                         int packedLight, CallbackInfo ci) {
        if (boat instanceof ISablePhysicsEntity sableEntity) {
            float pitch = sableEntity.sable$getPitch(partialTicks);
            float roll = sableEntity.sable$getRoll(partialTicks);

            // Apply 6-DOF tilt (pitch and roll) on top of interpolated yaw
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }
}
