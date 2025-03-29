package idespring.lab5.service.subjectservice;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.model.Mark;
import idespring.lab5.model.Subject;
import idespring.lab5.repository.markrepo.MarkRepository;
import idespring.lab5.repository.subjectrepo.SubjectRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final MarkRepository markRepository;
    private final CacheConfig<String, Object> cache;
    private static final String NOTFOUND = "Subject not found with id: ";
    private static final Logger logger = LoggerFactory.getLogger(SubjectServiceImpl.class);

    @Autowired
    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              MarkRepository markRepository,
                              CacheConfig<String, Object> cache) {
        this.subjectRepository = subjectRepository;
        this.markRepository = markRepository;
        this.cache = cache;
    }

    @Override
    public List<Subject> readSubjects(String namePattern, String sort) {
        String cacheKey = namePattern + "-" + (sort != null ? sort : "default");
        if (cache.get(cacheKey) != null) {
            return (List<Subject>) cache.get(cacheKey);
        }

        final long start = System.nanoTime();
        logger.info("Fetching subjects from database for namePattern: {}, sort: {}",
                namePattern, sort);

        List<Subject> subjects;
        if (namePattern != null) {
            subjects = subjectRepository.findByNameContaining(namePattern);
        } else if ("asc".equalsIgnoreCase(sort)) {
            subjects = subjectRepository.findAllByOrderByNameAsc();
        } else {
            subjects = subjectRepository.findAll();
        }

        cache.put(cacheKey, subjects);
        long end = System.nanoTime();
        logger.info("Execution time for readSubjects: {} ms", (end - start) / 1_000_000);
        return subjects;
    }

    @Override
    public Subject findById(Long id) {
        String cacheKey = "subject-" + id;
        if (cache.get(cacheKey) != null) {
            return (Subject) cache.get(cacheKey);
        }

        long start = System.nanoTime();
        logger.info("Fetching subject from database for id: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOTFOUND + id));

        cache.put(cacheKey, subject);
        long end = System.nanoTime();
        logger.info("Execution time for findById: {} ms", (end - start) / 1_000_000);
        return subject;
    }

    @Override
    public Subject findByName(String name) {
        String cacheKey = "subject-" + name;
        if (cache.get(cacheKey) != null) {
            return (Subject) cache.get(cacheKey);
        }

        long start = System.nanoTime();
        logger.info("Fetching subject from database for name: {}", name);

        Subject subject = subjectRepository.findByName(name)
                .orElseThrow(() -> new
                        EntityNotFoundException("Subject not found with name: " + name));

        cache.put(cacheKey, subject);
        long end = System.nanoTime();
        logger.info("Execution time for findByName: {} ms", (end - start) / 1_000_000);
        return subject;
    }

    @Override
    public Subject addSubject(Subject subject) {
        final long start = System.nanoTime();
        logger.info("Saving subject: {}", subject.getName());

        Subject savedSubject = subjectRepository.save(subject);
        cache.put("subject-" + savedSubject.getId(), savedSubject);
        cache.put("subject-" + savedSubject.getName(), savedSubject);

        long end = System.nanoTime();
        logger.info("Execution time for addSubject: {} ms", (end - start) / 1_000_000);
        return savedSubject;
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        logger.info("Deleting subject with id: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOTFOUND + id));

        clearCacheForSubject(subject);

        subjectRepository.deleteById(id);
        logger.info("Subject with id {} deleted", id);
    }

    @Override
    @Transactional
    public void deleteSubjectByName(String name) {
        logger.info("Deleting subject with name: {}", name);
        Subject subject = subjectRepository.findByName(name)
                .orElseThrow(() ->
                        new EntityNotFoundException("Subject not found with name: " + name));

        clearCacheForSubject(subject);

        subjectRepository.deleteByName(name);
        logger.info("Subject with name {} deleted", name);
    }

    @Override
    public boolean existsByName(String name) {
        logger.info("Checking existence of subject with name: {}", name);
        return subjectRepository.existsByName(name);
    }

    private void clearCacheForSubject(Subject subject) {
        Long subjectId = subject.getId();

        cache.remove("subject-" + subjectId);
        cache.remove("subject-" + subject.getName());

        cache.remove("avg-subject-" + subjectId);

        List<Mark> subjectMarks = markRepository.findBySubjectId(subjectId);

        Set<Long> affectedStudentIds = subjectMarks.stream()
                .map(mark -> mark.getStudent().getId())
                .collect(Collectors.toSet());

        for (Long studentId : affectedStudentIds) {
            cache.remove("marks-" + studentId + "-" + subjectId);
            cache.remove("avg-student-" + studentId);
        }

        for (Mark mark : subjectMarks) {
            cache.remove("mark-" + mark.getId());
        }
    }
}