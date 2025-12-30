/**
 * 协议无关的消息适配器模块
 * <p>
 * 本模块提供了一套协议无关的消息处理机制，支持多种序列化格式：
 * <ul>
 *   <li>Protobuf - Google Protocol Buffers</li>
 *   <li>JSON - Jackson JSON</li>
 *   <li>Protostuff - 高性能序列化</li>
 * </ul>
 * </p>
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link org.markeb.net.message.IMessage} - 协议无关的消息接口</li>
 *   <li>{@link org.markeb.net.message.IMessageParser} - 消息解析器接口</li>
 *   <li>{@link org.markeb.net.message.IMessageHandler} - 消息处理器接口</li>
 *   <li>{@link org.markeb.net.message.UnifiedMessageDispatcher} - 统一消息分发器</li>
 * </ul>
 *
 * <h2>适配器</h2>
 * <ul>
 *   <li>{@link org.markeb.net.message.ProtobufMessageAdapter} - Protobuf 消息适配器</li>
 *   <li>{@link org.markeb.net.message.JsonMessageAdapter} - JSON 消息适配器</li>
 *   <li>{@link org.markeb.net.message.ProtostuffMessageAdapter} - Protostuff 消息适配器</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 注入消息解析器和分发器
 * @Autowired
 * private IMessageParser messageParser;
 *
 * @Autowired
 * private UnifiedMessageDispatcher dispatcher;
 *
 * // 2. 注册消息处理器
 * dispatcher.registerHandler(11000, (ctx, msg) -> {
 *     ReqLoginMessage login = (ReqLoginMessage) msg;
 *     // 处理登录请求
 *     return ResLoginMessage.newBuilder()
 *         .setSuccess(true)
 *         .build();
 * });
 *
 * // 3. 包装和发送消息
 * ReqLoginMessage request = ReqLoginMessage.newBuilder()
 *     .setOpenId("user123")
 *     .setToken("token456")
 *     .build();
 * IMessage message = messageParser.wrap(request);
 * channel.writeAndFlush(message);
 * }</pre>
 *
 * <h2>JSON 消息示例</h2>
 * <pre>{@code
 * // 定义 JSON 消息类
 * @MessageId(12000)
 * public class MyJsonMessage {
 *     private String name;
 *     private int value;
 *     // getters and setters
 * }
 *
 * // 注册和使用
 * messageParser.register(MyJsonMessage.class);
 * MyJsonMessage msg = new MyJsonMessage();
 * IMessage message = messageParser.wrap(msg);
 * }</pre>
 *
 * @see org.markeb.net.message.IMessage
 * @see org.markeb.net.message.IMessageParser
 * @see org.markeb.net.message.UnifiedMessageDispatcher
 */
package org.markeb.net.message;

