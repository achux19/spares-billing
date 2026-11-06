package com.sparesbilling.backend.feature;

import com.sparesbilling.backend.tenant.AppPrincipal;
import com.sparesbilling.backend.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/** Refuses calls to a module the shop does not have. Hiding menus is not enough, the server enforces it. */
@Component
@RequiredArgsConstructor
public class FeatureInterceptor implements HandlerInterceptor {

    private record Rule(Pattern path, Feature feature) {}

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("^/api/(purchases|suppliers)(/.*)?$"), Feature.PURCHASES),
            new Rule(Pattern.compile("^/api/quotations(/.*)?$"), Feature.QUOTATIONS),
            new Rule(Pattern.compile("^/api/credit-notes(/.*)?$"), Feature.CREDIT_NOTES),
            new Rule(Pattern.compile("^/api/invoices/[^/]+/eway(/.*)?$"), Feature.EWAY),
            new Rule(Pattern.compile("^/api/reports(?!/dashboard($|/))(/.*)?$"), Feature.REPORTS)
    );

    private final FeatureService featureService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        AppPrincipal p = TenantContext.principal();
        if (p == null || p.getShopId() == null) return true;       // not a shop user: other rules decide

        String path = request.getRequestURI();
        for (Rule rule : RULES) {
            if (rule.path().matcher(path).matches()) {
                if (!featureService.enabledFor(p.getShopId()).contains(rule.feature())) {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"" + rule.feature().label()
                            + " is not enabled for your shop. Please contact support.\"}");
                    return false;
                }
                break;
            }
        }
        return true;
    }
}
