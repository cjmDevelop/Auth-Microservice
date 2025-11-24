package com.authservice.controller;

import org.springframework.web.bind.annotation.RestController;

import com.authservice.dto.notes.CreateNoteRequestDto;
import com.authservice.dto.notes.NoteResponseDto;
import com.authservice.dto.notes.UpdateNoteRequestDto;
import com.authservice.model.Note;
import com.authservice.service.NoteService;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    
    @GetMapping
    public List<NoteResponseDto> getNotes() {
        List<Note> notes = noteService.getUserNotes();

        //Converting notes into NoteResponseDto's
        List<NoteResponseDto> responses = new ArrayList<>();
        for(Note note : notes) {
           NoteResponseDto response = NoteResponseDto.builder()
                                      .id(note.getId())
                                      .content(note.getContent())
                                      .createdAt(note.getCreatedAt())
                                      .updatedAt(note.getUpdatedAt())
                                      .build();

                                    responses.add(response);
        }
        return responses; 
    }
    
    @PostMapping
    public NoteResponseDto createNote(@RequestBody CreateNoteRequestDto request) {
        
        Note note = noteService.createNote(request.getContent());
        
        return NoteResponseDto.builder()
                .id(note.getId())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }


    @PutMapping("/{id}")
    public NoteResponseDto updateNote(@PathVariable Long id, @RequestBody UpdateNoteRequestDto request) {
       Note note = noteService.updateNote(id, request.getContent());

       //Convert Note to NoteResponseDto
       return NoteResponseDto.builder()
            .id(note.getId())
            .content(note.getContent())
            .createdAt(note.getCreatedAt())
            .updatedAt(note.getUpdatedAt())
            .build();

    }

    @DeleteMapping("/{id}")
    public void deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
    }

}
