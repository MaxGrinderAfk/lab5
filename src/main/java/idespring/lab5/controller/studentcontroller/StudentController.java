package idespring.lab5.controller.studentcontroller;

import idespring.lab5.model.Student;
import idespring.lab5.service.studservice.StudentServ;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/students")
public class StudentController {
    private final StudentServ studentService;

    @Autowired
    public StudentController(StudentServ studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Student>> createStudentsBulk(@RequestBody List<Student> students) {
        List<Student> createdStudents = students.stream()
                .map(studentService::addStudent)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(createdStudents);
    }

    @PostMapping
    public ResponseEntity<Student> createStudent(@Valid @RequestBody Student student) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.addStudent(student));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<Student> getStudentById(@Positive @NotNull @PathVariable Long studentId) {
        Student student = studentService.findById(studentId);
        return ResponseEntity.ok(student);
    }

    @GetMapping
    public ResponseEntity<List<Student>> getStudents(
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Long id) {
        List<Student> students = studentService.readStudents(age, sort, id);
        return students.isEmpty()
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(students);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Student>>
        getStudentsByGroup(@Positive @NotNull @PathVariable Long groupId) {
        List<Student> students = studentService.findByGroupId(groupId);
        return students.isEmpty()
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(students);
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<Student> updateStudent(
            @Positive @NotNull @PathVariable Long studentId,
            @RequestParam(required = false, defaultValue = "unknown") String name,
            @Positive @RequestParam(required = false, defaultValue = "15") int age) {
        studentService.updateStudent(name, age, studentId);
        return ResponseEntity.ok(studentService.findById(studentId));
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deleteStudent(@Positive @NotNull @PathVariable Long studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.ok().build();
    }
}