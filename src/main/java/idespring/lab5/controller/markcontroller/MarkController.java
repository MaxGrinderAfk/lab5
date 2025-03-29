package idespring.lab5.controller.markcontroller;

import idespring.lab5.model.Mark;
import idespring.lab5.service.markservice.MarkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/marks")
public class MarkController {
    private final MarkService markService;

    public MarkController(MarkService markService) {
        this.markService = markService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Mark>> createMarksBulk(@RequestBody List<Mark> marks) {
        List<Mark> createdMarks = marks.stream()
                .map(markService::addMark)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(createdMarks);
    }

    @PostMapping
    public ResponseEntity<Mark> createMark(@Valid @RequestBody Mark mark) {
        return ResponseEntity.status(HttpStatus.CREATED).body(markService.addMark(mark));
    }

    @GetMapping
    public ResponseEntity<Set<Mark>> getMarks(
            @RequestParam(required = false) @Positive Long studentId,
            @RequestParam(required = false) @Positive Long subjectId) {
        Set<Mark> marks = new HashSet<>(markService.readMarks(studentId, subjectId));
        return marks.isEmpty()
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(marks);
    }

    @GetMapping("/value/{value}")
    public ResponseEntity<Set<Mark>> getMarksByValue(@Positive @PathVariable int value) {
        Set<Mark> marks = new HashSet<>(markService.findByValue(value));
        return marks.isEmpty()
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(marks);
    }

    @GetMapping("/average/student/{studentId}")
    public ResponseEntity<Double> getAverageMarkByStudent(
            @Positive @NotNull @PathVariable Long studentId) {
        Double average = markService.getAverageMarkByStudentId(studentId);
        return (average != null)
                ? ResponseEntity.ok(average)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/average/subject/{subjectId}")
    public ResponseEntity<Double> getAverageMarkBySubject(
            @Positive @NotNull @PathVariable Long subjectId) {
        Double average = markService.getAverageMarkBySubjectId(subjectId);
        return (average != null)
                ? ResponseEntity.ok(average)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @DeleteMapping("/delete-specific")
    public ResponseEntity<String> deleteSpecificMark(
            @RequestParam @Positive Long studentId,
            @RequestParam @NotNull String subjectName,
            @RequestParam @Positive int markValue,
            @RequestParam(required = false) @Positive Long id) {
        markService.deleteMarkSpecific(studentId, subjectName, markValue, id);
        return ResponseEntity.ok("Specific mark deleted successfully.");
    }

    @DeleteMapping("/{markId}")
    public ResponseEntity<Void> deleteMark(@Positive @NotNull @PathVariable Long markId) {
        markService.deleteMark(markId);
        return ResponseEntity.ok().build();
    }
}