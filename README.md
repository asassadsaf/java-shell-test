Java创建操作系统进程，完成执行命令或脚本操作
1.Process proc = Runtime.getRuntime().exec(command)    本质还是通过new ProcessBuilder(command).start()
2.Process proc = new ProcessBuilder(command).start()   这种方式可以配置更多子流程的配置参数，例如重定向设置


设置子流程的重定向输出/输入，一般只关心输出
1.ProcessBuilder.redirectOutput(Redirect/File)    设置子流程的标准输出目标，默认Redirect.PIPE   
2.ProcessBuilder.redirectError(Redirect/File)     设置子流程的错误输出目标，默认Redirect.PIPE   
3.ProcessBuilder.redirectInput(Redirect/File)     设置子流程的输入源，默认Redirect.PIPE   
4.ProcessBuilder.redirectErrorStream(boolean)     是否将错误输出合并到标准输出，默认false
这里一般设置为redirectErrorStream(true)，其他选择默认，通过java进程来获取子流程的输出，并写入java日志。
在特殊场景下可能需要更改redirectOutput为java日志文件或其他日志文件，例如当子流程将java进程杀死了。

重定向选项Redirect
1.Redirect.PIPE        指示子进程I/O将通过管道连接到当前Java进程。这是子流程标准I/O的默认处理。
通过Process.getInputStream可以获取到子进程的输出内容，正因为子进程IO通过管道连接到了java进程。
2.Redirect.INHERIT     指示子流程I/O源或目标与当前流程的I/O源或目的相同。这是大多数操作系统命令解释器（shell）的正常行为。
无法通过Process.getInputStream获取脚本内容，如果前台启动java进程，则子进程的输出会输出到控制台，正因为子流程I/O源或目标与当前流程的I/O源或目的相同


使用场景和细节解释
1.通过java启动子流程将该java进程杀掉，并执行重启操作，此时需要设置子流程的标准输出为Redirect.INHERIT或输出到文件，
让子流程和当前流程IO独立，而不采用Redirect.PIPE，因为其子进程I/O将通过管道连接到当前Java进程，当java进程被子流程杀掉后，
相当于两者的IO管道断开，这会导致子流程崩溃，无法执行后面的启动命令。
2.通过Process.getInputStream获取的输入流在子流程没有执行完成时，且此时子流程没有输出任何内容，执行InputStream.read方法都会被阻塞，
直到子流程执行完成后自动销毁，也可以手动执行Process.destroy方法销毁子流程，例如在使用waitFor且指定超时时间后，
若在限制的时间内子流程没有结束，也就不会获取的子流程退出的返回值，此时可以手动销毁子流程，除非程序不在意子流程的退出值。
