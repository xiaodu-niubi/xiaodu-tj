---
title: 默认模块
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# 默认模块

Base URLs:

# Authentication

# 会话接口

## POST 新建会话

POST /ais/session

新建会话

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|n|query|integer| 否 |生成热门问题的数量，默认为3|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{"code":200,"msg":"OK","data":{"sessionId":"718f331076e442059a10da26be5f292d","title":"Hello，我是天机AI助理","describe":"我是由天机学堂倾力打造的智能助理，我不仅能推荐课程、答疑解惑，还能为您激发创意、畅聊心事。","examples":[{"title":"知识讲解","describe":"面向对象的三大特征是什么？"},{"title":"课程介绍","describe":"《Java高级架构与微服务》能学到什么内容？"},{"title":"课程购买","describe":"《Java进阶编程实战》的课程价格是多少？"}]},"requestId":"0e3d0acb9c884a539424f857be3e0cb1"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» sessionId|string|true|none|会话id|none|
|»» title|string|true|none|欢迎页标题|none|
|»» describe|string|true|none|欢迎页描述|none|
|» requestId|string|true|none||none|

## GET 热门问题

GET /ais/session/hot

热门问题

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|n|query|integer| 否 |生成的数量，默认为3|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "data": [
        {
            "title": "学习资源",
            "describe": "能给我些配套的电子书资料吗？"
        },
        {
            "title": "课程作业",
            "describe": "提交编程作业后有自动批改功能吗？"
        },
        {
            "title": "课程难度",
            "describe": "我现在学的内容太简单了，能升级难度吗？"
        }
    ],
    "requestId": "26c38818c680444ea1d15ddef6657e1e"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|[object]|true|none||none|
|»» title|string|true|none||none|
|»» describe|string|true|none||none|
|» requestId|string|true|none||none|

# 聊天接口

## POST 聊天接口

POST /ais/chat

流式聊天接口。
响应结构说明：
1、每行数据，都是一个json数据
2、json中有eventData和eventType，两个属性
3、eventData：文本内容
4、eventType：事件类型，1001-数据事件，1002-停止时间，1003-参数事件

> Body 请求参数

```json
{
    "question":"能帮我推荐一个合适的课吗？",
    "sessionId":"f1dbf6ca0ed34eeda02ec0d0545a4429"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |none|
|body|body|object| 否 |none|
|» question|body|string| 是 |问题内容|
|» sessionId|body|string| 是 |会话id|

> 返回示例

> 200 Response

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» sessionId|string|true|none|会话id|none|
|»» title|string|true|none|欢迎页标题|none|
|»» describe|string|true|none|欢迎页描述|none|

## POST 停止生成

POST /ais/chat/stop

停止生成对话

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|sessionId|query|string| 是 |会话id|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "requestId": "4f85d26cbe3546a8a99dc96cc8eb5cbb"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|

## POST 文本聊天

POST /ais/chat/text

文本聊天接口，该接口不保存记录

> Body 请求参数

```
string

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |none|
|body|body|string| 否 |none|

> 返回示例

> 200 Response

```json
{"code":200,"msg":"OK","data":"我是一个人工智能助手，旨在回答你的问题和提供信息。有什么我可以帮助你的吗？","requestId":"4f0184c761bc4a7d9997f9379bed60bd"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|string|true|none||none|
|» requestId|string|true|none||none|

## GET 模版列表

GET /ais/chat/templates

查询系统设定的模版，用于调用通用文本聊天接口使用

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{"code":200,"msg":"OK","data":{"associationalWord":"请根据用户提供的核心关键词，按照以下要求生成3个关联问题：\n1. 用户输入关键词：$input\n2. 生成规则：生成的每一个关联问题必须包含【$input】，不能重复，必须是中文，字数不超过20个\n3. 输出要求，输出纯文本内容，每个关联问题之间用|分隔，格式：xxx|xxx|xxx\n","helpedWrite":"AI帮写\n","continuedWrite":"AI续写\n","polish":"AI润色\n","streamline":"AI精简\n"},"requestId":"e77886fbc90d405496b98e79187482bf"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» associationalWord|string|true|none||none|
|»» helpedWrite|string|true|none||none|
|»» continuedWrite|string|true|none||none|
|»» polish|string|true|none||none|
|»» streamline|string|true|none||none|
|» requestId|string|true|none||none|

# 历史会话

## GET 查询会话详情

GET /ais/session/{sessionId}

查询历史聊天记录

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|sessionId|path|string| 是 |会话id|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "data": [
        {
            "type": "USER",
            "content": "我要点餐"
        },
        {
            "type": "ASSISTANT",
            "content": "您好！现在是晚上，我建议您可以尝试吃一碗热乎的面条或是混沌，既暖胃又营养。您觉得如何？"
        },
        {
            "type": "USER",
            "content": "我要查询订单"
        },
        {
            "type": "ASSISTANT",
            "content": "请您提供订单号或下单人姓名，以便我为您查询订单。"
        }
    ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|[object]|true|none||none|
|»» type|string|true|none||none|
|»» content|string|true|none||none|

#### 枚举值

|属性|值|
|---|---|
|type|USER|
|type|ASSISTANT|

## GET 查询历史会话

GET /ais/session/history

查询最近历史会话，按照更新时间倒序排序，最多提供30条记录

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{"code":200,"msg":"OK","data":{"1年以上":[{"sessionId":"03b6491d3a1949c98cf0f8c37aa623fc","title":"水水水水谁谁谁水水水水谁谁谁水水水水水水水水","updateTime":"2023-02-26 15:45:31"}],"最近1年":[{"sessionId":"53349594acff4a0fb92f71541491dc1b","title":"帮我推荐课程","updateTime":"2025-01-18 21:33:55"},{"sessionId":"695fdea704254c089da454133a1c17a8","title":"你是谁","updateTime":"2025-01-18 21:33:37"}],"最近30天":[{"sessionId":"e380350f97174313898c214afb37d6d8","title":"22222","updateTime":"2025-02-25 13:44:44"}],"当天":[{"sessionId":"fa046bdb4ffe48fba4915e490e1e0b0e","title":"xxxxxx","updateTime":"2025-02-26 15:44:01"}]},"requestId":"bc8d535241104da7802e5d27f229d219"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» 1年以上|[object]|false|none||none|
|»»» sessionId|string|false|none||none|
|»»» title|string|false|none||none|
|»»» updateTime|string|false|none||none|
|»» 最近1年|[object]|false|none||none|
|»»» sessionId|string|true|none||none|
|»»» title|string|true|none||none|
|»»» updateTime|string|true|none||none|
|»» 最近30天|[object]|false|none||none|
|»»» sessionId|string|false|none||none|
|»»» title|string|false|none||none|
|»»» updateTime|string|false|none||none|
|»» 当天|[object]|false|none||none|
|»»» sessionId|string|false|none||none|
|»»» title|string|false|none||none|
|»»» updateTime|string|false|none||none|
|» requestId|string|true|none||none|

## PUT 更新历史会话标题

PUT /ais/session/history

更新历史会话标题

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|sessionId|query|string| 是 |会话id|
|title|query|string| 是 |对话标题，最大长度100|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "requestId": "3c84e5b45b5a45919dd11b89e98be8c5"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» requestId|string|true|none||none|

## DELETE 删除历史会话

DELETE /ais/session/history

删除历史会话

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|sessionId|query|string| 否 |会话id|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{"code":200,"msg":"OK","requestId":"fa9164cdd018482ea0a69c3a1fb6392c"}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» requestId|string|true|none||none|

# 知识库

## GET 文本转向量

GET /ais/embedding

文本转向量

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|message|query|string| 是 |会话id|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "requestId": "4f85d26cbe3546a8a99dc96cc8eb5cbb"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|

## POST 保存文本到向量库

POST /ais/embedding

保存文本到向量库

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|messages|query|array[string]| 是 |会话id|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "requestId": "4f85d26cbe3546a8a99dc96cc8eb5cbb"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|

## DELETE 删除文本

DELETE /ais/embedding

保存文本到向量库

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|ids|query|array[string]| 是 |会话id|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "requestId": "4f85d26cbe3546a8a99dc96cc8eb5cbb"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|

## GET 内容搜索

GET /ais/embedding/search

内容搜索

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|message|query|string| 是 |会话id|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "requestId": "4f85d26cbe3546a8a99dc96cc8eb5cbb"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|

## GET 搜索全部

GET /ais/embedding/search/all

搜索全部

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |none|

> 返回示例

> 200 Response

```json
{
    "code": 200,
    "msg": "OK",
    "requestId": "4f85d26cbe3546a8a99dc96cc8eb5cbb"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|

# 语音接口

## POST 语音转文字

POST /ais/audio/stt

语音文件转文本，仅支持中文，文件格式为wav

> Body 请求参数

```yaml
audioFile: ""

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |none|
|body|body|object| 否 |none|
|» audioFile|body|string(binary)| 否 |none|

> 返回示例

> 200 Response

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» sessionId|string|true|none|会话id|none|
|»» title|string|true|none|欢迎页标题|none|
|»» describe|string|true|none|欢迎页描述|none|

## POST 文本转语音

POST /ais/audio/tts

文字换语音

> Body 请求参数

```
文本转语音示例文本

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |none|
|body|body|string| 否 |none|

> 返回示例

> 200 Response

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» sessionId|string|true|none|会话id|none|
|»» title|string|true|none|欢迎页标题|none|
|»» describe|string|true|none|欢迎页描述|none|

## POST 文本转语音（流式）

POST /ais/audio/tts-stream

文字换语音

> Body 请求参数

```
文本转语音示例文本

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |none|
|body|body|string| 否 |none|

> 返回示例

> 200 Response

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||none|
|» msg|string|true|none||none|
|» data|object|true|none||none|
|»» sessionId|string|true|none|会话id|none|
|»» title|string|true|none|欢迎页标题|none|
|»» describe|string|true|none|欢迎页描述|none|

# 数据模型

