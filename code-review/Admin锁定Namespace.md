# 概述

Admin Service 锁定 Namespace 。可通过设置 ConfigDB 的 ServerConfig 的 "namespace.lock.switch" 为 "true" 开启。效果如下：

🙂 一次配置修改只能是一个人

😈 一次配置发布只能是另一个人

也就是说，开启后，一次配置修改并发布，需要两个人。

默认为 "false" ，即关闭。