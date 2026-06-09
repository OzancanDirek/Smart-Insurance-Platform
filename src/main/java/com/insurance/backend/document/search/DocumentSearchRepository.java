package com.insurance.backend.document.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentSearchDocument, String>
{
    List<DocumentSearchDocument> findByOcrTextContaining(String text);

    List<DocumentSearchDocument> findByClaimId(Long claimId);
}