package com.example.miui.multiuserhook;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiUserSwitchActivity extends Activity {

    private static class UserItem {
        int id;
        String name;
        boolean running;

        UserItem(int id, String name, boolean running) {
            this.id = id;
            this.name = name;
            this.running = running;
        }

        public String toString() {
            return name + "  ID:" + id + (running ? "  运行中" : "");
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<UserItem> users = loadUsersByRoot();
        if (users.isEmpty()) {
            showErrorAndFinish("未获取到用户列表，可能没有 root 或 su 权限被拒绝。");
            return;
        }

        showUserDialog(users);
    }

    private void showUserDialog(List<UserItem> users) {
        String[] items = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            items[i] = users.get(i).toString();
        }

        new AlertDialog.Builder(this)
                .setTitle("选择要切换的用户")
                .setItems(items, (dialog, which) -> switchUserByRoot(users.get(which).id))
                .setNegativeButton("取消", (d, w) -> finish())
                .setOnCancelListener(d -> finish())
                .show();
    }

    private List<UserItem> loadUsersByRoot() {
        List<UserItem> result = new ArrayList<>();

        try {
            CommandResult r = execRootCommand("pm list users");
            if (r.exitCode != 0) return result;

            Pattern p = Pattern.compile("UserInfo\\{(\\d+):([^:}]+):[^}]+\\}(.*)");
            String[] lines = r.output.split("\n");

            for (String line : lines) {
                line = line.trim();
                Matcher m = p.matcher(line);
                if (m.find()) {
                    int id = Integer.parseInt(m.group(1));
                    String name = m.group(2);
                    String tail = m.group(3);
                    boolean running = tail != null && tail.indexOf("running") >= 0;
                    result.add(new UserItem(id, name, running));
                }
            }
        } catch (Throwable ignored) {
        }

        return result;
    }

    private void switchUserByRoot(int userId) {
        try {
            CommandResult r = execRootCommand("am switch-user " + userId);
            if (r.exitCode == 0) {
                Toast.makeText(this, "正在切换到用户 " + userId, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                showErrorAndFinish("切换失败:\n" + r.output);
            }
        } catch (Throwable t) {
            showErrorAndFinish("切换异常:\n" + t);
        }
    }

    private static class CommandResult {
        int exitCode;
        String output;

        CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private CommandResult execRootCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});

        StringBuilder out = new StringBuilder();
        BufferedReader br1 = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader br2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = br1.readLine()) != null) {
            out.append(line).append('\n');
        }
        while ((line = br2.readLine()) != null) {
            out.append(line).append('\n');
        }

        int code = process.waitFor();
        return new CommandResult(code, out.toString());
    }

    private void showErrorAndFinish(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("多用户切换")
                .setMessage(msg)
                .setPositiveButton("确定", (d, w) -> finish())
                .setOnCancelListener(d -> finish())
                .show();
    }
}
