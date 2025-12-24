package com.authservice.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.authservice.model.Note;
import com.authservice.model.User;
import com.authservice.repository.NoteRepository;
import com.authservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    /**
     * Getting current authenticated user's ID from JWT token in SecurityContext
     * FIXED: Now properly handles multi-app support by using the User principal
     */
    private Long getCurrentAuthenticatedUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // The principal is the actual User object (set by JwtAuthenticationFilter)
        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            // Direct access to authenticated user - most reliable
            User user = (User) principal;
            return user.getId();
        }

        // Fallback: This shouldn't happen but provides backwards compatibility
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId();
    }

    /**
     * This method is getting all notes in descending order for currently
     * logged-in user based on userId generated from previous method
     * getCurrentAuthenticatedUserId()
     */
    public List<Note> getUserNotes() {
        Long currentUserId = getCurrentAuthenticatedUserId();
        return noteRepository.findByUserIdOrderByCreatedAtAsc(currentUserId);
    }

    /**
     * Creating a new note for the current user
     * 
     * @param content - The users though/idea text
     * @return Created note
     */
    public Note createNote(String content) {

        // Validating content is not null
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Note content cannot be empty");
        }

        // Getting user
        Long currentUserId = getCurrentAuthenticatedUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build note
        Note note = Note.builder()
                .content(content)
                .user(user)
                .build();

        // Saving Note
        return noteRepository.save(note);
    }

    /**
     * Update an existing note, & only if it belongs to the current user
     * 
     * @param noteId
     * @param newContent
     * @return Updated note
     */
    public Note updateNote(Long noteId, String newContent) {

        // Validate new content is not null
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new RuntimeException("Note content cannot be empty");
        }

        Long currentUserId = getCurrentAuthenticatedUserId();

        // Security check, Note belongs to current user
        Note note = noteRepository.findByIdAndUserId(noteId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Note not found or access forbidden"));

        // Update & save new content
        note.setContent(newContent);
        return noteRepository.save(note);
    }

    /**
     * Delete a user's note
     * @param noteId
     */
    public void deleteNote(Long noteId) {
        Long currentUserId = getCurrentAuthenticatedUserId();

        // Security check: Note must belong to current user
        Note note = noteRepository.findByIdAndUserId(noteId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Note not found or access forbidden"));

        noteRepository.delete(note);
    }

}
