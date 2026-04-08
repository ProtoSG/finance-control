package com.financecontrol.service;

import com.financecontrol.model.Transaction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParserService {

    // Ejemplo: "S/ 14.00"
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("S/\\s*(\\d+(?:\\.\\d{2})?)");

    // Ejemplo: "06 de abril de 2026 - 01:01 PM"
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{2} de \\w+ de \\d{4} - \\d{2}:\\d{2} (?:AM|PM))");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy - hh:mm a", new Locale("es", "PE"));

    // Mapeo de meses en español a número (el Locale peruano puede no estar disponible en todos los JDK)
    private static final Map<String, String> MONTH_MAP = new HashMap<>();
    static {
        MONTH_MAP.put("enero", "01");
        MONTH_MAP.put("febrero", "02");
        MONTH_MAP.put("marzo", "03");
        MONTH_MAP.put("abril", "04");
        MONTH_MAP.put("mayo", "05");
        MONTH_MAP.put("junio", "06");
        MONTH_MAP.put("julio", "07");
        MONTH_MAP.put("agosto", "08");
        MONTH_MAP.put("septiembre", "09");
        MONTH_MAP.put("setiembre", "09");
        MONTH_MAP.put("octubre", "10");
        MONTH_MAP.put("noviembre", "11");
        MONTH_MAP.put("diciembre", "12");
    }

    /**
     * parseBcpEmail parsea el HTML del email de BCP y retorna una Transaction sin persistir.
     * @param htmlContent   contenido HTML del email.
     * @param gmailMessageId identificador del mensaje de Gmail para deduplicación.
     * @return Optional con la transacción extraída, o vacío si no se pudo parsear.
     */
    public Optional<Transaction> parseBcpEmail(String htmlContent, String gmailMessageId) {
        try {
            Document doc = Jsoup.parse(htmlContent);

            // Normalizar espacios: &nbsp; (\u00A0) y otros espacios unicode → espacio normal
            String text = doc.text()
                    .replace('\u00A0', ' ')
                    .replace('\u202F', ' ')
                    .replaceAll("\\s+", " ")
                    .trim();

            BigDecimal amount = extractAmount(text);
            LocalDateTime date = extractDate(text);
            String merchant = extractMerchant(text);
            String operationType = extractOperationType(text);
            String cardNumber = extractCardNumber(text);
            String operationNumber = extractOperationNumber(text);

            if (amount == null || date == null) {
                return Optional.empty();
            }

            Transaction transaction = Transaction.builder()
                    .amount(amount)
                    .merchant(merchant != null ? merchant : "Desconocido")
                    .operationType(operationType != null ? operationType : "Consumo")
                    .transactionDate(date)
                    .cardNumber(cardNumber)
                    .operationNumber(operationNumber)
                    .source(Transaction.TransactionSource.BCP_EMAIL)
                    .type(Transaction.TransactionType.EXPENSE)
                    .category(categorize(merchant))
                    .gmailMessageId(gmailMessageId)
                    .build();

            return Optional.of(transaction);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * extractAmount extrae el monto en soles del texto del email.
     * @param text texto plano del email normalizado.
     * @return monto como BigDecimal, o null si no se encontró.
     */
    private BigDecimal extractAmount(String text) {
        Matcher m = AMOUNT_PATTERN.matcher(text);
        if (m.find()) {
            return new BigDecimal(m.group(1));
        }
        return null;
    }

    /**
     * extractDate extrae y parsea la fecha/hora de la transacción del texto del email.
     * @param text texto plano del email normalizado.
     * @return fecha y hora de la transacción, o null si no se pudo parsear.
     */
    private LocalDateTime extractDate(String text) {
        Matcher m = DATE_PATTERN.matcher(text);
        if (m.find()) {
            String rawDate = m.group(1);

            // Reemplazar mes en español por número (comparar en minúsculas)
            String rawLower = rawDate.toLowerCase();
            for (Map.Entry<String, String> entry : MONTH_MAP.entrySet()) {
                String token = " de " + entry.getKey() + " de ";
                int idx = rawLower.indexOf(token);
                if (idx >= 0) {
                    rawDate = rawDate.substring(0, idx) + "/" + entry.getValue() + "/" + rawDate.substring(idx + token.length());
                    break;
                }
            }

            // Formato resultante: "06/04/2026 - 01:01 PM"
            rawDate = rawDate.replaceAll("\\s*-\\s*", " ");
            try {
                // parseCaseInsensitive para tolerar AM/PM en cualquier capitalización
                DateTimeFormatter fmt = new java.time.format.DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("dd/MM/yyyy hh:mm a")
                        .toFormatter(Locale.ENGLISH);
                return LocalDateTime.parse(rawDate, fmt);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * extractMerchant extrae el nombre del comercio del texto del email.
     * @param text texto plano del email normalizado.
     * @return nombre del comercio, o null si no se encontró.
     */
    private String extractMerchant(String text) {
        // Busca patrón: "Empresa GASTRONOMICA MAN HU"
        Pattern p = Pattern.compile("Empresa\\s+([A-Z0-9 &.,'-]+?)(?:\\s{2,}|Número|$)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * extractOperationType extrae el tipo de operación del texto del email.
     * @param text texto plano del email normalizado.
     * @return descripción del tipo de operación, o null si no se encontró.
     */
    private String extractOperationType(String text) {
        Pattern p = Pattern.compile("Operación realizada\\s+([\\w\\s]+?)(?:\\s{2,}|Fecha|$)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * extractCardNumber extrae el número enmascarado de la tarjeta del texto del email.
     * @param text texto plano del email normalizado.
     * @return número de tarjeta en formato ****XXXX, o null si no se encontró.
     */
    private String extractCardNumber(String text) {
        Pattern p = Pattern.compile("(\\*{4,}\\d{4})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * extractOperationNumber extrae el número de operación del texto del email.
     * @param text texto plano del email normalizado.
     * @return número de operación como cadena, o null si no se encontró.
     */
    private String extractOperationNumber(String text) {
        Pattern p = Pattern.compile("Número de operación\\s+(\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * categorize asigna una categoría automática basada en el nombre del comercio.
     * @param merchant nombre del comercio a categorizar.
     * @return categoría correspondiente; OTROS si no coincide ningún patrón.
     */
    private Transaction.Category categorize(String merchant) {
        if (merchant == null) return Transaction.Category.OTROS;
        String upper = merchant.toUpperCase();

        if (upper.matches(".*(?:RESTAURANT|GASTRO|COMIDA|PIZZA|BURGER|POLLO|SUSHI|CAFE|CAFETERIA|PANADERIA|MERCADO|SUPERMERCADO|WONG|METRO|PLAZA VEA|TAMBO|TOTTUS).*"))
            return Transaction.Category.ALIMENTACION;
        if (upper.matches(".*(?:TAXI|UBER|CABIFY|BEAT|BUS|METRO|COMBUSTIBLE|GRIFO|GASOLINERA|REPSOL|PRIMAX).*"))
            return Transaction.Category.TRANSPORTE;
        if (upper.matches(".*(?:NETFLIX|SPOTIFY|CINEMA|CINE|CINEPLANET|STEAM|AMAZON|DISNEY|HBO|YOUTUBE).*"))
            return Transaction.Category.ENTRETENIMIENTO;
        if (upper.matches(".*(?:FARMACIA|CLINICA|HOSPITAL|MEDICO|SALUD|INKAFARMA|MIFARMA).*"))
            return Transaction.Category.SALUD;
        if (upper.matches(".*(?:UNIVERSIDAD|COLEGIO|INSTITUTO|CURSO|UDEMY|PLATZI|EDUCACION).*"))
            return Transaction.Category.EDUCACION;
        if (upper.matches(".*(?:LUZ|AGUA|GAS|INTERNET|MOVISTAR|CLARO|ENTEL|BITEL).*"))
            return Transaction.Category.SERVICIOS;
        if (upper.matches(".*(?:RIPLEY|SAGA|FALABELLA|OECHSLE|SODIMAC|PROMART|TIENDA|SHOP|STORE).*"))
            return Transaction.Category.COMPRAS;
        if (upper.matches(".*(?:YAPE|TRANSFERENCIA|PLIN).*"))
            return Transaction.Category.TRANSFERENCIA;

        return Transaction.Category.OTROS;
    }
}
