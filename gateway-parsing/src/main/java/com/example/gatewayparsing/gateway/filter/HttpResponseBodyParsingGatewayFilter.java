package com.example.gatewayparsing.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class HttpResponseBodyParsingGatewayFilter extends AbstractGatewayFilterFactory<ModifyResponseBodyGatewayFilterFactory.Config> implements Ordered {
    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory;

    private int newBodyFieldRequestId = 0;

    /// # ModifyResponseBodyGatewayFilterFactory
    /// 응답 본문을 변환하기 위해 사용되는 Factory 클래스
    /// Mono 를
    public HttpResponseBodyParsingGatewayFilter(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory) {
        super(ModifyResponseBodyGatewayFilterFactory.Config.class);
        this.modifyResponseBodyFilterFactory = modifyResponseBodyFilterFactory;
    }

    @Override
    public GatewayFilter apply(ModifyResponseBodyGatewayFilterFactory.Config config) {
        final ModifyResponseBodyGatewayFilterFactory.Config modifyResponseBodyFilterFactoryConfig = new ModifyResponseBodyGatewayFilterFactory.Config();

        modifyResponseBodyFilterFactoryConfig.setRewriteFunction(Map.class, Map.class, (swe, bodyAsMap) -> {
            var request = swe.getRequest();
            HttpHeaders headers = request.getHeaders();
            MediaType contentType = request.getHeaders().getContentType();
            if (!MediaType.APPLICATION_JSON.equals(contentType)) {
                return Mono.just(bodyAsMap);
            }

            log.info("Url: {}", request.getURI().getPath());
            // Parsing 할때 Source IP 를 설정하기 위해 사용
            var ips = request.getRemoteAddress();
            if (ips != null) {
                headers = new HttpHeaders();
                headers.add("X-FORWARDED-FOR", ips.getAddress().getHostAddress());
            }

            Map<String, Object> newBody = this.parsingBody(bodyAsMap);

            var params = request.getQueryParams();
            log.info("Params: {}", params);
            log.warn("Response: {}", newBody);
            log.warn("Response code: {}", swe.getResponse().getStatusCode());
            return Mono.just(newBody);
        });
        return modifyResponseBodyFilterFactory.apply(modifyResponseBodyFilterFactoryConfig);
    }

    private Map<String, Object> parsingBody(Map<String, Object> bodyAsMap) {
        String newBodyData = "newBodyValue :" + newBodyFieldRequestId++;
        Map<String, Object> newBody = new HashMap<>();
        bodyAsMap.forEach((key, value) -> {
            if (Objects.equals(key, "postName")) {
                newBody.put("newPostName", value);
            } else {
                newBody.put(key, value);
            }
        });
        newBody.put("newBodyField", newBodyData);
        return newBody;
    }

    /**
     * Get the order value of this object.
     * <p>Higher values are interpreted as lower priority. As a consequence,
     * the object with the lowest value has the highest priority (somewhat
     * analogous to Servlet {@code load-on-startup} values).
     * <p>Same order values will result in arbitrary sort positions for the
     * affected objects.
     *
     * @return the order value
     * @see #HIGHEST_PRECEDENCE
     * @see #LOWEST_PRECEDENCE
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
