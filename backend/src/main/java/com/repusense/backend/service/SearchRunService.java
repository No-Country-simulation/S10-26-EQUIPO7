package com.repusense.backend.service;

import com.repusense.backend.model.SearchRun;
import com.repusense.backend.repository.SearchRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SearchRunService {

    private final SearchRunRepository searchRunRepository;

    public SearchRunService(SearchRunRepository searchRunRepository) {
        this.searchRunRepository = searchRunRepository;
    }

    @Transactional
    public void deleteSearchRun(Long id) {
        SearchRun searchRun = searchRunRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la búsqueda solicitada"
                ));

        searchRunRepository.unlinkBusinessesFromSearchRun(id);
        searchRunRepository.delete(searchRun);
    }

    @Transactional
    public void clearSearchHistory() {
        searchRunRepository.unlinkAllBusinessesFromSearchRuns();
        searchRunRepository.deleteAllInBatch();
    }
}
