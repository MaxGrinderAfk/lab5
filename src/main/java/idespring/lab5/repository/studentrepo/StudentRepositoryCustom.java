package idespring.lab5.repository.studentrepo;

import idespring.lab5.model.Student;

import java.util.List;

public interface StudentRepositoryCustom {
    List<Student> findByAgeAndSortByName(int age, String sort);

    List<Student> sortByName(String sort);
}
