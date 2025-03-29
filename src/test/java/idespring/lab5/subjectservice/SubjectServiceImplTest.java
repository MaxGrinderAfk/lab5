package idespring.lab5.subjectservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.model.Mark;
import idespring.lab5.model.Student;
import idespring.lab5.model.Subject;
import idespring.lab5.repository.markrepo.MarkRepository;
import idespring.lab5.repository.subjectrepo.SubjectRepository;
import idespring.lab5.service.subjectservice.SubjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
public class SubjectServiceImplTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private MarkRepository markRepository;

    @Mock
    private CacheConfig<String, Object> cache;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    private Subject testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new Subject();
        testSubject.setId(1L);
        testSubject.setName("Math");
    }

    // Тесты для readSubjects()
    @Test
    void readSubjects_ShouldReturnCachedList_WhenCacheExists() {
        String cacheKey = "test-default";
        List<Subject> cachedSubjects = Collections.singletonList(testSubject);
        when(cache.get(cacheKey)).thenReturn(cachedSubjects);

        List<Subject> result = subjectService.readSubjects("test", null);

        assertEquals(cachedSubjects, result);
        verify(subjectRepository, never()).findByNameContaining(any());
    }

    @Test
    void readSubjects_ShouldFetchFromDbAndCache_WhenNoCache() {
        String namePattern = "math";
        List<Subject> subjects = Collections.singletonList(testSubject);
        when(cache.get(any())).thenReturn(null);
        when(subjectRepository.findByNameContaining(namePattern)).thenReturn(subjects);

        List<Subject> result = subjectService.readSubjects(namePattern, null);

        assertEquals(subjects, result);
        verify(cache).put(eq(namePattern + "-default"), eq(subjects));
    }

    // Тесты для findById()
    @Test
    void findById_ShouldThrowException_WhenSubjectNotFound() {
        Long id = 2L;
        when(cache.get("subject-" + id)).thenReturn(null);
        when(subjectRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> subjectService.findById(id));
    }

    @Test
    void findById_ShouldReturnCachedSubject() {
        when(cache.get("subject-1")).thenReturn(testSubject);

        Subject result = subjectService.findById(1L);

        assertEquals(testSubject, result);
        verify(subjectRepository, never()).findById(any());
    }

    @Test
    void deleteSubject_ShouldClearCache() {
        // 1. Подготовка данных
        Student student = new Student();
        student.setId(100L); // Устанавливаем ID студента

        Mark mark = new Mark();
        mark.setStudent(student); // Устанавливаем студента в оценку

        List<Mark> marks = Collections.singletonList(mark);

        // 2. Настройка моков
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(markRepository.findBySubjectId(1L)).thenReturn(marks);

        // 3. Вызов метода
        subjectService.deleteSubject(1L);

        // 4. Проверки
        verify(cache).remove("subject-1");
        verify(cache).remove("subject-Math");
        verify(cache).remove("avg-subject-1");
        verify(cache).remove("marks-100-1");
        verify(cache).remove("avg-student-100");
        verify(cache).remove("mark-" + mark.getId());
        verify(subjectRepository).deleteById(1L);
    }

    // Тесты для addSubject()
    @Test
    void addSubject_ShouldCacheByBothIdAndName() {
        when(subjectRepository.save(testSubject)).thenReturn(testSubject);

        Subject result = subjectService.addSubject(testSubject);

        verify(cache).put("subject-1", testSubject);
        verify(cache).put("subject-Math", testSubject);
    }

    // Тесты для findByName()
    @Test
    void findByName_ShouldFetchFromDb_WhenNotCached() {
        when(cache.get("subject-Math")).thenReturn(null);
        when(subjectRepository.findByName("Math")).thenReturn(Optional.of(testSubject));

        Subject result = subjectService.findByName("Math");

        verify(cache).put("subject-Math", testSubject);
        assertEquals(testSubject, result);
    }

    // Тесты для CacheConfig
    @Test
    void cachePut_ShouldEvictOldestWhenMaxSizeReached() {
        CacheConfig<String, String> cache = new CacheConfig<>(1000, 2);
        cache.put("1", "A");
        cache.put("2", "B");
        cache.put("3", "C");

        assertNull(cache.get("1"));
        assertEquals("B", cache.get("2"));
    }

    @Test
    void cacheShouldExpireEntries() throws InterruptedException {
        CacheConfig<String, String> cache = new CacheConfig<>(500, 10);
        cache.put("temp", "value");
        TimeUnit.MILLISECONDS.sleep(600);

        assertNull(cache.get("temp"));
    }

    @Test
    void readSubjects_ShouldReturnCachedData_WhenCacheHit() {
        String namePattern = "math";
        String sort = "asc";
        String cacheKey = namePattern + "-" + sort;
        List<Subject> cachedSubjects = List.of(testSubject);

        when(cache.get(cacheKey)).thenReturn(cachedSubjects);

        List<Subject> result = subjectService.readSubjects(namePattern, sort);

        assertEquals(cachedSubjects, result);
        verify(subjectRepository, never()).findByNameContaining(any());
        verify(cache, never()).put(any(), any());
    }

    @Test
    void readSubjects_ShouldFetchFromDbAndCache_WhenNamePatternProvided() {
        String namePattern = "math";
        String sort = "desc";
        String cacheKey = namePattern + "-" + sort;
        List<Subject> subjects = List.of(testSubject);

        when(cache.get(cacheKey)).thenReturn(null);
        when(subjectRepository.findByNameContaining(namePattern)).thenReturn(subjects);

        List<Subject> result = subjectService.readSubjects(namePattern, sort);

        assertEquals(subjects, result);
        verify(subjectRepository).findByNameContaining(namePattern);
        verify(cache).put(cacheKey, subjects);
    }

    @Test
    void readSubjects_ShouldFetchAllSortedAsc_WhenNamePatternNullAndSortAsc() {
        String namePattern = null;
        String sort = "asc";
        String cacheKey = "null-asc";
        List<Subject> subjects = List.of(testSubject);

        when(cache.get(cacheKey)).thenReturn(null);
        when(subjectRepository.findAllByOrderByNameAsc()).thenReturn(subjects);

        List<Subject> result = subjectService.readSubjects(namePattern, sort);

        assertEquals(subjects, result);
        verify(subjectRepository).findAllByOrderByNameAsc();
        verify(cache).put(cacheKey, subjects);
    }

    @Test
    void readSubjects_ShouldFetchAll_WhenNamePatternNullAndSortNull() {
        String namePattern = null;
        String sort = null;
        String cacheKey = "null-default";
        List<Subject> subjects = List.of(testSubject);

        when(cache.get(cacheKey)).thenReturn(null);
        when(subjectRepository.findAll()).thenReturn(subjects);

        List<Subject> result = subjectService.readSubjects(namePattern, sort);

        assertEquals(subjects, result);
        verify(subjectRepository).findAll();
        verify(cache).put(cacheKey, subjects);
    }

    @Test
    void readSubjects_ShouldFetchAll_WhenNamePatternNullAndSortDesc() {
        String namePattern = null;
        String sort = "desc";
        String cacheKey = "null-desc";
        List<Subject> subjects = List.of(testSubject);

        when(cache.get(cacheKey)).thenReturn(null);
        when(subjectRepository.findAll()).thenReturn(subjects);

        List<Subject> result = subjectService.readSubjects(namePattern, sort);

        assertEquals(subjects, result);
        verify(subjectRepository).findAll();
        verify(cache).put(cacheKey, subjects);
    }

    @Test
    void readSubjects_ShouldHandleSortCaseInsensitive_WhenSortIsUpperCase() {
        String namePattern = null;
        String sort = "ASC";
        String cacheKey = "null-ASC";
        List<Subject> subjects = List.of(testSubject);

        when(cache.get(cacheKey)).thenReturn(null);
        when(subjectRepository.findAllByOrderByNameAsc()).thenReturn(subjects);

        List<Subject> result = subjectService.readSubjects(namePattern, sort);

        assertEquals(subjects, result);
        verify(subjectRepository).findAllByOrderByNameAsc();
        verify(cache).put(cacheKey, subjects);
    }

    @Test
    void readSubjects_ShouldUseFindByNameContaining_WhenNamePatternIsEmpty() {
        String namePattern = "";
        String sort = "default";
        String cacheKey = "-default";
        List<Subject> subjects = List.of(testSubject);

        when(cache.get(cacheKey)).thenReturn(null);
        when(subjectRepository.findByNameContaining(namePattern)).thenReturn(subjects);

        List<Subject> result = subjectService.readSubjects(namePattern, sort);

        assertEquals(subjects, result);
        verify(subjectRepository).findByNameContaining(namePattern);
        verify(cache).put(cacheKey, subjects);
    }

    @Test
    void deleteSubjectByName_ShouldDeleteSubjectAndClearCache_WhenSubjectExists() {
        String subjectName = "Math";
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName(subjectName);

        Mark mark = new Mark();
        Student student = new Student();
        student.setId(100L);
        mark.setStudent(student);
        mark.setId(200L);

        List<Mark> marks = List.of(mark);

        when(subjectRepository.findByName(subjectName)).thenReturn(Optional.of(subject));
        when(markRepository.findBySubjectId(1L)).thenReturn(marks);

        // Вызов метода
        subjectService.deleteSubjectByName(subjectName);

        // Проверки
        verify(subjectRepository).findByName(subjectName);
        verify(cache).remove("subject-1");
        verify(cache).remove("subject-Math");
        verify(cache).remove("avg-subject-1");
        verify(cache).remove("marks-100-1");
        verify(cache).remove("avg-student-100");
        verify(cache).remove("mark-200");
        verify(subjectRepository).deleteByName(subjectName);
    }

    @Test
    void deleteSubjectByName_ShouldThrowException_WhenSubjectNotFound() {
        String invalidName = "NonExistent";

        when(subjectRepository.findByName(invalidName)).thenReturn(Optional.empty());

        // Проверка исключения
        assertThrows(EntityNotFoundException.class, () ->
                subjectService.deleteSubjectByName(invalidName));

        // Убедимся, что кэш и delete не вызывались
        verify(cache, never()).remove(any());
        verify(subjectRepository, never()).deleteByName(any());
    }

    @Test
    void existsByName_ShouldReturnTrue_WhenSubjectExists() {
        String existingName = "Math";

        when(subjectRepository.existsByName(existingName)).thenReturn(true);

        boolean result = subjectService.existsByName(existingName);

        assertTrue(result);
        verify(subjectRepository).existsByName(existingName);
    }

    @Test
    void existsByName_ShouldReturnFalse_WhenSubjectDoesNotExist() {
        String nonExistingName = "Biology";

        when(subjectRepository.existsByName(nonExistingName)).thenReturn(false);

        boolean result = subjectService.existsByName(nonExistingName);

        assertFalse(result);
        verify(subjectRepository).existsByName(nonExistingName);
    }
}
