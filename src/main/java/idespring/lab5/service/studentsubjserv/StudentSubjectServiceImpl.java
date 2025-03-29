package idespring.lab5.service.studentsubjserv;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.model.Student;
import idespring.lab5.model.Subject;
import idespring.lab5.repository.studentrepo.StudentRepository;
import idespring.lab5.repository.subjectrepo.SubjectRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentSubjectServiceImpl implements StudentSubjectService {
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final CacheConfig<String, Object> cache;
    private static final String STUDENT_ERR = "Student not found";
    private static final String SUBJECT_ERR = "Subject not found";
    private static final Logger logger = LoggerFactory.getLogger(StudentSubjectServiceImpl.class);

    @Autowired
    public StudentSubjectServiceImpl(StudentRepository studentRepository,
                                     SubjectRepository subjectRepository,
                                     CacheConfig<String, Object> cache) {
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.cache = cache;
    }

    private void clearCaches(Long studentId, Long subjectId) {
        cache.remove("subjects-" + studentId);
        cache.remove("students-" + subjectId);

        cache.remove("student-with-subjects-" + studentId);
        cache.remove("subject-with-students-" + subjectId);

        logger.debug("Cleared caches for student {} and subject {}", studentId, subjectId);
    }

    @Override
    @Transactional
    public void addSubjectToStudent(Long studentId, Long subjectId) {
        logger.info("Adding subject {} to student {}", subjectId, studentId);

        studentRepository.findById(studentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(STUDENT_ERR));
        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(SUBJECT_ERR));

        studentRepository.addSubject(studentId, subjectId);
        clearCaches(studentId, subjectId);
        logger.info("Subject {} added to student {}", subjectId, studentId);
    }

    @Override
    @Transactional
    public void removeSubjectFromStudent(Long studentId, Long subjectId) {
        logger.info("Removing subject {} from student {}", subjectId, studentId);

        studentRepository.findById(studentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(STUDENT_ERR));
        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(SUBJECT_ERR));

        studentRepository.removeSubject(studentId, subjectId);
        clearCaches(studentId, subjectId);
        logger.info("Subject {} removed from student {}", subjectId, studentId);
    }

    @Override
    public List<Subject> getSubjectsByStudent(Long studentId) {
        long start = System.nanoTime();
        logger.info("Fetching subjects for student {}", studentId);

        @SuppressWarnings("unchecked")
        List<Subject> subjects = (List<Subject>) cache.get("subjects-" + studentId);
        if (subjects == null) {
            subjects = subjectRepository.findByStudentId(studentId);
            cache.put("subjects-" + studentId, subjects);
        }

        long end = System.nanoTime();
        logger.info("Execution time for getSubjectsByStudent: {} ms", (end - start) / 1_000_000);
        return subjects;
    }

    @Override
    public Set<Student> getStudentsBySubject(Long subjectId) {
        long start = System.nanoTime();
        logger.info("Fetching students for subject {}", subjectId);

        @SuppressWarnings("unchecked")
        Set<Student> students = (Set<Student>) cache.get("students-" + subjectId);
        if (students == null) {
            Subject subject = subjectRepository.findByIdWithStudents(subjectId)
                    .orElseThrow(() ->
                            new jakarta.persistence.EntityNotFoundException(SUBJECT_ERR));
            students = subject.getStudents();
            cache.put("students-" + subjectId, students);
        }

        long end = System.nanoTime();
        logger.info("Execution time for getStudentsBySubject: {} ms", (end - start) / 1_000_000);
        return students;
    }

    @Override
    public Student findStudentWithSubjects(Long studentId) {
        long start = System.nanoTime();
        logger.info("Fetching student with subjects for ID: {}", studentId);

        Student student = (Student) cache.get("student-with-subjects-" + studentId);
        if (student == null) {
            student = studentRepository.findByIdWithSubjects(studentId)
                    .orElseThrow(() ->
                            new jakarta.persistence.EntityNotFoundException(STUDENT_ERR));
            cache.put("student-with-subjects-" + studentId, student);
        }

        long end = System.nanoTime();
        logger.info("Execution time for findStudentWithSubjects: {} ms", (end - start) / 1_000_000);
        return student;
    }

    @Override
    public Subject findSubjectWithStudents(Long subjectId) {
        long start = System.nanoTime();
        logger.info("Fetching subject with students for ID: {}", subjectId);

        Subject subject = (Subject) cache.get("subject-with-students-" + subjectId);
        if (subject == null) {
            subject = subjectRepository.findByIdWithStudents(subjectId)
                    .orElseThrow(() -> new EntityNotFoundException(SUBJECT_ERR));
            cache.put("subject-with-students-" + subjectId, subject);
        }

        long end = System.nanoTime();
        logger.info("Execution time for findSubjectWithStudents: {} ms", (end - start) / 1_000_000);
        return subject;
    }
}