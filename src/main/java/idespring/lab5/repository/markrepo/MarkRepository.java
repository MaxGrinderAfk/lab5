package idespring.lab5.repository.markrepo;

import idespring.lab5.model.Mark;
import idespring.lab5.model.Student;
import idespring.lab5.model.Subject;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {

    @Query(value = "SELECT * FROM studentmanagement.marks WHERE studentid = :#{#student.id} "
            + "AND subjectid = :#{#subject.id}", nativeQuery = true)
    List<Mark> findByStudentAndSubject(@Param("student") Student student,
                                       @Param("subject") Subject subject);

    @Query(value = "SELECT * FROM studentmanagement.marks WHERE value = :value", nativeQuery = true)
    List<Mark> findByValue(@Param("value") int value);

    @Query(value = "SELECT AVG(value) FROM studentmanagement.marks "
            + "WHERE studentid = :studentId", nativeQuery = true)
    Double getAverageMarkByStudentId(@Param("studentId") Long studentId);

    @Query(value = "SELECT AVG(value) FROM studentmanagement.marks "
            + "WHERE subjectid = :subjectId", nativeQuery = true)
    Double getAverageMarkBySubjectId(@Param("subjectId") Long subjectId);

    @Query(value = "SELECT * FROM studentmanagement.marks WHERE "
            + "studentid = :studentId", nativeQuery = true)
    List<Mark> findByStudentId(@Param("studentId") Long studentId);

    @Query(value = "SELECT * FROM studentmanagement.marks WHERE "
            + "subjectid = :subjectId", nativeQuery = true)
    List<Mark> findBySubjectId(@Param("subjectId") Long subjectId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM studentmanagement.marks "
            + "WHERE studentid = :studentId "
            + "AND (:id IS NULL OR id = :id) "
            + "AND value = :markValue "
            + "AND subjectid = (SELECT id FROM studentmanagement.subjects "
            + "WHERE name = :subjectName)",
            nativeQuery = true)
    int deleteMarkByStudentIdSubjectNameValueAndOptionalId(@Param("studentId") Long studentId,
                                                           @Param("subjectName") String subjectName,
                                                           @Param("markValue") int markValue,
                                                           @Param("id") Long id);
}
