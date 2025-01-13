package com.example.addon.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class HeadlessWindow {
//    @Final @Shadow private long handle;
//
//    @Inject(method = "<init>", at = @At("RETURN"))
//    private void onInit(CallbackInfo ci) {
//        // Do nothing, effectively preventing window creation
//    }
//
//    @Inject(method = "updateWindowRegion", at = @At("HEAD"), cancellable = true)
//    private void onUpdateWindowRegion(CallbackInfo ci) {
//        ci.cancel(); // Prevent window updates
//    }
//
//    @Inject(method = "swapBuffers", at = @At("HEAD"), cancellable = true)
//    private void onSwapBuffers(CallbackInfo ci) {
//        RenderSystem.replayQueue();
//        ci.cancel(); // Prevent buffer swapping
//    }
//
//    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
//    private void onSetTitle(String title, CallbackInfo ci) {
//        ci.cancel(); // Prevent title updates
//    }
//
//    @Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
//    private void onToggleFullscreen(CallbackInfo ci) {
//        ci.cancel(); // Prevent fullscreen toggling
//    }
}
