package idespring.lab5.controller.studsubcontroller;

import idespring.lab5.model.Student;
import idespring.lab5.model.Subject;
import idespring.lab5.service.studentsubjserv.StudentSubjectService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/student-subjects")
public class StudentSubjectController {
    private final StudentSubjectService studentSubjectService;

    @Autowired
    public StudentSubjectController(StudentSubjectService studentSubjectService) {
        this.studentSubjectService = studentSubjectService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<Void> addSubjectsToStudentBulk(
            @RequestParam @NotNull @Positive Long studentId,
            @RequestBody @NotNull List<@Positive Long> subjectIds) {
        subjectIds.stream()
                .distinct()
                .forEach(subjectId ->
                        studentSubjectService.addSubjectToStudent(studentId, subjectId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping
    public ResponseEntity<Void> addSubjectToStudent(
            @RequestParam @NotNull @Positive Long studentId,
            @RequestParam @NotNull @Positive Long subjectId) {
        studentSubjectService.addSubjectToStudent(studentId, subjectId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> removeSubjectFromStudent(
            @RequestParam @NotNull @Positive Long studentId,
            @RequestParam @NotNull @Positive Long subjectId) {
        studentSubjectService.removeSubjectFromStudent(studentId, subjectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{studentId}/subjects")
    public ResponseEntity<Set<Subject>>
        getSubjectsByStudent(@PathVariable @NotNull @Positive Long studentId) {
        Set<Subject> subjects = new
                HashSet<>(studentSubjectService.getSubjectsByStudent(studentId));
        return subjects.isEmpty()
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(subjects);
    }

    @GetMapping("/{subjectId}/students")
    public ResponseEntity<Set<Student>>
        getStudentsBySubject(@PathVariable @NotNull @Positive Long subjectId) {
        Set<Student> students = studentSubjectService.getStudentsBySubject(subjectId);
        return students.isEmpty()
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(students);
    }

    @GetMapping("/student/{studentId}/with-subjects")
    public ResponseEntity<Student>
        getStudentWithSubjects(@PathVariable @NotNull @Positive Long studentId) {
        Student student = studentSubjectService.findStudentWithSubjects(studentId);
        return ResponseEntity.ok(student);
    }

    @GetMapping("/subject/{subjectId}/with-students")
    public ResponseEntity<Subject>
        getSubjectWithStudents(@PathVariable @NotNull @Positive Long subjectId) {
        Subject subject = studentSubjectService.findSubjectWithStudents(subjectId);
        return ResponseEntity.ok(subject);
    }
}