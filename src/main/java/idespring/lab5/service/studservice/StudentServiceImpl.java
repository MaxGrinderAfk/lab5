package idespring.lab5.service.studservice;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.model.Mark;
import idespring.lab5.model.Student;
import idespring.lab5.model.Subject;
import idespring.lab5.repository.studentrepo.StudentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentServiceImpl implements StudentServ {
    private final StudentRepository studentRepository;
    private final CacheConfig<String, Object> cache;
    private static final String NOTFOUND = "Student not found with id: ";
    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

    @Autowired
    public StudentServiceImpl(StudentRepository studentRepository,
                              CacheConfig<String, Object> cache) {
        this.studentRepository = studentRepository;
        this.cache = cache;
    }

    @Override
    public List<Student> readStudents(Integer age, String sort, Long id) {
        long start = System.nanoTime();
        String cacheKey = age + "-" + sort + "-" + id;
        logger.info("Fetching students with age: {}, sort: {}, id: {}", age, sort, id);

        @SuppressWarnings("unchecked")
        List<Student> students = (List<Student>) cache.get(cacheKey);
        if (students == null) {
            if (id != null) {
                students = Collections.singletonList(
                        studentRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException(NOTFOUND + id))
                );
            } else if (age != null && sort != null) {
                students = studentRepository.findByAgeAndSortByName(age, sort);
            } else if (age != null) {
                students = studentRepository.findByAge(age).stream().toList();
            } else if (sort != null) {
                students = studentRepository.sortByName(sort);
            } else {
                students = studentRepository.findAll();
            }
            cache.put(cacheKey, students);
        }

        long end = System.nanoTime();
        logger.info("Execution time for readStudents: {} ms", (end - start) / 1_000_000);
        return students;
    }

    @Override
    public List<Student> findByGroupId(Long groupId) {
        logger.info("Fetching students from group ID: {}", groupId);
        String cacheKey = "group-" + groupId;

        @SuppressWarnings("unchecked")
        List<Student> students = (List<Student>) cache.get(cacheKey);
        if (students == null) {
            students = studentRepository.findByGroupId(groupId).stream().toList();
            cache.put(cacheKey, students);
        }
        return students;
    }

    @Override
    public Student findById(Long id) {
        long start = System.nanoTime();
        logger.info("Fetching student from database with id: {}", id);

        Student student = (Student) cache.get(id.toString());
        if (student == null) {
            student = studentRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException(NOTFOUND + id));
            cache.put(id.toString(), student);
        }

        long end = System.nanoTime();
        logger.info("Execution time for findById: {} ms", (end - start) / 1_000_000);
        return student;
    }

    @Override
    public Student addStudent(Student student) {
        final long start = System.nanoTime();
        logger.info("Saving student: {}", student.getName());

        final Set<Long> subjectIds = student.getSubjects().stream()
                .map(Subject::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Mark mark : student.getMarks()) {
            mark.setStudent(student);
        }

        student.setSubjects(new HashSet<>());
        Student savedStudent = studentRepository.save(student);
        cache.put(savedStudent.getId().toString(), savedStudent);

        for (Long subjectId : subjectIds) {
            studentRepository.addSubject(savedStudent.getId(), subjectId);
        }

        clearRelatedCaches(savedStudent);

        long end = System.nanoTime();
        logger.info("Execution time for addStudent: {} ms", (end - start) / 1_000_000);
        return savedStudent;
    }

    @Override
    public void updateStudent(String name, int age, long id) {
        logger.info("Updating student with id: {}", id);
        Student student = findById(id);
        studentRepository.update(name, age, id);
        clearStudentCache(id);
        clearRelatedCaches(student);
        logger.info("Student with id {} updated", id);
    }

    @Override
    @Transactional
    public void deleteStudent(long id) {
        logger.info("Deleting student with id: {}", id);
        Student student = studentRepository.findById(id).orElseThrow();
        final Long groupId = student.getGroup() != null ? student.getGroup().getId() : null;
        final int age = student.getAge();

        student.getSubjects().clear();
        studentRepository.saveAndFlush(student);
        studentRepository.delete(student);

        clearStudentCache(id);
        clearCachesByAge(age);
        if (groupId != null) {
            clearGroupCache(groupId);
        }
        clearListCaches();

        logger.info("Student with id {} deleted", id);
    }

    public void clearStudentCache(long id) {
        cache.remove(String.valueOf(id));
        logger.info("Cleared cache for student id: {}", id);
    }

    public void clearGroupCache(Long groupId) {
        String groupCacheKey = "group-" + groupId;
        cache.remove(groupCacheKey);
        cache.remove("students-in-group-" + groupId);
        logger.info("Cleared cache for group id: {}", groupId);
    }


    public void clearCachesByAge(int age) {
        List<String> keysToRemove = new ArrayList<>();

        for (int i = 0; i < cache.size(); i++) {
            String key = age + "-null-null";
            keysToRemove.add(key);

            key = age + "-asc-null";
            keysToRemove.add(key);

            key = age + "-desc-null";
            keysToRemove.add(key);
        }

        for (String key : keysToRemove) {
            cache.remove(key);
        }

        logger.info("Cleared caches for age: {}", age);
    }

    public void clearListCaches() {
        List<String> keysToRemove = new ArrayList<>();

        keysToRemove.add("null-null-null");
        keysToRemove.add("null-asc-null");
        keysToRemove.add("null-desc-null");

        for (String key : keysToRemove) {
            cache.remove(key);
        }

        logger.info("Cleared list caches");
    }

    private void clearRelatedCaches(Student student) {
        if (student == null) {
            return;
        }

        int age = student.getAge();
        Long groupId = student.getGroup() != null ? student.getGroup().getId() : null;

        clearCachesByAge(age);
        if (groupId != null) {
            clearGroupCache(groupId);
        }
        clearListCaches();

        logger.info("Cleared all related caches for student: {}", student.getId());
    }
}
