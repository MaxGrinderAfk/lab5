package idespring.lab5.repository.subjectrepo;

import idespring.lab5.model.Subject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Query(value = "SELECT * FROM studentmanagement.subjects WHERE "
            + "name = :name", nativeQuery = true)
    Optional<Subject> findByName(@Param("name") String name);

    @Query(value = "SELECT * FROM studentmanagement.subjects WHERE "
            + "name LIKE CONCAT('%', :namePattern, '%')", nativeQuery = true)
    List<Subject> findByNameContaining(@Param("namePattern") String namePattern);

    @Query(value = "SELECT CASE WHEN COUNT(id) > 0 THEN true ELSE "
            + "false END FROM studentmanagement.subjects WHERE name = :name",
            nativeQuery = true)
    boolean existsByName(@Param("name") String name);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM studentmanagement.subjects WHERE name = :name", nativeQuery = true)
    void deleteByName(@Param("name") String name);

    @Query(value = "SELECT * FROM studentmanagement.subjects ORDER BY name", nativeQuery = true)
    List<Subject> findAllByOrderByNameAsc();

    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.students WHERE s.id = :id")
    Optional<Subject> findByIdWithStudents(@Param("id") Long id);

    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.marks WHERE s.id = :id")
    Optional<Subject> findByIdWithMarks(@Param("id") Long id);

    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.students "
            + "LEFT JOIN FETCH s.marks WHERE s.id = :id")
    Optional<Subject> findByIdWithStudentsAndMarks(@Param("id") Long id);

    @Query(value = "SELECT s.* FROM studentmanagement.subjects s "
            + "JOIN studentmanagement.student_subject ss ON s.id = ss.subjectid "
            + "WHERE ss.studentid = :studentId", nativeQuery = true)
    List<Subject> findByStudentId(@Param("studentId") Long studentId);
}