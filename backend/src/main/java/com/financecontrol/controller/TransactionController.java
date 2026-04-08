package com.financecontrol.controller;

import com.financecontrol.dto.SummaryResponse;
import com.financecontrol.dto.TransactionRequest;
import com.financecontrol.model.Transaction;
import com.financecontrol.service.GmailService;
import com.financecontrol.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

// En prod: restringir al dominio de la app
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;
    private final GmailService gmailService;

    @Autowired
    public TransactionController(TransactionService transactionService, @Lazy GmailService gmailService) {
        this.transactionService = transactionService;
        this.gmailService = gmailService;
    }

    /**
     * getAll obtiene todas las transacciones, con filtro opcional por rango de fechas.
     * @param from fecha/hora de inicio del rango (opcional).
     * @param to   fecha/hora de fin del rango (opcional).
     * @return lista de transacciones ordenadas por fecha descendente.
     */
    @GetMapping("/transactions")
    public List<Transaction> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from != null && to != null) {
            return transactionService.findByPeriod(from, to);
        }
        return transactionService.findAll();
    }

    /**
     * create registra una nueva transacción manual.
     * @param req datos de la transacción validados.
     * @return la transacción creada con su ID asignado.
     */
    @PostMapping("/transactions")
    public ResponseEntity<Transaction> create(@Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(transactionService.createManual(req));
    }

    /**
     * update modifica una transacción existente por su ID.
     * @param id  identificador de la transacción a actualizar.
     * @param req nuevos datos de la transacción.
     * @return la transacción actualizada.
     */
    @PutMapping("/transactions/{id}")
    public ResponseEntity<Transaction> update(@PathVariable Long id, @Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.ok(transactionService.update(id, req));
    }

    /**
     * delete elimina una transacción por su ID.
     * @param id identificador de la transacción a eliminar.
     * @return respuesta vacía con código 204.
     */
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * getSummary retorna el resumen de gastos e ingresos agrupado por categoría para un mes.
     * @param month mes en formato YYYY-MM; si está vacío usa el mes actual.
     * @return resumen mensual con totales por categoría.
     */
    @GetMapping("/summary")
    public SummaryResponse getSummary(@RequestParam(defaultValue = "") String month) {
        YearMonth ym = month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        return transactionService.getSummary(ym);
    }

    /**
     * syncNow fuerza una sincronización inmediata con Gmail.
     * @return mapa con estado y mensaje de confirmación.
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncNow() {
        gmailService.syncNow();
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Sincronización iniciada"));
    }
}
