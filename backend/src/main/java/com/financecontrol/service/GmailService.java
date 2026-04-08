package com.financecontrol.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService {

    private final Gmail gmail;
    private final EmailParserService emailParserService;
    private final TransactionService transactionService;

    private static final String USER = "me";

    // Solo emails de BCP Notificaciones con consumos de tarjeta
    private static final String BCP_QUERY =
            "from:notificaciones@notificacionesbcp.com.pe subject:\"consumo con tu Tarjeta\"";

    public void syncBcpEmails() {
        log.info("Sincronizando emails de BCP...");
        try {
            List<Message> messages = fetchBcpMessages();
            int imported = 0;

            for (Message msg : messages) {
                // Evitar duplicados ya importados
                if (transactionService.existsByGmailId(msg.getId())) {
                    continue;
                }

                Message fullMsg = gmail.users().messages()
                        .get(USER, msg.getId())
                        .setFormat("full")
                        .execute();

                String html = extractHtmlBody(fullMsg);
                if (html == null) continue;

                boolean saved = emailParserService.parseBcpEmail(html, msg.getId())
                        .map(tx -> { transactionService.save(tx); return true; })
                        .orElse(false);
                if (saved) {
                    imported++;
                } else {
                    String text = org.jsoup.Jsoup.parse(html).text();
                    log.warn("No se pudo parsear email id={} — texto: {}", msg.getId(),
                            text.length() > 500 ? text.substring(0, 500) : text);
                }
            }

            log.info("Sincronización completa: {} transacciones nuevas importadas", imported);
        } catch (Exception e) {
            log.error("Error al sincronizar Gmail: {}", e.getMessage());
        }
    }

    /**
     * syncNow ejecuta una sincronización manual bajo demanda.
     * @return 0 (simplificado; podría retornar el conteo exacto).
     */
    public int syncNow() {
        syncBcpEmails();
        return 0;
    }

    /**
     * fetchBcpMessages obtiene todos los mensajes de BCP desde Gmail paginando los resultados.
     * @return lista de mensajes encontrados con la query de BCP.
     */
    private List<Message> fetchBcpMessages() throws Exception {
        List<Message> result = new ArrayList<>();
        String pageToken = null;

        do {
            ListMessagesResponse response = gmail.users().messages()
                    .list(USER)
                    .setQ(BCP_QUERY)
                    .setMaxResults(50L)
                    .setPageToken(pageToken)
                    .execute();

            if (response.getMessages() != null) {
                result.addAll(response.getMessages());
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null);

        return result;
    }

    /**
     * extractHtmlBody extrae el cuerpo HTML de un mensaje de Gmail.
     * @param message mensaje completo de Gmail.
     * @return cadena HTML del cuerpo, o null si no se encuentra.
     */
    private String extractHtmlBody(Message message) {
        return extractPartHtml(message.getPayload());
    }

    /**
     * extractPartHtml recorre recursivamente las partes MIME buscando text/html.
     * @param part parte MIME del mensaje a inspeccionar.
     * @return contenido HTML decodificado, o null si la parte no es HTML.
     */
    private String extractPartHtml(MessagePart part) {
        if (part == null) return null;

        if ("text/html".equals(part.getMimeType()) && part.getBody() != null) {
            byte[] data = Base64.getUrlDecoder().decode(part.getBody().getData());
            return new String(data, StandardCharsets.UTF_8);
        }

        if (part.getParts() != null) {
            for (MessagePart sub : part.getParts()) {
                String result = extractPartHtml(sub);
                if (result != null) return result;
            }
        }

        return null;
    }
}
