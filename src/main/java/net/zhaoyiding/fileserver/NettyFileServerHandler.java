package net.zhaoyiding.fileserver;

import java.io.File;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class NettyFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private String root;

	public NettyFileServerHandler(String root) {
		super();
		this.root = root;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		if (ctx.channel().isActive()) {
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (request.decoderResult().isFailure()) {
			sendError(ctx, HttpResponseStatus.BAD_REQUEST);
		}

		if (request.method() != HttpMethod.GET) {
			sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
			return;
		}

		String uri = request.uri();
		String fileName = uri.substring(1);
		File file = new File(root, fileName);

		if (file.isHidden() || !file.exists()) {
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
			return;
		}

		FileRegion region = new DefaultFileRegion(file, 0, file.length());

		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM);
		response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, HttpHeaderValues.ATTACHMENT);
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());

		if (HttpUtil.isKeepAlive(request)) {
			HttpUtil.setKeepAlive(response, true);
		}

		ctx.writeAndFlush(response);
		ChannelFuture sendFileFuture = ctx.writeAndFlush(region, ctx.newProgressivePromise());
		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {

			@Override
			public void operationComplete(ChannelProgressiveFuture future) throws Exception {
				System.out.println("transfer complete");
			}

			@Override
			public void operationProgressed(ChannelProgressiveFuture future, long progress, long total)
					throws Exception {
				if (total < 0) {
					System.err.println("transfer progress: " + total);
				} else {
					System.err.println("transfer progress: " + progress + "/" + total);
				}
			}
		});

		if (!HttpUtil.isKeepAlive(request)) {
			sendFileFuture.addListener(ChannelFutureListener.CLOSE);
		}

	}

	private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
				Unpooled.copiedBuffer("Failure" + status.toString() + "\r\n", CharsetUtil.UTF_8));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

}
