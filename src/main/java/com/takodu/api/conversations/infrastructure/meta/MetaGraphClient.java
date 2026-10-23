package com.takodu.api.conversations.infrastructure.meta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

/**
 * The Graph API calls Takodu makes for WhatsApp, checked against Meta's docs on 2026-10-21. The version is
 * {@code takodu.meta.graph-version}; every call except the code exchange carries the line's business token.
 */
@RegisterRestClient(configKey = "meta-graph")
public interface MetaGraphClient {

    /** Embedded Signup: trades the single-use code for the business token, server to server. */
    @GET
    @Path("/{version}/oauth/access_token")
    AccessTokenResponse exchangeCode(@PathParam("version") String version, @QueryParam("client_id") String appId,
                                     @QueryParam("client_secret") String appSecret, @QueryParam("code") String code);

    @POST
    @Path("/{version}/{wabaId}/subscribed_apps")
    void subscribeApp(@PathParam("version") String version, @PathParam("wabaId") String wabaId,
                      @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{version}/{wabaId}/subscribed_apps")
    void unsubscribeApp(@PathParam("version") String version, @PathParam("wabaId") String wabaId,
                        @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{version}/{phoneNumberId}")
    PhoneNumberResponse fetchPhoneNumber(@PathParam("version") String version,
                                         @PathParam("phoneNumberId") String phoneNumberId,
                                         @QueryParam("fields") String fields,
                                         @HeaderParam("Authorization") String authorization);

    /** Coexistence: {@code sync_type} {@code history} asks for the chat history; allowed once, within 24 hours. */
    @POST
    @Path("/{version}/{phoneNumberId}/smb_app_data")
    void requestAppData(@PathParam("version") String version, @PathParam("phoneNumberId") String phoneNumberId,
                        @HeaderParam("Authorization") String authorization, AppDataRequest request);

    /** Cloud API mode only: registers the number with its two-step verification PIN. */
    @POST
    @Path("/{version}/{phoneNumberId}/register")
    void register(@PathParam("version") String version, @PathParam("phoneNumberId") String phoneNumberId,
                  @HeaderParam("Authorization") String authorization, RegisterRequest request);

    /** @param name Meta matches names that contain it, so callers compare the exact name and language */
    @GET
    @Path("/{version}/{wabaId}/message_templates")
    TemplateList fetchTemplates(@PathParam("version") String version, @PathParam("wabaId") String wabaId,
                                @QueryParam("name") String name, @QueryParam("fields") String fields,
                                @HeaderParam("Authorization") String authorization);

    /** @param template {@code name}, {@code language}, {@code category} and {@code components}; Meta reviews it */
    @POST
    @Path("/{version}/{wabaId}/message_templates")
    TemplateResponse createTemplate(@PathParam("version") String version, @PathParam("wabaId") String wabaId,
                                    @HeaderParam("Authorization") String authorization, Map<String, Object> template);

    /** @param payload a Cloud API message, such as a text with {@code to}, {@code type} and {@code text.body} */
    @POST
    @Path("/{version}/{phoneNumberId}/messages")
    JsonNode sendMessage(@PathParam("version") String version, @PathParam("phoneNumberId") String phoneNumberId,
                         @HeaderParam("Authorization") String authorization, Map<String, Object> payload);

    /** @param phoneNumberId Meta answers only when the media reached this number */
    @GET
    @Path("/{version}/{mediaId}")
    MediaResponse fetchMedia(@PathParam("version") String version, @PathParam("mediaId") String mediaId,
                             @QueryParam("phone_number_id") String phoneNumberId,
                             @HeaderParam("Authorization") String authorization);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccessTokenResponse(@JsonProperty("access_token") String accessToken) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhoneNumberResponse(@JsonProperty("display_phone_number") String displayPhoneNumber) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TemplateList(List<TemplateResponse> data) { }

    /** The creation response carries only {@code id}, {@code status} and {@code category}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record TemplateResponse(String id, String name, String language, String status) { }

    /** @param url valid for 5 minutes, fetched with the same token */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MediaResponse(String url, @JsonProperty("mime_type") String mimeType,
                         @JsonProperty("file_size") Long fileSize) { }

    record AppDataRequest(@JsonProperty("messaging_product") String messagingProduct,
                          @JsonProperty("sync_type") String syncType) { }

    record RegisterRequest(@JsonProperty("messaging_product") String messagingProduct, String pin) { }
}
