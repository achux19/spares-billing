package com.sparesbilling.backend.admin;

import com.sparesbilling.backend.admin.AdminDtos.*;
import com.sparesbilling.backend.common.NotFoundException;
import com.sparesbilling.backend.common.StateCodes;
import com.sparesbilling.backend.feature.Feature;
import com.sparesbilling.backend.feature.FeatureService;
import com.sparesbilling.backend.tenant.Shop;
import com.sparesbilling.backend.tenant.ShopRepository;
import com.sparesbilling.backend.tenant.ShopStatus;
import com.sparesbilling.backend.user.Role;
import com.sparesbilling.backend.user.User;
import com.sparesbilling.backend.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Platform-level work: shops and their owners. It reads shop data only as counts and never opens a
 * shop's bills, customers or products.
 */
@Service
@RequiredArgsConstructor
public class AdminShopService {

    private static final Set<String> RESERVED_CODES = Set.of(
            "admin", "platform", "api", "www", "app", "login", "support", "root", "staging", "billing");

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final FeatureService featureService;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager em;

    // ---------- read ----------

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<ShopRow> list() {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT s.id, s.code, s.name, s.status, s.created_at,
                       (SELECT COUNT(*) FROM users u WHERE u.shop_id = s.id),
                       (SELECT COUNT(*) FROM users u WHERE u.shop_id = s.id AND u.active),
                       (SELECT COUNT(*) FROM invoices i WHERE i.shop_id = s.id AND i.status = 'ACTIVE'),
                       (SELECT MAX(i.invoice_date) FROM invoices i WHERE i.shop_id = s.id),
                       (SELECT MAX(u.last_login_at) FROM users u WHERE u.shop_id = s.id)
                FROM shops s ORDER BY s.id
                """).getResultList();
        return rows.stream().map(r -> {
            Long id = ((Number) r[0]).longValue();
            List<String> features = featureService.enabledFor(id).stream().map(Enum::name).sorted().toList();
            return new ShopRow(id, (String) r[1], (String) r[2], (String) r[3], toDateTime(r[4]), features,
                    ((Number) r[5]).longValue(), ((Number) r[6]).longValue(), ((Number) r[7]).longValue(),
                    toDate(r[8]), toDateTime(r[9]));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ShopUserRow> users(Long shopId) {
        findShop(shopId);
        return userRepository.findAllByShopId(shopId, Sort.by("username")).stream()
                .map(u -> new ShopUserRow(u.getId(), u.getUsername(), u.getFullName(), u.getRole().name(),
                        u.isActive(), u.getLastLoginAt()))
                .toList();
    }

    public List<FeatureInfo> featureCatalog() {
        return java.util.Arrays.stream(Feature.values()).map(f -> new FeatureInfo(f.name(), f.label())).toList();
    }

    // ---------- write ----------

    @Transactional
    public ShopRow create(CreateShopRequest req) {
        String code = req.code().trim().toLowerCase();
        checkCode(code, null);
        if (!StateCodes.isValid(req.stateCode())) {
            throw new IllegalArgumentException("Unknown state code " + req.stateCode());
        }
        Set<Feature> features = parseFeatures(req.features());

        Shop shop = new Shop();
        shop.setCode(code);
        shop.setName(req.name().trim());
        shop = shopRepository.saveAndFlush(shop);
        int id = Math.toIntExact(shop.getId());

        // the shop's own settings and branding rows (their row id equals the shop id)
        em.createNativeQuery("INSERT INTO shop_settings (id, shop_id, shop_name, state_code, invoice_prefix) "
                        + "VALUES (:id, :shopId, :name, :state, 'INV')")
                .setParameter("id", id).setParameter("shopId", shop.getId())
                .setParameter("name", shop.getName()).setParameter("state", req.stateCode()).executeUpdate();
        String appName = shop.getName().length() > 60 ? shop.getName().substring(0, 60) : shop.getName();
        em.createNativeQuery("INSERT INTO branding (id, shop_id, app_name) VALUES (:id, :shopId, :app)")
                .setParameter("id", id).setParameter("shopId", shop.getId()).setParameter("app", appName)
                .executeUpdate();

        featureService.setFeatures(shop.getId(), features);

        User owner = new User();
        owner.setShopId(shop.getId());
        owner.setUsername(req.ownerUsername().trim());
        owner.setFullName(req.ownerFullName().trim());
        owner.setPasswordHash(passwordEncoder.encode(req.ownerPassword()));
        owner.setRole(Role.OWNER);
        owner.setMustChangePassword(true);
        userRepository.save(owner);

        return row(shop.getId());
    }

    @Transactional
    public ShopRow update(Long id, UpdateShopRequest req) {
        Shop shop = findShop(id);
        String code = req.code().trim().toLowerCase();
        if (!code.equals(shop.getCode())) checkCode(code, id);
        shop.setCode(code);
        shop.setName(req.name().trim());
        return row(id);
    }

    @Transactional
    public ShopRow setStatus(Long id, ShopStatus status) {
        findShop(id).setStatus(status);
        return row(id);
    }

    @Transactional
    public ShopRow setFeatures(Long id, List<String> keys) {
        findShop(id);
        featureService.setFeatures(id, parseFeatures(keys));
        return row(id);
    }

    @Transactional
    public void resetPassword(Long shopId, Long userId, String newPassword) {
        findShop(shopId);
        User u = userRepository.findByIdAndShopId(userId, shopId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        u.setMustChangePassword(true);
    }

    // ---------- helpers ----------

    private ShopRow row(Long id) {
        return list().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    private Shop findShop(Long id) {
        return shopRepository.findById(id).orElseThrow(() -> new NotFoundException("Shop not found"));
    }

    private void checkCode(String code, Long selfId) {
        if (RESERVED_CODES.contains(code)) throw new IllegalArgumentException("'" + code + "' is a reserved shop code");
        shopRepository.findByCode(code).ifPresent(other -> {
            if (!other.getId().equals(selfId)) throw new IllegalArgumentException("Shop code '" + code + "' is already used");
        });
    }

    private static Set<Feature> parseFeatures(List<String> keys) {
        Set<Feature> set = EnumSet.noneOf(Feature.class);
        for (String k : keys) {
            try {
                set.add(Feature.valueOf(k));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown feature: " + k);
            }
        }
        return set;
    }

    private static LocalDateTime toDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime t) return t;
        if (o instanceof java.sql.Timestamp t) return t.toLocalDateTime();
        if (o instanceof java.time.OffsetDateTime t) return t.toLocalDateTime();
        if (o instanceof java.time.Instant t) return LocalDateTime.ofInstant(t, java.time.ZoneId.of("Asia/Kolkata"));
        throw new IllegalStateException("Unexpected date-time type " + o.getClass());
    }

    private static LocalDate toDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        throw new IllegalStateException("Unexpected date type " + o.getClass());
    }
}
