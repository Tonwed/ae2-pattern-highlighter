package com.ae2addon.highlighter.client;

import appeng.api.stacks.AEKey;

/**
 * 存储待处理的相机动作
 * 当用户点击眼镜按钮后，需要先获取provider列表，然后触发相机
 */
public class PendingCameraAction {
    private static AEKey pendingItem = null;
    
    public static void set(AEKey item) {
        pendingItem = item;
    }
    
    public static AEKey get() {
        return pendingItem;
    }
    
    public static void clear() {
        pendingItem = null;
    }
    
    public static boolean hasPending() {
        return pendingItem != null;
    }
}
