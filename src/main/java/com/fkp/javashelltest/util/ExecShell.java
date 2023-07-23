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

    /**
     * 执行命令，阻塞等待执行结果
     * @param cmd 命令
     * @return 返回值
     */
    public static int execLocal(String cmd) {
        return execLocal(cmd, null);
    }

    /**
     * 执行命令，在超时时间内等待结果
     * @param cmd 命令
     * @param timeout 超时时间值
     * @param unit 超时时间单位
     * @return 返回值，若超时返回-1
     */
    public static int execLocalWithTimeout(String cmd, long timeout, TimeUnit unit){
        return execLocalWithTimeout(cmd, timeout, unit, null);
    }

    public static int execLocalWithTimeout(String cmd){
        return execLocalWithTimeout(cmd, DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
    }

    /**
     * 杀掉指定port的进程，kill -9
     * @param port 进程的端口号
     * @return 成功为true，失败为false
     */
    public synchronized static boolean killProcess(int port) {
        Process process = null;
        try {
            //netstat -ntlp | grep 8080 | grep LISTEN | awk '{print $7}'
            process = getProcess("netstat -ntlp | grep " + port + " | grep LISTEN | awk '{print $7}'");
            String info = getOutput(process);
            log.info("process info: {}", info);
            if(StringUtils.isNotBlank(info)){
                String pid = info.split("/")[0];
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


    /**
     * 判断某个端口是否被监听
     * @param port 被查询的端口号
     * @return 正在被监听为true，否则为false
     */
    public static boolean isRunning(int port) {
        //netstat -tl | grep 20133 | grep LISTEN | awk '{print $6}'
        //lsof -i:xxx -sTCP:LISTEN | awk '{print $10}'
        return resultHasStr("netstat -ntl | grep " + port + " | grep LISTEN | awk '{print $6}'", "LISTEN");
    }

    /**
     * 执行命令，判断子流程的输出是否包含指定字符串
     * @param cmd 命令
     * @param str 匹配字符串
     * @return 匹配为true，不匹配为false
     */
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

    /**
     * 执行脚本，阻塞等待，将子流程的输出重定向到指定文件
     * @param cmd 命令
     * @param filePath 文件路径
     * @return 子流程返回值
     */
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

    /**
     * 执行命令，在超时时间内阻塞等待，将子流程输出重定向到指定文件
     * @param cmd 命令
     * @param timeout 超时时间值
     * @param unit 超时时间单位
     * @param filePath 文件路径
     * @return 子流程返回值，若超时返回-1
     */
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

    /**
     * 执行命令，返回Process对象
     * @param command 命令
     * @return Process对象
     * @throws IOException 捕获异常
     */
    public static Process getProcess(String command) throws IOException {
        String[] cmd = {"/bin/sh", "-c", command};
        return new ProcessBuilder(cmd).redirectErrorStream(true).start();
    }

    /**
     * 执行命令，将子流程输出重定向到指定文件，返回Process对象
     * @param command 命令
     * @param filePath 文件路径
     * @return Process对象
     * @throws IOException 捕获异常
     */
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
