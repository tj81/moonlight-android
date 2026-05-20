package com.limelight;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.CountDownTimer;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

public class MyAccessibilityService extends AccessibilityService {

    private static final int TOUCH_SIM_INTERVAL = 333;

    private static final int TOUCH_SIM_DURATION_MS = 50;

    private static final int TOUCH_SIM_POS_X = 0;

    private static final int TOUCH_SIM_POS_Y = 0;

    private static MyAccessibilityService instance;

    private static boolean isPaused = true;

    private static boolean hasVirtualController;

    /**
     * Track if there is one or more touches active.
     */
    private static boolean isTouchDown;

    /**
     * Periodically create touch-events via accessability API.
     */
    private final CountDownTimer toucher = new CountDownTimer(Long.MAX_VALUE, TOUCH_SIM_INTERVAL) {
        public void onTick(long millisUntilFinished) {
            boolean hasMoonlightApp = false, hasInputMethod = false;

            var windows = getWindows();
            for (int i = 0; i < windows.size(); i++) {
                var window = windows.get(i);
                if (window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    var node = window.getRoot();
                    var b = new Rect();
                    window.getBoundsInScreen(b);
                    if (node != null &&
                        node.getPackageName().toString().startsWith("com.limelight") &&
                        window.isActive() && // Focussed and in use and not PiP
                        b.left == 0 && b.top == 0 // Detect fullscreen
                    ) {
                        hasMoonlightApp = true;
                    }
                }
                else if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    hasInputMethod = true;
                }
            }

            if (hasMoonlightApp && !hasInputMethod) {
                simulateTap(TOUCH_SIM_POS_X, TOUCH_SIM_POS_Y, TOUCH_SIM_DURATION_MS);
            }
        }

        public void onFinish() {
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        // Optimization paranoia: Tell we are not interested in any packages
        var info = getServiceInfo();
        info.packageNames = new String[] { };
        setServiceInfo(info);

        instance = this;
        updateState();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Should never be called (we don't listen to any events)
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (instance != null) {
            instance.toucher.cancel();
        }
        instance = null;
        super.onDestroy();
    }

    private void updateState() {
        if (isPaused || hasVirtualController || isTouchDown) {
            instance.toucher.cancel();
        } else {
            instance.toucher.start();
        }
    }

    public static void setIsPaused(boolean isPaused) {
        MyAccessibilityService.isPaused = isPaused;
        if (instance != null) {
            instance.updateState();
        }
    }

    public static void setHasVirtualController(boolean hasVirtualController) {
        MyAccessibilityService.hasVirtualController = hasVirtualController;
        if (instance != null) {
            instance.updateState();
        }
    }

    public static boolean allowTouchDown(int eventX, int eventY) {
        return eventX != TOUCH_SIM_POS_X && eventY != TOUCH_SIM_POS_Y;
    }

    public static boolean allowTouchUp(int eventX, int eventY) {
        return isTouchDown || (eventX != TOUCH_SIM_POS_X && eventY != TOUCH_SIM_POS_Y);
    }

    public static void setIsTouchDown(boolean isTouchDown) {
        MyAccessibilityService.isTouchDown = isTouchDown;
        if (instance != null) {
            instance.updateState();
        }
    }

    private void simulateTap(float x, float y, long duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var path = new Path();
            path.moveTo(x, y);

            var builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));

            dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                }
            }, null);
        }
    }
}
