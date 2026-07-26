package com.human.client.render.item.gun.muzzled.impl;

import com.human.client.animation.item.ZX76ShotgunAnimator;
import com.human.client.render.item.gun.muzzled.MuzzledGunItemRenderer;

import java.util.List;

public class ZX76ShotgunItemRenderer extends MuzzledGunItemRenderer {

    public ZX76ShotgunItemRenderer(String name) {
        super(
            name,
            List.of("gFlash1", "gFlash2"),
            config -> config
                .setAnimatorProvider(ZX76ShotgunAnimator::new)
        );
    }
}
