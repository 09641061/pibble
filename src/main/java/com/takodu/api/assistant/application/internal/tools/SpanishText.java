package com.takodu.api.assistant.application.internal.tools;

import com.takodu.api.shared.domain.model.valueobjects.Money;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** How the tools write dates, times and prices for the model: in Spanish and in the establishment's time zone. */
final class SpanishText {
    static final Locale SPANISH = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", SPANISH);

    private SpanishText() { }

    /** @return such as {@code sábado 24 de octubre} */
    static String date(LocalDate date) {
        return DATE.format(date);
    }

    /** @return such as {@code sábado 24 de octubre a las 10:00} */
    static String dateTime(Instant instant, ZoneId zone) {
        var local = instant.atZone(zone);
        return DATE.format(local) + " a las " + TIME.format(local);
    }

    static String time(Instant instant, ZoneId zone) {
        return TIME.format(instant.atZone(zone));
    }

    static String weekday(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, SPANISH);
    }

    /** @return such as {@code S/ 35.00}, or {@code USD 20.00} for another currency */
    static String price(Money money) {
        var amount = "%d.%02d".formatted(money.cents() / 100, money.cents() % 100);
        return ("PEN".equals(money.currency()) ? "S/ " : money.currency() + " ") + amount;
    }

    /** @return lower case without accents or extra spaces, so {@code "  Depilación "} matches {@code "depilacion"} */
    static String plain(String text) {
        var decomposed = Normalizer.normalize(text.strip(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
