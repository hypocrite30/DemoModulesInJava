package com.hypocrite30.netty.simple;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

/**
 * 1. 自定义一个Handler 需要继续 netty 规定好的某个 HandlerAdapter(规范)
 * 2. 这时自定义一个Handler , 才能称为一个handler
 * @Description: Netty 服务器自定义 handler
 * @Author: Hypocrite30
 * @Date: 2021/10/12 20:22
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 读取数据实际(可以读取客户端发送的消息)
     * @param ctx 上下文对象, 含有 管道pipeline, 通道channel, 地址
     * @param msg 客户端发送的数据 默认Object
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        System.out.println("Current Thread Name = " + Thread.currentThread().getName() + " Channle = " + ctx.channel());
        System.out.println("Server ctx = " + ctx);
        //Channel 和 Pipeline 的关系，通过断点从 ctx 取出 channel 和 pipeline，两者我中有你，你中有我，相互包含
        Channel channel = ctx.channel();
        ChannelPipeline channelPipeline = ctx.pipeline(); //本质是一个双向链表, 出站入站
        //将 msg 转成一个 ByteBuf
        //ByteBuf 是 Netty 提供的，不是 NIO 的 ByteBuffer.
        ByteBuf byteBuf = (ByteBuf) msg;
        System.out.println("Client sending message : " + byteBuf.toString(CharsetUtil.UTF_8));
        System.out.println("Client remote address: " + channel.remoteAddress());


        /**
         * NIOEventLoop 中含有一个 selector 和一个 TaskQueue
         * TaskQuque 任务队列常见用法：1.用户自定义普通任务 2.用户自定义定时任务
         *    3.非 Reactor 线程调用 Channel 各种方法「推送系统，找到channel，然后write向用户推送信息，最终会提交到任务队列被消费」
         */
        //比如这里有一个非常耗时长的业务-> 异步执行 -> 提交该channel 对应的
        //NIOEventLoop 的 TaskQueue 可以处理异步

        //解决方案1 用户程序自定义的普通任务
        //ctx获取channel，获取EventLoop，然后交给 TaskQueue 开一个线程去异步执行
        /*ctx.channel().eventLoop().execute(() -> {
            try {
                Thread.sleep(5 * 1000);
                ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端 2", CharsetUtil.UTF_8));
            } catch (Exception e) {
                System.out.println("发生异常" + e.getMessage());
            }
        });*/

        //解决方案2 : 用户自定义定时任务 -》 该任务是提交到 scheduleTaskQueue 中
        /*ctx.channel().eventLoop().schedule(() -> {
            try {
                Thread.sleep(5 * 1000);
                ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端 3", CharsetUtil.UTF_8));
            } catch (Exception e) {
                System.out.println("发生异常" + e.getMessage());
            }
        }, 5, TimeUnit.SECONDS);*/
    }

    /**
     * 数据读取完毕，执行操作
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        //writeAndFlush 是 write + flush
        //将数据写入到缓存，并刷新
        //一般讲，我们对这个发送的数据进行编码
        //Unpooled.copiedBuffer 返回非池化的ByteBuf
        ctx.writeAndFlush(Unpooled.copiedBuffer("Hi! Client ...", CharsetUtil.UTF_8));
    }

    /**
     * 处理异常, 一般是需要关闭通道
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}