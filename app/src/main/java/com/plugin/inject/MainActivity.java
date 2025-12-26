package com.plugin.inject;

import android.app.Activity;
import android.os.Bundle;

import com.plugin.inject.crash.CrashUtil;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashUtil.init(this);
        throw new RuntimeException("测试崩溃：这是一个故意触发的异常");
    }
}
