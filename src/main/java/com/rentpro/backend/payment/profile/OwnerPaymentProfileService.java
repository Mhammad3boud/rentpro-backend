package com.rentpro.backend.payment.profile;

import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OwnerPaymentProfileService {

    private final OwnerPaymentProfileRepository profileRepository;
    private final UserRepository userRepository;

    public OwnerPaymentProfileService(OwnerPaymentProfileRepository profileRepository,
                                      UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public OwnerPaymentProfileResponse getOrEmpty(UUID ownerUserId) {
        return profileRepository.findByUser_UserId(ownerUserId)
                .map(this::toResponse)
                .orElse(emptyResponse());
    }

    public OwnerPaymentProfileResponse upsert(UUID ownerUserId, OwnerPaymentProfileRequest req) {
        OwnerPaymentProfile profile = profileRepository.findByUser_UserId(ownerUserId)
                .orElseGet(() -> {
                    User user = userRepository.findById(ownerUserId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    OwnerPaymentProfile p = new OwnerPaymentProfile();
                    p.setUser(user);
                    return p;
                });

        if (req.country() != null) profile.setCountry(req.country());
        if (req.acceptedMethods() != null) profile.setAcceptedMethods(String.join(",", req.acceptedMethods()));
        profile.setBankName(req.bankName());
        profile.setBankAccountNumber(req.bankAccountNumber());
        profile.setBankAccountName(req.bankAccountName());
        profile.setBankSwiftCode(req.bankSwiftCode());
        profile.setDuitnowId(req.duitnowId());
        profile.setTouchngoPhone(req.touchngoPhone());
        profile.setGrabpayPhone(req.grabpayPhone());
        profile.setMpesaPhone(req.mpesaPhone());
        profile.setAirtelMoneyPhone(req.airtelMoneyPhone());
        profile.setTigoPesaPhone(req.tigoPesaPhone());

        return toResponse(profileRepository.save(profile));
    }

    /** Used by the gateway service to resolve the owner's country for a given owner user ID. */
    public String getCountry(UUID ownerUserId) {
        return profileRepository.findByUser_UserId(ownerUserId)
                .map(OwnerPaymentProfile::getCountry)
                .orElse("MY");
    }

    public String getStripeAccountId(UUID ownerUserId) {
        return profileRepository.findByUser_UserId(ownerUserId)
                .map(OwnerPaymentProfile::getStripeAccountId)
                .orElse(null);
    }

    public boolean isStripeOnboarded(UUID ownerUserId) {
        return profileRepository.findByUser_UserId(ownerUserId)
                .map(OwnerPaymentProfile::isStripeOnboarded)
                .orElse(false);
    }

    public void saveStripeConnect(UUID ownerUserId, String stripeAccountId, boolean onboarded) {
        OwnerPaymentProfile profile = profileRepository.findByUser_UserId(ownerUserId)
                .orElseGet(() -> {
                    User user = userRepository.findById(ownerUserId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    OwnerPaymentProfile p = new OwnerPaymentProfile();
                    p.setUser(user);
                    return p;
                });
        profile.setStripeAccountId(stripeAccountId);
        profile.setStripeOnboarded(onboarded);
        profileRepository.save(profile);
    }

    private OwnerPaymentProfileResponse toResponse(OwnerPaymentProfile p) {
        return new OwnerPaymentProfileResponse(
                p.getCountry(),
                parseMethods(p.getAcceptedMethods()),
                p.getBankName(),
                p.getBankAccountNumber(),
                p.getBankAccountName(),
                p.getBankSwiftCode(),
                p.getDuitnowId(),
                p.getTouchngoPhone(),
                p.getGrabpayPhone(),
                p.getMpesaPhone(),
                p.getAirtelMoneyPhone(),
                p.getTigoPesaPhone(),
                p.getStripeAccountId(),
                p.isStripeOnboarded()
        );
    }

    private List<String> parseMethods(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private OwnerPaymentProfileResponse emptyResponse() {
        return new OwnerPaymentProfileResponse("MY", List.of(), null, null, null, null, null, null, null, null, null, null, null, false);
    }
}
