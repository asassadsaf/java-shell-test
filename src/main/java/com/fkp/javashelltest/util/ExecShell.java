package com.fkp.javashelltest.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author fengkunpeng
 * @version 1.0
 * @description 调用shell工具类
 * @date 2023/7/21 11:05
 */
@Slf4j
public class ExecShell {

    public static final String INFO_LEVEL = "info";
    public static final String ERROR_LEVEL = "error";
    public static final String ALL_LEVEL = "all";
    public static final String SEPARATOR = System.getProperty("line.separator");

    public static Process getExecShellProcess(String command) throws IOException {
        String[] cmd = {"/bin/sh", "-c", command};
        return Runtime.getRuntime().exec(cmd);

    }


    public synchronized static void killDupProcess(int port, String type) throws IOException {

        Process process2 = ExecShell.getExecShellProcess("ps -ef|grep " + type + "|grep -w " + port + "|grep -v grep|awk '{print $2}'");

        BufferedReader br2 = new BufferedReader(new InputStreamReader(process2.getInputStream(), "UTF-8"));
        String checkPid;
        while (true) {
            checkPid = br2.readLine();
            if (checkPid == null || checkPid.equals("")) {
                break;
            }
            String[] cmd1 = {"/bin/sh", "-c", "kill " + checkPid};
            String[] cmd2 = {"/bin/sh", "-c", "kill -9 " + checkPid};
            try {
                Runtime.getRuntime().exec(cmd1).waitFor();
                Runtime.getRuntime().exec(cmd2).waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        br2.close();
        process2.getOutputStream().close();
    }


    public static boolean isRunning(int port) throws IOException {
        boolean flag = false;
        Process process2 = ExecShell.getExecShellProcess("lsof -i:" + port + " -sTCP:LISTEN |awk '{print $10}'");
        BufferedReader br2 = new BufferedReader(new InputStreamReader(process2.getInputStream(), "UTF-8"));
        while (true) {
            String line2 = br2.readLine();
            if (line2 == null) {
                break;
            } else if (line2.equals("(LISTEN)")) {
                flag = true;
                return flag;
            }
        }
        br2.close();
        process2.getOutputStream().close();
        return flag;
    }

    /**
     * 判断返回值是否包含指定字段
     * @param command
     * @param str
     * @return
     * @throws IOException
     */
    public static Boolean resultHasStr(String command, String str) throws IOException {
        Process process = ExecShell.getExecShellProcess(command);
        if(StringUtils.isBlank(str)){
            throw new IllegalArgumentException("The string that needs to be matched is empty.");
        }
        String execResult = getAllResult(process);
        return StringUtils.isNotBlank(execResult) && execResult.contains(str);
    }

    public static int execLocal(String cmd) {
        String[] command = {"/bin/sh", "-c", cmd};
        log.info("start exec command: {}", Arrays.toString(command));
        int exitVal = 1;
        try {
            Process proc = Runtime.getRuntime().exec(command);
            printAllResult(proc);
            exitVal = proc.waitFor();
        } catch (Exception e) {
            log.error("exec command error,msg: {}", e.getMessage(), e);
        }
        return exitVal;
    }

    /**
     * 执行shell命令，指定超时时间
     * @param cmd 要执行的命令
     * @param timeout 超时时间值
     * @param unit 超时时间单位
     * @return 执行结果是否成功标志结果
     */
    public static int execLocalWithTimeout(String cmd, long timeout, TimeUnit unit){
        String[] command = {"/bin/sh", "-c", cmd};
        log.info("start exec command: {}", Arrays.toString(command));
        int exitVal = 1;
        try {
            //exec方法本质还是new ProcessBuilder().start()
//            Process proc = Runtime.getRuntime().exec(command);
            //使用processBuilder好处是可以重定向子进程的输入输出，将子进程IO和父进程IO独立，防止父进程挂掉导致子进程崩溃
            //以下设置子进程标准输出重定向到ProcessBuilder.Redirect.INHERIT或指定文件，并将错误输出合并到标准输出
            Process proc = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.PIPE).redirectErrorStream(true).start();
//            Process proc = new ProcessBuilder(command).redirectOutput(new File("/home/fkp/logs/Kms/Kms.log")).redirectErrorStream(true).start();
            printAllResult(proc);
            boolean flag = proc.waitFor(timeout, unit);
            if(flag){
                exitVal = proc.exitValue();
            }else {
                log.error("exec command timeout,value: {},timeUnit: {}", timeout, unit.toString());
            }
            log.info("destroy process.");
            proc.destroy();
        } catch (Exception e) {
            log.error("exec command error,msg: {}", e.getMessage(), e);
        }
        return exitVal;
    }

    private static String getAllResult(Process process){
        return getInfoResult(process) + getErrorResult(process);
    }

    private static String getInfoResult(Process process){
        return getResult(process.getInputStream());
    }

    private static String getErrorResult(Process process){
        return getResult(process.getErrorStream());
    }

    private static String getResult(InputStream in){
        StringBuffer buffer = new StringBuffer();
        try (BufferedReader infoReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))){
            String infoStr;
            while ((infoStr = infoReader.readLine()) != null){
                buffer.append(infoStr).append(SEPARATOR);
                log.info("print:{}", infoStr);
            }
        } catch (IOException e) {
            log.error("exec command get result error: {}", e.getMessage(), e);
        }
        return buffer.toString();
    }

    private static void printResult(Process process, String level){
        if(INFO_LEVEL.equals(level)){
            new Thread(() -> {
                log.info("exec command result: {}", getInfoResult(process));
            }).start();
        }else if (ERROR_LEVEL.equals(level)){
            new Thread(() -> {
                log.error("exec command result: {}", getErrorResult(process));
            }).start();
        }else if(ALL_LEVEL.equals(level)){
            new Thread(() -> {
                log.info("exec command result: {}", getAllResult(process));
            }).start();
        }else {
            throw new IllegalArgumentException("level argument error,the parameter must be one of info,error,all");
        }
    }

    private static void printAllResult(Process process){
        printResult(process, ALL_LEVEL);
    }


}
