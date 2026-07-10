package net.lavacro.finances.agent.config;

import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.resources.LoopResources;

@Configuration
public class NettyConfig {
	@Bean
	public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
		return container -> container.addServerCustomizers(server ->
				server.runOn(LoopResources.create("my-app-netty", 1, true))
		);
	}
}
