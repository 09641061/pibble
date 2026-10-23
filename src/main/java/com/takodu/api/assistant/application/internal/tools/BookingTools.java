package com.takodu.api.assistant.application.internal.tools;

import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalBookingService;
import com.takodu.api.assistant.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.assistant.domain.model.valueobjects.CustomerAppointment;
import com.takodu.api.assistant.domain.model.valueobjects.OfferedService;
import com.takodu.api.assistant.domain.services.TurnKeys;
import com.takodu.api.shared.domain.exceptions.DomainException;
import com.takodu.api.shared.domain.exceptions.PlanInactiveException;
import com.takodu.api.shared.domain.exceptions.ResourceNotFoundException;
import com.takodu.api.shared.domain.model.valueobjects.MemberId;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A5: what the assistant can do, as tools the model calls. Every tool reads the tenant, the customer and the time
 * zone from {@link TurnScope}, never from the model (A4), and writes through booking with a key derived from the
 * turn, so a retried turn changes nothing twice. Results are short Spanish text for the model. A business refusal
 * comes back as text the model can recover from; only infrastructure errors escape and fail the turn.
 */
@ApplicationScoped
public class BookingTools {
    /** B16: the model offers at most this many times at once. */
    static final int MAX_TIMES = 6;

    private final TurnScope scope;
    private final ExternalOrganizationService organization;
    private final ExternalBookingService booking;

    public BookingTools(TurnScope scope, ExternalOrganizationService organization, ExternalBookingService booking) {
        this.scope = scope;
        this.organization = organization;
        this.booking = booking;
    }

    @Tool("Lista los servicios del local con su duración, precio e indicaciones.")
    public String listServices() {
        if (!scope.countToolCall()) return tooManyCalls();
        var services = services();
        if (services.isEmpty()) return "El local no tiene servicios disponibles para agendar.";
        return services.stream().map(service -> "- %s: %d min, %s%s".formatted(service.name(),
                        service.duration().toMinutes(), SpanishText.price(service.price()),
                        service.instructions() == null ? "" : ". Indicaciones: " + service.instructions()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Da la dirección, el horario de atención y la información que el dueño dejó sobre el negocio.")
    public String businessInfo() {
        if (!scope.countToolCall()) return tooManyCalls();
        var establishment = scope.establishment();
        var hours = Arrays.stream(DayOfWeek.values()).map(day -> {
            var ranges = establishment.openingHours().stream().filter(range -> range.day() == day)
                    .map(range -> range.opensAt() + "-" + range.closesAt()).toList();
            return SpanishText.weekday(day) + ": " + (ranges.isEmpty() ? "cerrado" : String.join(", ", ranges));
        }).collect(Collectors.joining("\n"));
        return "Local: " + establishment.name()
                + (establishment.address() == null ? "" : "\nDirección: " + establishment.address())
                + "\nHorario:\n" + hours
                + (scope.businessInfo().isEmpty() ? "" : "\nInformación del negocio:\n" + scope.businessInfo());
    }

    @Tool("Busca horarios libres para un servicio en una fecha. La fecha va en formato YYYY-MM-DD.")
    public String findSlots(@P("Nombre del servicio") String serviceName,
                            @P("Fecha en formato YYYY-MM-DD") String date,
                            @P(value = "Nombre del profesional, si el cliente lo pidió", required = false)
                            String staffName) {
        if (!scope.countToolCall()) return tooManyCalls();
        var service = service(serviceName);
        if (service.isEmpty()) return noSuchService();
        LocalDate day;
        try {
            day = LocalDate.parse(date.strip());
        } catch (DateTimeParseException e) {
            return "La fecha debe ir en formato YYYY-MM-DD.";
        }
        var member = member(service.get(), staffName);
        if (member.isEmpty()) return noSuchMember(service.get());
        try {
            return freeTimes(service.get(), day, member.get().orElse(null));
        } catch (DomainException e) {
            return refusal(e);
        }
    }

    @Tool("Agenda una cita para este cliente. Confirma antes servicio, fecha y hora con el cliente.")
    public String bookAppointment(@P("Nombre del servicio") String serviceName,
                                  @P("Inicio en hora local, formato YYYY-MM-DDTHH:MM") String start,
                                  @P(value = "Nombre del profesional, si el cliente lo pidió", required = false)
                                  String staffName) {
        if (!scope.countToolCall()) return tooManyCalls();
        var service = service(serviceName);
        if (service.isEmpty()) return noSuchService();
        var at = instant(start);
        if (at.isEmpty()) return "La hora debe ir en formato YYYY-MM-DDTHH:MM.";
        var member = member(service.get(), staffName);
        if (member.isEmpty()) return noSuchMember(service.get());
        try {
            var appointment = booking.book(scope.organizationId(), scope.customerId(), service.get().id(),
                    member.get().orElse(null), at.get(), TurnKeys.of(scope.turnId(), "bookAppointment",
                            service.get().id().value(), at.get(), member.get().map(MemberId::value).orElse(null)));
            return "Cita agendada: " + describe(appointment) + "."
                    + (service.get().instructions() == null ? "" : " Indicaciones: " + service.get().instructions());
        } catch (DomainException e) {
            return refusal(e) + alternatives(service.get(), at.get(), member.get().orElse(null));
        }
    }

    @Tool("Lista las próximas citas de este cliente en el local, numeradas, e indica si aún puede cambiarlas.")
    public String myAppointments() {
        if (!scope.countToolCall()) return tooManyCalls();
        var appointments = upcoming();
        if (appointments.isEmpty()) return "El cliente no tiene citas próximas en este local.";
        var lines = new StringBuilder();
        for (int i = 0; i < appointments.size(); i++) {
            var appointment = appointments.get(i);
            lines.append(i + 1).append(". ").append(describe(appointment))
                    .append(appointment.mayChange() ? "" : " (ya no se puede cambiar por aquí)").append('\n');
        }
        return lines.toString().strip();
    }

    @Tool("Reprograma una cita del cliente. Usa el número que dio myAppointments.")
    public String rescheduleAppointment(@P("Número de la cita en la lista de myAppointments") int appointmentNumber,
                                        @P("Nuevo inicio en hora local, formato YYYY-MM-DDTHH:MM") String newStart) {
        if (!scope.countToolCall()) return tooManyCalls();
        var appointment = numbered(appointmentNumber);
        if (appointment.isEmpty()) return noSuchAppointment();
        var at = instant(newStart);
        if (at.isEmpty()) return "La hora debe ir en formato YYYY-MM-DDTHH:MM.";
        try {
            var moved = booking.reschedule(scope.organizationId(), scope.customerId(), appointment.get().id(),
                    at.get(), TurnKeys.of(scope.turnId(), "rescheduleAppointment", appointment.get().id().value(),
                            at.get()));
            return "Cita reprogramada: " + describe(moved) + ".";
        } catch (DomainException e) {
            return refusal(e);
        }
    }

    @Tool("Cancela una cita del cliente. Usa el número que dio myAppointments.")
    public String cancelAppointment(@P("Número de la cita en la lista de myAppointments") int appointmentNumber,
                                    @P("Motivo que dio el cliente") String reason) {
        if (!scope.countToolCall()) return tooManyCalls();
        var appointment = numbered(appointmentNumber);
        if (appointment.isEmpty()) return noSuchAppointment();
        var why = reason == null || reason.isBlank() ? "Cancelada por el cliente" : reason.strip();
        try {
            var cancelled = booking.cancel(scope.organizationId(), scope.customerId(), appointment.get().id(), why,
                    TurnKeys.of(scope.turnId(), "cancelAppointment", appointment.get().id().value()));
            return "Cita cancelada: " + describe(cancelled) + ".";
        } catch (DomainException e) {
            return refusal(e);
        }
    }

    @Tool("Pasa la conversación a una persona del equipo: si el cliente lo pide, reclama, o no logras resolver.")
    public String handOffToHuman(@P("Resumen breve de lo que necesita el cliente") String summary) {
        if (!scope.countToolCall()) return tooManyCalls();
        scope.requestHandOff(summary == null || summary.isBlank() ? "El cliente necesita a una persona" : summary);
        return "Listo. Dile al cliente que una persona del equipo continuará la conversación.";
    }

    private List<OfferedService> services() {
        return organization.fetchActiveServices(scope.organizationId(), scope.establishmentId());
    }

    /** A4: only the bound establishment's services; an exact name first, else the only one that contains it. */
    private Optional<OfferedService> service(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        var wanted = SpanishText.plain(name);
        var services = services();
        var exact = services.stream().filter(service -> SpanishText.plain(service.name()).equals(wanted)).findFirst();
        if (exact.isPresent()) return exact;
        var partial = services.stream().filter(service -> SpanishText.plain(service.name()).contains(wanted)).toList();
        return partial.size() == 1 ? Optional.of(partial.getFirst()) : Optional.empty();
    }

    /** @return empty when the name matches nobody who performs the service; a present empty for no preference */
    private Optional<Optional<MemberId>> member(OfferedService service, String staffName) {
        if (staffName == null || staffName.isBlank()) return Optional.of(Optional.empty());
        var wanted = SpanishText.plain(staffName);
        return service.performers().stream()
                .filter(member -> organization.fetchMemberName(scope.organizationId(), member, scope.establishmentId())
                        .map(SpanishText::plain).filter(name -> name.contains(wanted)).isPresent())
                .findFirst()
                .map(Optional::of);
    }

    private String freeTimes(OfferedService service, LocalDate day, MemberId member) {
        var times = booking.fetchFreeTimes(scope.organizationId(), service.id(), scope.customerId(), day, member);
        if (times.isEmpty()) return "No hay horarios libres el " + SpanishText.date(day) + ".";
        return "Horarios libres el " + SpanishText.date(day) + ": " + times.stream().limit(MAX_TIMES)
                .map(time -> SpanishText.time(time, scope.zone())).collect(Collectors.joining(", ")) + ".";
    }

    /** The same day's free times, so the model can offer another one at once. */
    private String alternatives(OfferedService service, Instant asked, MemberId member) {
        try {
            return " " + freeTimes(service, asked.atZone(scope.zone()).toLocalDate(), member);
        } catch (DomainException e) {
            return "";
        }
    }

    private List<CustomerAppointment> upcoming() {
        var appointments = booking.fetchUpcoming(scope.organizationId(), scope.establishmentId(), scope.customerId())
                .stream().sorted(Comparator.comparing(CustomerAppointment::start)).toList();
        scope.rememberAppointments(appointments);
        return appointments;
    }

    private Optional<CustomerAppointment> numbered(int number) {
        var appointments = scope.listedAppointments().orElseGet(this::upcoming);
        return number >= 1 && number <= appointments.size() ? Optional.of(appointments.get(number - 1))
                : Optional.empty();
    }

    private Optional<Instant> instant(String localDateTime) {
        try {
            return Optional.of(LocalDateTime.parse(localDateTime.strip()).atZone(scope.zone()).toInstant());
        } catch (DateTimeParseException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private String describe(CustomerAppointment appointment) {
        var service = services().stream().filter(offered -> offered.id().equals(appointment.serviceId()))
                .map(OfferedService::name).findFirst().orElse("servicio");
        var staff = organization.fetchMemberName(scope.organizationId(), appointment.staffMemberId(),
                scope.establishmentId());
        return service + staff.map(name -> " con " + name).orElse("") + ", "
                + SpanishText.dateTime(appointment.start(), scope.zone());
    }

    /** Booking's refusals as text the model can act on; B14 hands the chat to a person (A7). */
    private String refusal(DomainException e) {
        return switch (e.code()) {
            case "SLOT_UNAVAILABLE", "TIME_BLOCKED", "OUTSIDE_OPENING_HOURS" -> "Ese horario no está disponible.";
            case "OUTSIDE_BOOKING_WINDOW" ->
                    "Solo puedo agendar con al menos 30 minutos de anticipación y hasta 60 días antes.";
            case "CUSTOMER_CHANGE_TOO_LATE" -> {
                scope.requestHandOff("El cliente quiere cambiar una cita que empieza en menos de 2 horas");
                yield "Esa cita empieza en menos de 2 horas, así que no puedo cambiarla. Dile al cliente que una"
                        + " persona del equipo lo ayudará.";
            }
            case "MEMBER_CANNOT_PERFORM_SERVICE", "MEMBER_NOT_AT_ESTABLISHMENT" ->
                    "Esa persona no realiza ese servicio en este local.";
            case "SERVICE_NOT_BOOKABLE" -> "Ese servicio no se puede agendar ahora.";
            case "INVALID_TRANSITION" -> "Esa cita ya no se puede cambiar.";
            default -> e instanceof ResourceNotFoundException ? noSuchAppointment()
                    : e instanceof PlanInactiveException ? "Ahora no puedo agendar; una persona del equipo te ayudará."
                    : "No pude hacerlo.";
        };
    }

    private String noSuchService() {
        return "No existe ese servicio. Servicios: "
                + services().stream().map(OfferedService::name).collect(Collectors.joining(", ")) + ".";
    }

    private String noSuchMember(OfferedService service) {
        var names = service.performers().stream()
                .map(member -> organization.fetchMemberName(scope.organizationId(), member, scope.establishmentId()))
                .flatMap(Optional::stream).collect(Collectors.joining(", "));
        return "Nadie con ese nombre realiza " + service.name() + ". Lo realizan: " + names + ".";
    }

    private static String noSuchAppointment() {
        return "No encontré esa cita. Usa myAppointments para ver las citas del cliente.";
    }

    private String tooManyCalls() {
        scope.requestHandOff("El asistente no logró resolver la consulta");
        return "Ya no puedo usar más herramientas. Dile al cliente que una persona del equipo continuará.";
    }
}
