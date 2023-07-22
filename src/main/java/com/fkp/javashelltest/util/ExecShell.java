package com.fkp.javashelltest.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author fengkunpeng
 * @version 1.0
 * @description 调用shell工具类
 * @date 2023/7/21 11:05
 */
@Slf4j
public class ExecShell {

    public static final String SEPARATOR = System.getProperty("line.separator");
    public static final long DEFAULT_TIMEOUT = 10;
    public static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.SECONDS;

    public static int execLocal(String cmd) {
        return execLocal(cmd, null);
    }

    public static int execLocalWithTimeout(String cmd, long timeout, TimeUnit unit){
        return execLocalWithTimeout(cmd, timeout, unit, null);
    }

    public static int execLocalWithTimeout(String cmd){
        return execLocalWithTimeout(cmd, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
    }

    public synchronized static boolean killProcess(int port, String name) {
        Process process = null;
        try {
            process = getProcess("ps -ef|grep " + name + "|grep -w " + port + "|grep -v grep|awk '{print $2}'");
            String pid = getOutput(process);
            log.info("process id: {}", pid);
            if(StringUtils.isNotBlank(pid)){
                return execLocalWithTimeout("kill -9 " + pid) == 0;
            }
        }catch (Exception e){
            log.error("exec command error,msg: {}", e.getMessage(), e);
        }finally {
            log.info("destroy process: {}", process);
            destroy(process);
        }
        return false;
    }


    public static boolean isRunning(int port) {
        //netstat -tl | grep 20133 | grep LISTEN | awk '{print $6}'
        //lsof -i:xxx -sTCP:LISTEN | awk '{print $10}'
        return resultHasStr("netstat -tl | grep " + port + " | grep LISTEN | awk '{print $6}'", "LISTEN");
    }


    public static boolean resultHasStr(String cmd, String str) {
        log.info("exec command: {},hasStr: {}", cmd, str);
        if(StringUtils.isBlank(str)){
            throw new IllegalArgumentException("The string to be matched is empty.");
        }
        Process process = null;
        try {
            process = getProcess(cmd);
            BufferedReader reader = getBufferedReader(process);
            String info;
            while ((info = reader.readLine()) != null){
                if(info.contains(str)){
                    return true;
                }
            }
        }catch (Exception e){
            log.error("exec command error,command: {},msg: {}", cmd, e.getMessage(), e);
        }finally {
            destroy(process);
        }
        return false;
    }

    public static int execLocal(String cmd, String filePath) {
        log.info("start exec command: {}", cmd);
        int exitVal = -1;
        Process process = null;
        try {
            if(filePath == null){
                process = getProcess(cmd);
                printOutput(process);
            }else {
                process = getProcessOutput2File(cmd, filePath);
            }
            exitVal = process.waitFor();
        } catch (Exception e) {
            log.error("exec command error,msg: {}", e.getMessage(), e);
        }finally {
            destroy(process);
        }
        return exitVal;
    }

    public static int execLocalWithTimeout(String cmd, long timeout, TimeUnit unit, String filePath){
        log.info("start exec command: {}", cmd);
        int exitVal = -1;
        Process process = null;
        try {
            if(filePath == null){
                process = getProcess(cmd);
                printOutput(process);
            }else {
                process = getProcessOutput2File(cmd, filePath);
            }
            boolean flag = process.waitFor(timeout, unit);
            if(flag){
                exitVal = process.exitValue();
            }else {
                log.error("exec command timeout,value: {},timeUnit: {}", timeout, unit.toString());
            }
        } catch (Exception e) {
            log.error("exec command error,msg: {}", e.getMessage(), e);
        }finally {
            destroy(process);
        }
        return exitVal;
    }

    public static Process getProcess(String command) throws IOException {
        String[] cmd = {"/bin/sh", "-c", command};
        return new ProcessBuilder(cmd).redirectErrorStream(true).start();
    }

    public static Process getProcessOutput2File(String command, String filePath) throws IOException {
        String[] cmd = {"/bin/sh", "-c", command};
        return new ProcessBuilder(cmd).redirectOutput(new File(filePath)).redirectErrorStream(true).start();
    }

    private static void printOutput(Process process){
        new Thread(() -> {
            try (BufferedReader reader = getBufferedReader(process)){
                String info;
                while ((info = reader.readLine()) != null){
                    log.info("exec command output: {}", info);
                }
            } catch (Exception e) {
                log.error("print output error.", e);
            }
        }).start();
    }

    private static String getOutput(Process process){
        return getOutput(process, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
    }

    private static String getOutput(Process process, long timeout, TimeUnit unit){
        new Thread(() -> {
            try {
                boolean flag = process.waitFor(timeout, unit);
                if(!flag){
                    destroy(process);
                }
            } catch (InterruptedException e) {
                log.error("get output waitFor error,msg: {}", e.getMessage(), e);
            }
        }).start();
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader reader = getBufferedReader(process);
            String info;
            while ((info = reader.readLine()) != null){
                sb.append(info).append(SEPARATOR);
            }
        }catch (IOException ignore){
            //ignore
        }catch (Exception e){
            log.error("get output error,msg: {}", e.getMessage(), e);
        }
        return StringUtils.substringBeforeLast(sb.toString(), SEPARATOR);
    }

    private static void destroy(Process process){
        if(process != null){
            process.destroy();
        }
    }

    private static BufferedReader getBufferedReader(Process process){
        return new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    }


}
