package com.sparesbilling.backend;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two-shop isolation test. It starts the real application against a THROWAWAY database, creates two
 * shops through the Super Admin API, and then tries to read and change one shop's data as the other.
 *
 * It only runs when TEST_DB_URL is set and the database name ends in _test. It WIPES that database.
 *
 *   TEST_DB_URL=jdbc:postgresql://localhost:5432/spares_billing_test ./mvnw test -Dtest=TenantIsolationTest
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".*_test.*")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
        "server.port=18089",
        "spring.datasource.url=${TEST_DB_URL}",
        "spring.datasource.username=${TEST_DB_USER:spares}",
        "spring.datasource.password=${TEST_DB_PASSWORD:spares_dev_pass}",
        "app.jwt.secret=" + TenantIsolationTest.SECRET,
        "app.initial-owner.username=owner1",
        "app.initial-owner.password=Passw0rd!1",
        "app.super-admin.username=super",
        "app.super-admin.password=SuperPass1"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantIsolationTest {

    static final String SECRET = "isolation-test-secret-key-0123456789-abcdefghijklmnop";
    private static final String BASE = "http://localhost:18089";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    // Wipe the throwaway database BEFORE Spring starts (Flyway then rebuilds it from V1..latest).
    static {
        String url = System.getenv("TEST_DB_URL");
        if (url != null && url.matches(".*/[A-Za-z0-9_]*_test(\\?.*)?$")) {
            String user = System.getenv().getOrDefault("TEST_DB_USER", "spares");
            String pass = System.getenv().getOrDefault("TEST_DB_PASSWORD", "spares_dev_pass");
            try (Connection c = DriverManager.getConnection(url, user, pass); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA public CASCADE");
                s.execute("CREATE SCHEMA public");
            } catch (Exception e) {
                throw new IllegalStateException("Could not reset the test database", e);
            }
        }
    }

    // state shared by the ordered tests
    static String superTok, owner1Tok, owner2Tok;
    static long shop2Id, p1, p2, c1, s1, inv1, owner1UserId;
    static String inv1No, inv2No;

    // ------------------------------------------------------------------ helpers

    record Resp(int status, String body) {}

    static Resp call(String method, String path, String token, String json) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                    .header("Content-Type", "application/json");
            if (token != null) b.header("Authorization", "Bearer " + token);
            b.method(method, json == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Resp(r.statusCode(), r.body());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String str(String body, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
        return m.find() ? m.group(1) : null;
    }

    static long num(String body, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(body);
        assertTrue(m.find(), "no '" + key + "' in: " + body);
        return Long.parseLong(m.group(1));
    }

    static BigDecimal money(String body, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[0-9.]+)").matcher(body);
        assertTrue(m.find(), "no '" + key + "' in: " + body);
        return new BigDecimal(m.group(1));
    }

    static String login(String shop, String user, String pass) {
        Resp r = call("POST", "/api/auth/login", null,
                "{\"shopCode\":\"" + shop + "\",\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}");
        assertEquals(200, r.status(), "login " + shop + "/" + user + " -> " + r.body());
        String t = str(r.body(), "token");
        assertNotNull(t);
        return t;
    }

    static void expect(int status, Resp r, String what) {
        assertEquals(status, r.status(), what + " -> " + r.status() + " " + r.body());
    }

    /** Refused: any 4xx. The one thing it must never be is a 2xx (data exposed) or 5xx (guard tripped). */
    static void refused(Resp r, String what) {
        assertTrue(r.status() >= 400 && r.status() < 500, what + " must be refused with 4xx but was " + r.status() + " " + r.body());
    }

    static final String PRODUCT_TMPL =
            "{\"partNumber\":\"%s\",\"name\":\"%s\",\"gstRate\":18,\"mrp\":%s,\"purchasePrice\":%s,\"sellingPrice\":%s,\"openingStock\":100}";

    // ------------------------------------------------------------------ setup

    @Test @Order(1)
    void setUpTwoShops() {
        superTok = login("", "super", "SuperPass1");
        owner1Tok = login("main", "owner1", "Passw0rd!1");

        Resp created = call("POST", "/api/admin/shops", superTok,
                "{\"code\":\"shop2\",\"name\":\"Second Shop\",\"stateCode\":\"33\",\"ownerUsername\":\"owner2\","
                        + "\"ownerFullName\":\"Owner Two\",\"ownerPassword\":\"Passw0rd!2\","
                        + "\"features\":[\"PURCHASES\",\"QUOTATIONS\",\"CREDIT_NOTES\",\"EWAY\",\"REPORTS\"]}");
        expect(201, created, "create shop2");
        shop2Id = num(created.body(), "id");
        owner2Tok = login("shop2", "owner2", "Passw0rd!2");
    }

    @Test @Order(2)
    void eachShopCreatesItsOwnData() {
        Resp r1 = call("POST", "/api/products", owner1Tok,
                String.format(PRODUCT_TMPL, "ISO-1", "Shop one oil", "1200", "700", "1000"));
        expect(201, r1, "shop1 product");
        p1 = num(r1.body(), "id");
        Resp r2 = call("POST", "/api/products", owner2Tok,
                String.format(PRODUCT_TMPL, "ISO-2", "Shop two oil", "250", "100", "200"));
        expect(201, r2, "shop2 product");
        p2 = num(r2.body(), "id");

        Resp c = call("POST", "/api/customers", owner1Tok, "{\"name\":\"Shop One Customer\"}");
        expect(201, c, "shop1 customer");
        c1 = num(c.body(), "id");
        Resp s = call("POST", "/api/suppliers", owner1Tok, "{\"name\":\"Shop One Supplier\"}");
        expect(201, s, "shop1 supplier");
        s1 = num(s.body(), "id");

        Resp i1 = call("POST", "/api/invoices", owner1Tok,
                "{\"items\":[{\"productId\":" + p1 + ",\"quantity\":3}]}");
        expect(201, i1, "shop1 invoice");
        inv1 = num(i1.body(), "id");
        inv1No = str(i1.body(), "invoiceNo");
        Resp i2 = call("POST", "/api/invoices", owner2Tok,
                "{\"items\":[{\"productId\":" + p2 + ",\"quantity\":1}]}");
        expect(201, i2, "shop2 invoice");
        inv2No = str(i2.body(), "invoiceNo");

        // numbering is per shop: both shops start at 00001
        assertTrue(inv1No.endsWith("/00001"), "shop1 first invoice number: " + inv1No);
        assertTrue(inv2No.endsWith("/00001"), "shop2 first invoice number: " + inv2No);
    }

    // ------------------------------------------------------------------ reading across shops

    @Test @Order(3)
    void otherShopCannotReadByIdOrList() {
        refused(call("GET", "/api/products/" + p1, owner2Tok, null), "GET product of other shop");
        refused(call("GET", "/api/customers/" + c1, owner2Tok, null), "GET customer of other shop");
        refused(call("GET", "/api/suppliers/" + s1, owner2Tok, null), "GET supplier of other shop");
        refused(call("GET", "/api/invoices/" + inv1, owner2Tok, null), "GET invoice of other shop");

        Resp products = call("GET", "/api/products?size=100&includeInactive=true", owner2Tok, null);
        expect(200, products, "list products");
        assertFalse(products.body().contains("ISO-1"), "shop2 product list leaks shop1: " + products.body());
        assertTrue(products.body().contains("ISO-2"));

        Resp customers = call("GET", "/api/customers?size=100&includeInactive=true", owner2Tok, null);
        assertFalse(customers.body().contains("Shop One Customer"), "customer list leaks: " + customers.body());
        Resp suppliers = call("GET", "/api/suppliers?size=100&includeInactive=true", owner2Tok, null);
        assertFalse(suppliers.body().contains("Shop One Supplier"), "supplier list leaks: " + suppliers.body());

        Resp invoices = call("GET", "/api/invoices?size=100", owner2Tok, null);
        expect(200, invoices, "list invoices");
        assertEquals(1L, num(invoices.body(), "totalElements"), "shop2 sees invoices of shop1: " + invoices.body());

        Resp moves = call("GET", "/api/products/" + p1 + "/stock-movements", owner2Tok, null);
        assertTrue(moves.status() >= 400 || moves.body().trim().equals("[]"),
                "stock movements of other shop's product leak: " + moves.status() + " " + moves.body());
    }

    @Test @Order(4)
    void otherShopCannotChangeOrUseData() {
        refused(call("PUT", "/api/products/" + p1, owner2Tok,
                String.format(PRODUCT_TMPL, "HACK", "Hacked", "1", "1", "1")), "PUT product of other shop");
        refused(call("PATCH", "/api/products/" + p1 + "/active?value=false", owner2Tok, null),
                "deactivate product of other shop");
        refused(call("PUT", "/api/customers/" + c1, owner2Tok, "{\"name\":\"Hacked\"}"), "PUT customer of other shop");
        refused(call("POST", "/api/invoices/" + inv1 + "/cancel", owner2Tok, "{\"reason\":\"hack\"}"),
                "cancel invoice of other shop");
        refused(call("POST", "/api/invoices", owner2Tok,
                "{\"items\":[{\"productId\":" + p1 + ",\"quantity\":1}]}"), "sell a product of the other shop");
        refused(call("POST", "/api/payments", owner2Tok,
                "{\"customerId\":" + c1 + ",\"amount\":1,\"method\":\"CASH\"}"), "payment for customer of other shop");

        // shop1's data is untouched
        Resp p = call("GET", "/api/products/" + p1, owner1Tok, null);
        expect(200, p, "shop1 product still readable");
        assertEquals("Shop one oil", str(p.body(), "name"));
        Resp i = call("GET", "/api/invoices/" + inv1, owner1Tok, null);
        assertEquals("ACTIVE", str(i.body(), "status"));
    }

    @Test @Order(5)
    void reportsOnlyCountOwnShop() {
        Resp d1 = call("GET", "/api/reports/dashboard", owner1Tok, null);
        Resp d2 = call("GET", "/api/reports/dashboard", owner2Tok, null);
        expect(200, d1, "dashboard 1");
        expect(200, d2, "dashboard 2");
        assertEquals(0, money(d1.body(), "salesTotal").compareTo(new BigDecimal("3000")), "shop1 sales: " + d1.body());
        assertEquals(0, money(d2.body(), "salesTotal").compareTo(new BigDecimal("200")), "shop2 sales leak: " + d2.body());
        assertEquals(1L, num(d2.body(), "invoiceCount"), "shop2 invoice count: " + d2.body());

        for (String path : new String[]{"/api/reports/sales", "/api/reports/gst", "/api/reports/stock",
                "/api/reports/top-products", "/api/reports/profit"}) {
            Resp r = call("GET", path, owner2Tok, null);
            expect(200, r, "report " + path);
            assertFalse(r.body().contains("ISO-1") || r.body().contains("Shop one"),
                    "report " + path + " leaks shop1: " + r.body());
        }
        Resp stock2 = call("GET", "/api/reports/stock", owner2Tok, null);
        Resp stock1 = call("GET", "/api/reports/stock", owner1Tok, null);
        assertFalse(stock1.body().equals(stock2.body()), "both shops report identical stock");
    }

    // ------------------------------------------------------------------ users and login

    @Test @Order(6)
    void usersAreSeparatePerShop() {
        Resp list2 = call("GET", "/api/users", owner2Tok, null);
        expect(200, list2, "users of shop2");
        assertTrue(list2.body().contains("\"owner2\""));
        assertFalse(list2.body().contains("\"owner1\""), "shop2 sees shop1 users: " + list2.body());

        Resp list1 = call("GET", "/api/users", owner1Tok, null);
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)\\s*,\\s*\"username\"\\s*:\\s*\"owner1\"").matcher(list1.body());
        assertTrue(m.find(), "owner1 id not found in " + list1.body());
        owner1UserId = Long.parseLong(m.group(1));

        refused(call("PUT", "/api/users/" + owner1UserId, owner2Tok,
                "{\"fullName\":\"x\",\"role\":\"OWNER\",\"active\":false}"), "edit user of other shop");
        refused(call("POST", "/api/users/" + owner1UserId + "/reset-password", owner2Tok,
                "{\"newPassword\":\"HackedPass1\"}"), "reset password of other shop's user");

        // the same username may exist in two shops
        expect(201, call("POST", "/api/users", owner2Tok,
                "{\"username\":\"owner1\",\"password\":\"Passw0rd!3\",\"fullName\":\"Same Name\",\"role\":\"STAFF\"}"),
                "same username in shop2");
        login("shop2", "owner1", "Passw0rd!3");
        login("main", "owner1", "Passw0rd!1");          // shop1's owner1 is unchanged

        // a shop owner can never create a platform admin
        refused(call("POST", "/api/users", owner2Tok,
                "{\"username\":\"evil\",\"password\":\"Passw0rd!4\",\"fullName\":\"Evil\",\"role\":\"SUPER_ADMIN\"}"),
                "owner creating SUPER_ADMIN");
    }

    @Test @Order(7)
    void loginDoesNotRevealWhatWasWrong() {
        Resp wrongShop = call("POST", "/api/auth/login", null,
                "{\"shopCode\":\"nope\",\"username\":\"owner1\",\"password\":\"Passw0rd!1\"}");
        Resp wrongPass = call("POST", "/api/auth/login", null,
                "{\"shopCode\":\"main\",\"username\":\"owner1\",\"password\":\"bad-password\"}");
        Resp wrongUser = call("POST", "/api/auth/login", null,
                "{\"shopCode\":\"main\",\"username\":\"nobody\",\"password\":\"Passw0rd!1\"}");
        Resp crossShop = call("POST", "/api/auth/login", null,   // owner2 exists only in shop2
                "{\"shopCode\":\"main\",\"username\":\"owner2\",\"password\":\"Passw0rd!2\"}");
        for (Resp r : new Resp[]{wrongShop, wrongPass, wrongUser, crossShop}) assertEquals(401, r.status(), r.body());
        assertEquals(wrongShop.body(), wrongPass.body());
        assertEquals(wrongShop.body(), wrongUser.body());
        // a shop user cannot use the platform login either
        assertEquals(401, call("POST", "/api/auth/login", null,
                "{\"shopCode\":\"\",\"username\":\"owner1\",\"password\":\"Passw0rd!1\"}").status());
    }

    // ------------------------------------------------------------------ platform admin boundaries

    @Test @Order(8)
    void superAdminSeesCountsOnly() {
        Resp shops = call("GET", "/api/admin/shops", superTok, null);
        expect(200, shops, "admin shop list");
        assertTrue(shops.body().contains("shop2") && shops.body().contains("main"));
        assertFalse(shops.body().contains("ISO-1"), "admin list shows product data");

        Resp products = call("GET", "/api/products?size=100", superTok, null);
        assertTrue(products.status() >= 400 || !products.body().contains("ISO-"),
                "super admin can read shop products: " + products.body());
        refused(call("GET", "/api/users", superTok, null), "super admin reading shop users");
        refused(call("GET", "/api/reports/dashboard", superTok, null), "super admin reading reports");

        refused(call("GET", "/api/admin/shops", owner1Tok, null), "shop owner calling admin API");
        refused(call("POST", "/api/admin/shops/" + shop2Id + "/suspend", owner1Tok, null), "shop owner suspending a shop");
        expect(401, call("GET", "/api/admin/shops", null, null), "admin API without login");
        expect(401, call("GET", "/api/products", null, null), "products without login");
        expect(200, call("GET", "/api/branding", null, null), "public branding");
    }

    @Test @Order(9)
    void forgedTokensAreRejected() {
        expect(401, call("GET", "/api/products", "abc.def.ghi", null), "garbage token");

        // signed with the real secret, but in the old format (no user id)
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String oldFormat = Jwts.builder().subject("owner1").claim("role", "OWNER").signWith(key).compact();
        expect(401, call("GET", "/api/products", oldFormat, null), "old-format token");

        // shop1's user id with shop2's id: token and database disagree
        String mixed = Jwts.builder().subject("owner1").claim("uid", owner1UserId).claim("sid", shop2Id)
                .claim("role", "OWNER").signWith(key).compact();
        expect(401, call("GET", "/api/products", mixed, null), "token naming the wrong shop");

        // a shop user claiming to be a platform admin (no shop) must not pass either
        String noShop = Jwts.builder().subject("owner1").claim("uid", owner1UserId)
                .claim("role", "SUPER_ADMIN").signWith(key).compact();
        expect(401, call("GET", "/api/admin/shops", noShop, null), "shop user token without shop");
    }

    // ------------------------------------------------------------------ modules and suspension

    @Test @Order(10)
    void modulesCanBeSwitchedOffPerShop() {
        expect(200, call("GET", "/api/quotations", owner2Tok, null), "quotations on");
        expect(200, call("PUT", "/api/admin/shops/" + shop2Id + "/features", superTok,
                "{\"features\":[\"PURCHASES\",\"CREDIT_NOTES\",\"EWAY\",\"REPORTS\"]}"), "switch quotations off");

        expect(403, call("GET", "/api/quotations", owner2Tok, null), "quotations off for shop2");
        expect(200, call("GET", "/api/quotations", owner1Tok, null), "quotations still on for shop1");
        Resp f = call("GET", "/api/features", owner2Tok, null);
        assertFalse(f.body().contains("QUOTATIONS"), f.body());
        expect(200, call("GET", "/api/reports/dashboard", owner2Tok, null), "dashboard stays");

        expect(200, call("PUT", "/api/admin/shops/" + shop2Id + "/features", superTok,
                "{\"features\":[\"PURCHASES\",\"QUOTATIONS\",\"CREDIT_NOTES\",\"EWAY\",\"REPORTS\"]}"), "switch back on");
        expect(200, call("GET", "/api/quotations", owner2Tok, null), "quotations on again");
    }

    @Test @Order(11)
    void suspendedShopIsLockedOutAtOnce() {
        expect(200, call("POST", "/api/admin/shops/" + shop2Id + "/suspend", superTok, null), "suspend");
        expect(401, call("GET", "/api/products", owner2Tok, null), "existing token after suspend");
        expect(403, call("POST", "/api/auth/login", null,
                "{\"shopCode\":\"shop2\",\"username\":\"owner2\",\"password\":\"Passw0rd!2\"}"), "login to suspended shop");
        expect(200, call("GET", "/api/products", owner1Tok, null), "other shop unaffected");

        expect(200, call("POST", "/api/admin/shops/" + shop2Id + "/activate", superTok, null), "activate");
        login("shop2", "owner2", "Passw0rd!2");
    }

    @Test @Order(12)
    void secondInvoiceNumbersStayPerShop() {
        Resp a = call("POST", "/api/invoices", owner1Tok, "{\"items\":[{\"productId\":" + p1 + ",\"quantity\":1}]}");
        expect(201, a, "shop1 second invoice");
        assertTrue(str(a.body(), "invoiceNo").endsWith("/00002"), str(a.body(), "invoiceNo"));
        Resp b = call("POST", "/api/invoices", owner2Tok, "{\"items\":[{\"productId\":" + p2 + ",\"quantity\":1}]}");
        expect(201, b, "shop2 second invoice");
        assertTrue(str(b.body(), "invoiceNo").endsWith("/00002"), str(b.body(), "invoiceNo"));
    }
}
