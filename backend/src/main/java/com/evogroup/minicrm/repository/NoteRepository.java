package com.evogroup.minicrm.repository;

import com.evogroup.minicrm.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findAllByOrderByIdAsc();
    List<Note> findByClientIdOrderByIdAsc(Long clientId);
}
