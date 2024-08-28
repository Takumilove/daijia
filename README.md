# 项目总结

## 1 项目功能

* 项目包含三部分：**乘客端、司机端**和管理端

* 乘客端：

登录--选择代驾地址--呼叫代驾--等待接单--15分钟没有司机接单自动取消--15内有司机接单，司乘同显--账单支付

* 司机端

登录--认证--开始接单--抢单--开始代驾--生成账单，发送乘客



## 2 项目主要技术

* **微信小程序，微信开发者工具**

注册微信公众平台账号，小程序服务

安装微信开发者工具



* **idea导入后端代码**



* **MyBatisPlus**

操作数据库



* **SpringBoot + SpringCloud**

Gateway、Nacos、OpenFeign



* **Redis**

登录使用Redis

Redis里面Geo功能



* **规则引擎Drools**

代驾费用、分账、系统奖励



* **分布式锁**

分布式锁解决司机抢单，项目里面使用Redisson实现



* **分布式事务**

保证不同数据库里面数据的一致性

项目使用Seata框架

支付成功后处理



* **RabbitMQ**

保证数据最终一致性

支付处理



* **多线程**

CompletableFuture是java.util.concurrent包里面类

作用：原来使用串行执行的变为并行方式执行，提高代码执行速度



* **任务调度 XXL-JOB**



* 其他
![476ca3d4-59be-40ac-be08-5a918fdf7a83](https://github.com/user-attachments/assets/7e6f9402-9bcb-49af-b73b-de07b7250fde)
![b02e7f43-f115-496e-876f-f543ecf014cd](https://github.com/user-attachments/assets/8fc9d523-7867-41a8-b1c4-4466f10a5a0e)







