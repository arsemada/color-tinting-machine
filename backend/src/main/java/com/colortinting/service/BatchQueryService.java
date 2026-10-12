package com.colortinting.service;

import com.colortinting.api.ApiException;
import com.colortinting.domain.ModelEnums.BatchState;
import com.colortinting.persistence.BatchEntity;
import com.colortinting.persistence.BatchRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BatchQueryService {
    private final BatchRepository batches;

    public BatchQueryService(BatchRepository batches) {
        this.batches = batches;
    }

    @Transactional(readOnly = true)
    public Page<BatchEntity> search(BatchState state, Instant from, Instant to, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Specification<BatchEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("startedAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<BatchEntity> result = batches.findAll(
                specification,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startedAt", "createdAt"))
        );
        result.getContent().forEach(batch -> {
            batch.getPumpResults().size();
            if (batch.getRecipe() != null) {
                batch.getRecipe().getId();
            }
        });
        return result;
    }

    @Transactional(readOnly = true)
    public BatchEntity get(UUID id) {
        BatchEntity batch = batches.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "BATCH_NOT_FOUND",
                "Batch " + id + " was not found."
        ));
        batch.getPumpResults().size();
        if (batch.getRecipe() != null) {
            batch.getRecipe().getId();
        }
        return batch;
    }
}
