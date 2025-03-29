package idespring.lab5.controller.subjectcontroller;

import idespring.lab5.model.Subject;
import idespring.lab5.service.subjectservice.SubjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/subjects")
public class SubjectController {
    private final SubjectService subjectService;

    @Autowired
    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Subject>>
        createSubjectsBulk(@RequestBody @Valid List<Subject> subjects) {
        List<Subject> createdSubjects = subjects.stream()
                .filter(subject -> !subjectService.existsByName(subject.getName()))
                .map(subjectService::addSubject)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubjects);
    }

    @PostMapping
    public ResponseEntity<Subject> createSubject(@Valid @RequestBody Subject subject) {
        if (subjectService.existsByName(subject.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.addSubject(subject));
    }

    @GetMapping
    public ResponseEntity<Set<Subject>> getSubjects(
            @RequestParam(required = false) String namePattern,
            @RequestParam(required = false) String sort) {
        Set<Subject> subjects = new HashSet<>(subjectService.readSubjects(namePattern, sort));
        return subjects.isEmpty()
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(subjects);
    }

    @GetMapping("/{subjectId}")
    public ResponseEntity<Subject> getSubjectById(@PathVariable @NotNull @Positive Long subjectId) {
        Subject subject = subjectService.findById(subjectId);
        return ResponseEntity.ok(subject);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Subject> getSubjectByName(@PathVariable @NotEmpty String name) {
        Subject subject = subjectService.findByName(name);
        return ResponseEntity.ok(subject);
    }

    @DeleteMapping("/{subjectId}")
    public ResponseEntity<Void> deleteSubject(@PathVariable @NotNull @Positive Long subjectId) {
        subjectService.deleteSubject(subjectId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/name/{name}")
    public ResponseEntity<Void> deleteSubjectByName(@PathVariable @NotEmpty String name) {
        subjectService.deleteSubjectByName(name);
        return ResponseEntity.ok().build();
    }
}