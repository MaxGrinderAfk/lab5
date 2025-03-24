package idespring.lab5.repository.studentrepo;

import idespring.lab5.model.Student;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, StudentRepositoryCustom {

    @Query(value = "SELECT * FROM studentmanagement.students WHERE age = :age", nativeQuery = true)
    Set<Student> findByAge(@Param("age") int age);

    @Query(value = "SELECT * FROM studentmanagement.students WHERE id = :id", nativeQuery = true)
    Optional<Student> findById(@Param("id") long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE studentmanagement.students SET "
            + "name = :name, age = :age WHERE id = :id", nativeQuery = true)
    void update(@Param("name") String name, @Param("age") int age, @Param("id") long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM studentmanagement.students WHERE id = :id", nativeQuery = true)
    void delete(@Param("id") long id);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO studentmanagement.student_subject "
            + "(studentid, subjectid) VALUES (:studentId, :subjectId)",
            nativeQuery = true)
    void addSubject(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM studentmanagement.student_subject WHERE "
            + "studentid = :studentId AND subjectid = :subjectId",
            nativeQuery = true)
    void removeSubject(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Query(value = "SELECT * FROM studentmanagement.students WHERE "
            + "groupid = :groupId", nativeQuery = true)
    Set<Student> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.subjects WHERE s.id = :id")
    Optional<Student> findByIdWithSubjects(@Param("id") Long id);
}