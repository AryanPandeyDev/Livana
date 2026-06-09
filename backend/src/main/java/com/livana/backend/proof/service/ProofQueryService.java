package com.livana.backend.proof.service;

import com.livana.backend.proof.dto.NgoProofDto;
import com.livana.backend.proof.dto.PendingProofDto;
import com.livana.backend.proof.dto.ProofDto;
import com.livana.backend.proof.entity.Proof;
import com.livana.backend.proof.repository.ProofRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProofQueryService {

    private final ProofRepository proofRepository;

    /**
     * Paginated proofs for a specific pool.
     * Default sort: submittedAt descending (newest first).
     */
    public Page<ProofDto> proofsByPool(String poolAddress, Pageable pageable) {
        Pageable cappedPageable = capPageSize(pageable, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<Proof> page = proofRepository.findByPoolAddress(poolAddress, cappedPageable);
        return page.map(this::toProofDto);
    }

    /**
     * Paginated pending (unreleased) proofs across all pools.
     * Default sort: submittedAt ascending (oldest first, for review priority).
     */
    public Page<PendingProofDto> pendingProofs(Pageable pageable) {
        Pageable cappedPageable = capPageSize(pageable, Sort.by(Sort.Direction.ASC, "submittedAt"));
        Page<Proof> page = proofRepository.findByReleasedFalse(cappedPageable);
        return page.map(this::toPendingProofDto);
    }

    /**
     * Paginated proofs across every pool created by the given NGO wallet, with
     * full release status. Serves PRD User Story 27 ("NGO sees the status of
     * their proof submissions"). Default sort: submittedAt descending.
     */
    public Page<NgoProofDto> proofsByCreator(String creatorAddress, Pageable pageable) {
        Pageable cappedPageable = capPageSize(pageable, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<Proof> page = proofRepository.findByCreatorAddress(creatorAddress, cappedPageable);
        return page.map(this::toNgoProofDto);
    }

    private Pageable capPageSize(Pageable pageable, Sort defaultSort) {
        int cappedSize = Math.min(pageable.getPageSize(), 100);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        return PageRequest.of(pageable.getPageNumber(), cappedSize, sort);
    }

    private ProofDto toProofDto(Proof proof) {
        return new ProofDto(
                proof.getProofId(),
                proof.getIpfsCid(),
                proof.getAmount(),
                proof.getReleased(),
                proof.getSubmittedAt(),
                proof.getReleasedAt()
        );
    }

    private PendingProofDto toPendingProofDto(Proof proof) {
        return new PendingProofDto(
                proof.getPoolAddress(),
                proof.getProofId(),
                proof.getIpfsCid(),
                proof.getAmount(),
                proof.getSubmittedAt()
        );
    }

    private NgoProofDto toNgoProofDto(Proof proof) {
        return new NgoProofDto(
                proof.getPoolAddress(),
                proof.getProofId(),
                proof.getIpfsCid(),
                proof.getAmount(),
                proof.getReleased(),
                proof.getSubmittedAt(),
                proof.getReleasedAt()
        );
    }
}
