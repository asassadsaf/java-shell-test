package com.fkp.javashelltest.controller;

import com.fkp.javashelltest.util.ExecShell;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author fengkunpeng
 * @version 1.0
 * @description 测试应用程序启动成功接口
 * @date 2023/7/21 11:05
 */
@RestController
public class UserController {

    @GetMapping(value = "/test", produces = MediaType.TEXT_PLAIN_VALUE)
    public String test(){
        return "test success!";
    }

    @GetMapping(value = "/execCommand")
    public int execCommand(String command){
        return ExecShell.execLocalWithTimeout(command, 10, TimeUnit.SECONDS);
    }

    @GetMapping(value = "/killProcess")
    public boolean killProcess(int port){
        return ExecShell.killProcess(port);
    }

    @GetMapping(value = "/isRunning")
    public boolean isRunning(int port){
        return ExecShell.isRunning(port);
    }
}
