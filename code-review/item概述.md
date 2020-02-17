Item配置项,是namespace下最小颗粒度的单位.在namespace下分成5种类型
properties/yml/yaml/json/xml

其中properties:每一行对应一条item记录.
后四者: 无法进行拆分,所以一个Namespace仅仅对应一条item记录.

```json
// 已经使用 http://tool.oschina.net/codeformat/json/ 进行格式化，实际是**紧凑型**
{
    "createItems": [ ], 
    "updateItems": [
        {
            "oldItem": {
                "namespaceId": 32, 
                "key": "key4", 
                "value": "value4123", 
                "comment": "123", 
                "lineNum": 4, 
                "id": 15, 
                "isDeleted": false, 
                "dataChangeCreatedBy": "apollo", 
                "dataChangeCreatedTime": "2018-04-27 16:49:59", 
                "dataChangeLastModifiedBy": "apollo", 
                "dataChangeLastModifiedTime": "2018-04-27 22:37:52"
            }, 
            "newItem": {
                "namespaceId": 32, 
                "key": "key4", 
                "value": "value41234", 
                "comment": "123", 
                "lineNum": 4, 
                "id": 15, 
                "isDeleted": false, 
                "dataChangeCreatedBy": "apollo", 
                "dataChangeCreatedTime": "2018-04-27 16:49:59", 
                "dataChangeLastModifiedBy": "apollo", 
                "dataChangeLastModifiedTime": "2018-04-27 22:38:58"
            }
        }
    ], 
    "deleteItems": [ ]
}
```

# Protal
在【添加配置项】的界面中，点击【提交】按钮，调用创建 Item 的 API .
![](http://static.iocoder.cn/images/Apollo/2018_03_20/02.png)

# 彩蛋
Commit 的设计，在我们日常的管理后台，对重要数据的变更，可以作为参考。

ConfigTextResolver 的设计，值得我们在业务系统开发学习。🙂 很多时候，我们习惯性把大量的逻辑，全部写在 Service 类中。