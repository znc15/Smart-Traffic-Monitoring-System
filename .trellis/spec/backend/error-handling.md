# Error Handling

> How errors are handled in this project.

---

## Overview

后端使用 Spring Boot 全局异常处理机制。Controller 层抛出业务异常，由 `@RestControllerAdvice` 统一捕获并格式化响应。

---

## Error Types

### AppException

`com.smarttraffic.backend.exception.AppException` 是统一的业务异常类：

```java
public class AppException extends RuntimeException {
    private final HttpStatus status;
    public AppException(HttpStatus status, String message) { ... }
}
```

使用方式：
```java
throw new AppException(HttpStatus.NOT_FOUND, "对话不存在");
throw new AppException(HttpStatus.FORBIDDEN, "无权访问此对话");
throw new AppException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
```

**禁止在 Controller 中直接抛 `RuntimeException`**。所有业务异常必须用 `AppException`。

---

## Error Handling Patterns

### ApiExceptionHandler

`com.smarttraffic.backend.exception.ApiExceptionHandler` 通过 `@RestControllerAdvice` 注册全局异常处理器：

| 处理的异常类型 | HTTP 状态码 | 日志级别 |
|---|---|---|
| `AppException` | 由异常的 `status` 决定 | 4xx → warn, 5xx → error |
| `MethodArgumentNotValidException` | 400 | — |
| `HandlerMethodValidationException` | 400 | — |
| `ConstraintViolationException` | 400 | — |
| `Exception`（兜底） | 500 | error |

### 响应格式

所有错误响应统一为 JSON：
```json
{ "detail": "错误描述" }
```

前端 `authFetch` 不自动抛异常（返回原始 `Response`），由调用方通过 `res.ok` 判断并使用 `getErrorDetail()` 提取错误信息。

---

## SSE 流式异常处理

AI 聊天的 SSE 流在异步线程中执行，异常处理有特殊要求：

1. **Controller 层参数校验**：在进入异步之前用 `AppException` 校验（如消息为空、对话不存在），这些会被全局异常处理器正常捕获。

2. **异步线程中的异常**：在 `executor.execute()` 内部 try-catch，通过 `SseEmitter` 发送 `error` 事件：
   ```java
   catch (Exception e) {
       emitter.send(SseEmitter.event().name("error").data("AI 服务异常: " + e.getMessage()));
       emitter.completeWithError(e);
   }
   ```

3. **前端处理**：composable 在 SSE 解析循环中检测 `event:error`，用 `toast.error()` 展示给用户。

4. **SSE 发送失败**：chunk 发送失败（如客户端已断开）仅 warn 日志，不中断循环，让流自然结束。

---

## API Error Responses

### 标准 REST 端点

返回 `{ "detail": "..." }` JSON，前端通过 `res.ok` + `res.json()` 处理。

### SSE 端点

SSE 流内的错误通过事件类型区分：
- `event:chunk` — 正常内容片段
- `event:error` — 服务端异常，data 字段包含错误描述
- `event:done` — 流结束标记

前端 composable 按事件名路由处理。

---

## Common Mistakes

1. **在 SSE 异步线程中抛 AppException**：`@RestControllerAdvice` 无法捕获异步线程中的异常，必须在 `executor.execute()` 内自行 try-catch。
2. **SseEmitter 超时设置过短**：AI 生成响应较慢时容易超时，当前设为 120 秒。
3. **前端忘记处理 AbortError**：用户主动停止生成不应弹出错误 toast，需判断 `e.name === 'AbortError'`。