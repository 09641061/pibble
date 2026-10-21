package com.takodu.api.conversations.application.internal.outboundservices.whatsapp;

import com.takodu.api.conversations.domain.exceptions.WhatsappSignupFailedException;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.valueobjects.EncryptedToken;
import com.takodu.api.conversations.domain.model.valueobjects.LineMode;
import com.takodu.api.shared.infrastructure.crypto.TokenCipher;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** The {@link WhatsappOnboarding} of every {@code @QuarkusTest}: accepts any signup and never calls Meta. */
@Singleton
public class RecordingWhatsappOnboarding implements WhatsappOnboarding {
    public static final String DISPLAY_PHONE = "+51 1 555 0100";

    private final TokenCipher tokenCipher;
    private final List<String> onboardedPhoneNumberIds = new CopyOnWriteArrayList<>();
    private final List<String> unsubscribedWabaIds = new CopyOnWriteArrayList<>();
    private final AtomicBoolean failNext = new AtomicBoolean();

    public RecordingWhatsappOnboarding(TokenCipher tokenCipher) {
        this.tokenCipher = tokenCipher;
    }

    @Override
    public OnboardedNumber onboard(String code, String wabaId, String phoneNumberId, LineMode mode, String pin) {
        if (failNext.getAndSet(false)) throw new WhatsappSignupFailedException("Meta refused exchanging the signup code");
        onboardedPhoneNumberIds.add(phoneNumberId);
        return new OnboardedNumber(DISPLAY_PHONE, new EncryptedToken(tokenCipher.encrypt("token-" + phoneNumberId)));
    }

    @Override
    public void unsubscribe(WhatsappLine line) {
        unsubscribedWabaIds.add(line.getWabaId());
    }

    /** Makes the next {@link #onboard} fail as Meta does with a used code. */
    public void failNextSignup() {
        failNext.set(true);
    }

    /** @return how many times the number went through signup */
    public long signupsOf(String phoneNumberId) {
        return onboardedPhoneNumberIds.stream().filter(phoneNumberId::equals).count();
    }

    public boolean unsubscribed(String wabaId) {
        return unsubscribedWabaIds.contains(wabaId);
    }
}
