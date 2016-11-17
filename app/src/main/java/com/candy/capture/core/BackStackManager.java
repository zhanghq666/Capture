package com.candy.capture.core;

import android.app.Activity;

import java.util.Iterator;
import java.util.Stack;

/**
 * Created by zhanghq on 2016/11/17.
 */

public class BackStackManager {

    private static BackStackManager backStackManager;
    private Stack<Activity> mActivityStack;

    private BackStackManager() {
        mActivityStack = new Stack<>();
    }

    public static BackStackManager getInstance() {
        if (backStackManager == null) {
            backStackManager = new BackStackManager();
        }
        return backStackManager;
    }

    public void push(Activity activity) {
        mActivityStack.push(activity);
    }

    public void removeSpecificActivity(Activity activity) {
        mActivityStack.remove(activity);
    }

    public void removeSpecificActivityByClass(Class<?> cls) {
        Iterator<Activity> iterator = mActivityStack.iterator();
        Activity activity = null;
        while (iterator.hasNext()) {
            activity = iterator.next();
            if (activity.getClass().equals(cls)) {
                activity.finish();
                iterator.remove();
            }
        }
    }
}
