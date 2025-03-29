package idespring.lab5.repository.grouprepo;

import idespring.lab5.model.Group;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query(value = "SELECT * FROM studentmanagement.groups WHERE name = :name", nativeQuery = true)
    Optional<Group> findByName(@Param("name") String name);

    @Query(value = "SELECT * FROM studentmanagement.groups WHERE name LIKE "
            + "CONCAT('%', :namePattern, '%')", nativeQuery = true)
    List<Group> findByNameContaining(@Param("namePattern") String namePattern);

    @Query(value = "SELECT CASE WHEN COUNT(id) > 0 THEN true ELSE false END "
            + "FROM studentmanagement.groups WHERE name = :name",
            nativeQuery = true)
    boolean existsByName(@Param("name") String name);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM studentmanagement.groups WHERE name = :name", nativeQuery = true)
    void deleteByName(@Param("name") String name);

    @Query(value = "SELECT * FROM studentmanagement.groups ORDER BY name", nativeQuery = true)
    List<Group> findAllByOrderByNameAsc();

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.students s WHERE g.id = :id")
    Optional<Group> findByIdWithStudents(@Param("id") Long id);
}