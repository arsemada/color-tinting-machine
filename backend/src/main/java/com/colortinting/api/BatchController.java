package com.colortinting.api;

import com.colortinting.domain.ModelEnums.BatchState;
import com.colortinting.service.BatchQueryService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batches")
public class BatchController {
    private final BatchQueryService batches;
    private final ApiPresenter presenter;

    public BatchController(BatchQueryService batches, ApiPresenter presenter) {
        this.batches = batches;
        this.presenter = presenter;
    }

    @GetMapping
    public ApiPresenter.PageResponse<ApiPresenter.BatchResponse> list(
            @RequestParam(required = false) BatchState state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<com.colortinting.persistence.BatchEntity> result = batches.search(state, from, to, page, size);
        return new ApiPresenter.PageResponse<>(
                result.getContent().stream().map(presenter::batch).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public ApiPresenter.BatchResponse get(@PathVariable UUID id) {
        return presenter.batch(batches.get(id));
    }
}
