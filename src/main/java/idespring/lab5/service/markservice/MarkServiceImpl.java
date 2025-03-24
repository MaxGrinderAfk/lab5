package idespring.lab5.service.markservice;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.exceptions.SubjectNotAssignedException;
import idespring.lab5.model.Mark;
import idespring.lab5.model.Student;
import idespring.lab5.model.Subject;
import idespring.lab5.repository.markrepo.MarkRepository;
import idespring.lab5.repository.studentrepo.StudentRepository;
import idespring.lab5.repository.subjectrepo.SubjectRepository;
import idespring.lab5.service.studentsubjserv.StudentSubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarkServiceImpl implements MarkService {
    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentSubjectService studentSubjectService;
    private final CacheConfig<String, Object> cache;
    private static final Logger logger = LoggerFactory.getLogger(MarkServiceImpl.class);

    @Autowired
    public MarkServiceImpl(MarkRepository markRepository,
                           StudentRepository studentRepository,
                           SubjectRepository subjectRepository,
                           StudentSubjectService studentSubjectService,
                           CacheConfig<String, Object> cache) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.studentSubjectService = studentSubjectService;
        this.cache = cache;
    }

    @Override
    public List<Mark> readMarks(Long studentId, Long subjectId) {
        boolean hasSubject = studentSubjectService.getSubjectsByStudent(studentId)
                .stream().anyMatch(s ->
                        s.getId().equals(subjectId));
        if (!hasSubject) {
            throw new SubjectNotAssignedException("Student with ID " + studentId
                    + " does not have subject with ID " + subjectId);
        }

        String cacheKey = "marks-" + (studentId != null ? studentId : "all")
                + "-" + subjectId;
        List<Mark> cachedMarks = (List<Mark>) cache.get(cacheKey);
        if (cachedMarks != null) {
            return cachedMarks;
        }

        logger.info("Fetching marks for student: {}, subject: {}", studentId, subjectId);
        List<Mark> marks;
        if (studentId != null && subjectId != null) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Student not found with id: " + studentId));
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Subject not found with id: " + subjectId));
            marks = markRepository.findByStudentAndSubject(student, subject);
        } else if (studentId != null) {
            marks = markRepository.findByStudentId(studentId);
        } else if (subjectId != null) {
            marks = markRepository.findBySubjectId(subjectId);
        } else {
            marks = markRepository.findAll();
        }

        cache.put(cacheKey, marks);
        return marks;
    }

    @Override
    public List<Mark> findByValue(int value) {
        String cacheKey = "value-" + value;
        List<Mark> cachedMarks = (List<Mark>) cache.get(cacheKey);
        if (cachedMarks != null) {
            return cachedMarks;
        }

        List<Mark> marks = markRepository.findByValue(value);
        cache.put(cacheKey, marks);
        return marks;
    }

    @Override
    public Double getAverageMarkByStudentId(Long studentId) {
        String cacheKey = "avg-student-" + studentId;
        Double cachedAvg = (Double) cache.get(cacheKey);
        if (cachedAvg != null) {
            return cachedAvg;
        }

        Double avgMark = markRepository.getAverageMarkByStudentId(studentId);
        cache.put(cacheKey, avgMark);
        return avgMark;
    }

    @Override
    public Double getAverageMarkBySubjectId(Long subjectId) {
        String cacheKey = "avg-subject-" + subjectId;
        Double cachedAvg = (Double) cache.get(cacheKey);
        if (cachedAvg != null) {
            return cachedAvg;
        }

        Double avgMark = markRepository.getAverageMarkBySubjectId(subjectId);
        cache.put(cacheKey, avgMark);
        return avgMark;
    }

    @Override
    @Transactional
    public void deleteMarkSpecific(Long studentId, String subjectName, int markValue, Long id) {
        logger.info("Deleting specific mark for student: {}, subject: {}, value: {}, id: {}",
                studentId, subjectName, markValue, id);

        Subject subject = subjectRepository.findByName(subjectName)
                .orElseThrow(() ->
                        new EntityNotFoundException("Subject not found with name: " + subjectName));
        Long subjectId = subject.getId();

        int deletedCount = markRepository.deleteMarkByStudentIdSubjectNameValueAndOptionalId(
                studentId, subjectName, markValue, id);
        if (deletedCount == 0) {
            throw new EntityNotFoundException("Mark not found with the given criteria.");
        }

        clearCacheForSubject(subjectId);
        clearCacheForStudent(studentId);
        if (id != null) {
            cache.remove("mark-" + id);
        }
    }

    @Override
    @Transactional
    public Mark addMark(Mark mark) {
        logger.info("Adding mark for student: {}, subject: {}, value: {}",
                mark.getStudent().getId(), mark.getSubject().getId(), mark.getValue());

        Student student = studentRepository.findById(mark.getStudent().getId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: "
                        + mark.getStudent().getId()));
        Subject subject = subjectRepository.findById(mark.getSubject().getId())
                .orElseThrow(() -> new EntityNotFoundException("Subject not found with id: "
                        + mark.getSubject().getId()));

        boolean hasSubject = studentSubjectService.getSubjectsByStudent(student.getId())
                .stream().anyMatch(s ->
                        s.getId().equals(subject.getId()));
        if (!hasSubject) {
            throw new SubjectNotAssignedException("Student with ID " + student.getId()
                    + " does not have subject with ID " + subject.getId());
        }

        final Mark savedMark = markRepository.save(mark);

        clearCacheForSubject(subject.getId());
        clearCacheForStudent(student.getId());

        return savedMark;
    }

    @Override
    @Transactional
    public void deleteMark(Long id) {
        logger.info("Deleting mark with id: {}", id);

        Mark mark = markRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mark not found with id: " + id));

        Long studentId = mark.getStudent().getId();
        Long subjectId = mark.getSubject().getId();

        markRepository.deleteById(id);

        clearCacheForSubject(subjectId);
        clearCacheForStudent(studentId);
    }

    public void clearCacheForSubject(Long subjectId) {
        logger.info("Clearing cache for subject with id: {}", subjectId);

        List<Mark> subjectMarks = markRepository.findBySubjectId(subjectId);

        Set<Long> affectedStudentIds = subjectMarks.stream()
                .map(mark -> mark.getStudent().getId())
                .collect(Collectors.toSet());

        for (Long studentId : affectedStudentIds) {
            cache.remove("marks-" + studentId + "-" + subjectId);
            cache.remove("marks-" + studentId + "-all");
            cache.remove("avg-student-" + studentId);
        }

        cache.remove("marks-all-" + subjectId);
        cache.remove("marks-all-all");
        cache.remove("avg-subject-" + subjectId);

        for (Mark mark : subjectMarks) {
            cache.remove("mark-" + mark.getId());
            cache.remove("value-" + mark.getValue());
        }
    }

    public void clearCacheForStudent(Long studentId) {
        logger.info("Clearing cache for student with id: {}", studentId);

        List<Mark> studentMarks = markRepository.findByStudentId(studentId);

        Set<Long> affectedSubjectIds = studentMarks.stream()
                .map(mark -> mark.getSubject().getId())
                .collect(Collectors.toSet());

        for (Long subjectId : affectedSubjectIds) {
            cache.remove("marks-" + studentId + "-" + subjectId);
            cache.remove("marks-all-" + subjectId);
            cache.remove("avg-subject-" + subjectId);
        }

        cache.remove("marks-" + studentId + "-all");
        cache.remove("marks-all-all");
        cache.remove("avg-student-" + studentId);

        for (Mark mark : studentMarks) {
            cache.remove("mark-" + mark.getId());
            cache.remove("value-" + mark.getValue());
        }
    }
}