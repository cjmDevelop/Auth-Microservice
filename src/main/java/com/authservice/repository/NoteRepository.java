package com.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.authservice.model.Note;
import com.authservice.model.User;
import java.util.List;


@Repository
public interface NoteRepository extends JpaRepository<Note, Long>{

        //Find all notes for a specific user
       List<Note> findByUserId(Long userId);

       //Find all notes for a specific user & order notes by latest entry
       List<Note> findByUserIdOrderByCreatedAtDesc(Long userId);

       // Find a specific note by id && verify it belongs to the current userId
       Optional<Note> findByIdAndUserId(Long id, Long userId);

    } 
