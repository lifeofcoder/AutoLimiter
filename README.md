# auto-limiter
Sentinel自动限流降级的集群方案。主要计划解决如下两个问题：
* 通过Ignite实现控制台集群方案，解决单点故障问题 (done)
* 基于动态集群流控的高可用高并发高性能的Token Server (done)

## autolimiter-common(done)
自动限流降级方案公共包。供集群限流和本地限流使用。

## autolimiter-client(done)
自动限流器的客户端核心jar包。接入控制台集群的客户端需要引入该jar包。
当前该jar主要有如下两个功能：
* 配置集群控制台的ip地址
* 随机选择向集群发送心跳，并处理控制台集群宕机等场景

## autolimiter-client-demo(done)
客户端Demo是笔者写的一个sentinel的客户端Demo。其引入了autolimiter-client的jar包，该Demo客户端可以接入控制台集群。
其主要如下两个功能：
* 支持接入控制台集群
* 内部有线程不断模拟用户请求，从而可以在控制台查看到其实时请求数据
* 代码中配置限流规则，集群流控，集群节点信息

## autolimiter-dashboard-cluster(done)
Sentinel控制台集群方案.基于Ignite实现控制台集群，解决了Sentinel自带控制台的单点故障问题。
其具体原理和基本使用请访问：[集群控制台原理与基本使用](https://blog.csdn.net/hilaryfrank/article/details/107963175 '原理与使用')

## autolimiter-cluster-parent(done)
高性能高可用集群限流解决方案。
其具体原理和基本使用请访问：[Sentinel高性能高可用集群限流解决方案](https://blog.csdn.net/hilaryfrank/article/details/125879672 '原理与使用')

### autolimiter-cluster-common（done）
Sentinel集群限流的公共包

### autolimiter-cluster-cluster（done）
Sentinel集群限流Token Server。基于动态集群流控的高可用高并发高性能的Token Server。其支持如下功能：
* 动态集群流控，即动态计算并返回最新的单机流控数据给客户端实现实时动态集群流控
* 普通集群流控，即每次客户端都实时请求TokenServer，实时判断集群流控

### autolimiter-cluster-client (done)
Sentinel集群流控客户端，和Token Server结合起来实现集群流控。其主要功能：
* 根据配置可动态选择集群流控方式：动态还是普通
* 对接Token Server支持容错策略，当Token Server可用率低于阈值（默认95%）时，自动降级到本地流控


## License
[Apache License 2.0](https://github.com/lifeofcoder/dynamic-executor/blob/master/LICENSE)
禁止商用，个人引用请标明出处。

## 联系我
如果有任何疑问或者高见，欢迎添加微信公众号"Life of Coder"共同交流探讨。

<p align="center"><img width="40%" src="https://img-blog.csdnimg.cn/20191128202145538.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2hpbGFyeWZyYW5r,size_16,color_FFFFFF,t_70" /></p>
