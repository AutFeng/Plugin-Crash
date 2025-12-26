package com.plugin.inject.crash;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;

import com.plugin.inject.crash.activity.CrashActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrashUtil {
    private static List<String> ignoredMessages = new ArrayList<>();

    private CrashUtil() {}

    public static void init(Context context) {
        loadIgnoredMessages(context);

        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            if (shouldIgnoreCrash(ex)) {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, ex);
                }
                return;
            }

            if (Looper.myLooper() == Looper.getMainLooper()) {
                handleCrash(context, ex);
            }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex);
            }
        });
    }

    private static void handleCrash(Context context, Throwable ex) {
        try {
            Intent intent = new Intent(context, CrashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("stack_trace", Log.getStackTraceString(ex));

            context.startActivity(intent);

            if (context instanceof Activity) {
                ((Activity) context).finishAffinity();
            }
            System.exit(10);
        } catch (Exception e) {
            Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
            if (handler != null) {
                handler.uncaughtException(
                    Thread.currentThread(),
                    new Throwable("Failed to start crash activity", ex)
                );
            }
        }
    }

    private static boolean shouldIgnoreCrash(Throwable ex) {
        String message = ex.getMessage();
        if (message == null) {
            message = "";
        }

        for (String ignoredMessage : ignoredMessages) {
            if (message.contains(ignoredMessage)) {
                return true;
            }
        }
        return false;
    }

    private static void loadIgnoredMessages(Context context) {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("ignored_crash_messages.txt"))
            );

            List<String> messages = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    messages.add(line);
                }
            }
            reader.close();

            ignoredMessages = messages;
        } catch (Exception e) {
            ignoredMessages = Collections.emptyList();
        }
    }
}
