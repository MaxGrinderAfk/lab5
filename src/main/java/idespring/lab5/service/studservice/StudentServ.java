package idespring.lab5.service.studservice;

import idespring.lab5.model.Student;
import java.util.List;

public interface StudentServ {
    List<Student> readStudents(Integer age, String sort, Long id);

    List<Student> findByGroupId(Long groupId);

    Student findById(Long id);

    Student addStudent(Student student);

    void updateStudent(String name, int age, long id);

    void deleteStudent(long id);
}