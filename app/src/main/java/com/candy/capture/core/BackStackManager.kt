package com.candy.capture.core

import android.app.Activity
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:18
 */
class BackStackManager private constructor() {

    private var mActivityStack: Stack<Activity> = Stack()

    companion object {
        private var backStackManager: BackStackManager? = null

        fun getInstance(): BackStackManager {
            if (backStackManager == null) {
                backStackManager = BackStackManager()
            }
            return backStackManager!!
        }
    }

    fun push(activity: Activity) {
        mActivityStack.push(activity)
    }

    fun removeSpecificActivity(activity: Activity) {
        mActivityStack.remove(activity)
    }

    fun removeSpecificActivityByClass(cls: Class<*>) {
        val iterator = mActivityStack.iterator()
        var activity: Activity? = null
        while (iterator.hasNext()) {
            activity = iterator.next()
            if (activity.javaClass == cls) {
                activity.finish()
                iterator.remove()
            }
        }
    }
}