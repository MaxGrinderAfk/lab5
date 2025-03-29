package idespring.lab5.studentsubjservice;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.model.Student;
import idespring.lab5.model.Subject;
import idespring.lab5.repository.studentrepo.StudentRepository;
import idespring.lab5.repository.subjectrepo.SubjectRepository;
import idespring.lab5.service.studentsubjserv.StudentSubjectServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentSubjectServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    private CacheConfig<String, Object> cache;

    @InjectMocks
    private StudentSubjectServiceImpl studentSubjectService;

    private static final Long STUDENT_ID = 1L;
    private static final Long SUBJECT_ID = 2L;
    private static final String STUDENT_ERR = "Student not found";
    private static final String SUBJECT_ERR = "Subject not found";

    @BeforeEach
    void setUp() {
        cache = new CacheConfig<>(60000, 100); // 1 минута, 100 элементов

        ReflectionTestUtils.setField(studentSubjectService, "cache", cache);
    }

    @Test
    void findStudentWithSubjects_StudentNotFound_ShouldThrowException() {
        Long studentId = 1L;

        when(cache.get("student-with-subjects-" + studentId)).thenReturn(null);
        when(studentRepository.findByIdWithSubjects(studentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> studentSubjectService.findStudentWithSubjects(studentId));
    }

    @Test
    void getStudentsBySubject_WhenCacheIsEmpty_ShouldFetchFromRepository() {
        // Arrange
        Subject mockSubject = new Subject();
        Set<Student> mockStudents = new HashSet<>();
        mockStudents.add(new Student());
        mockSubject.setStudents(mockStudents);

        when(subjectRepository.findByIdWithStudents(SUBJECT_ID))
                .thenReturn(Optional.of(mockSubject));

        // Act
        Set<Student> result = studentSubjectService.getStudentsBySubject(SUBJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(mockStudents, result);

        // Verify caching
        Object cachedResult = cache.get("students-" + SUBJECT_ID);
        assertNotNull(cachedResult);
        assertEquals(mockStudents, cachedResult);

        verify(subjectRepository).findByIdWithStudents(SUBJECT_ID);
    }

    @Test
    void getStudentsBySubject_WhenRepositoryReturnsEmpty_ShouldThrowException() {
        // Arrange
        when(subjectRepository.findByIdWithStudents(SUBJECT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentSubjectService.getStudentsBySubject(SUBJECT_ID)
        );

        assertEquals(SUBJECT_ERR, exception.getMessage());

        // Verify no caching of non-existent subject
        assertNull(cache.get("students-" + SUBJECT_ID));

        verify(subjectRepository).findByIdWithStudents(SUBJECT_ID);
    }

    @Test
    void findStudentWithSubjects_WhenCacheIsEmpty_ShouldFetchFromRepository() {
        // Arrange
        Student mockStudent = new Student();
        mockStudent.setId(STUDENT_ID);

        when(studentRepository.findByIdWithSubjects(STUDENT_ID))
                .thenReturn(Optional.of(mockStudent));

        // Act
        Student result = studentSubjectService.findStudentWithSubjects(STUDENT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(mockStudent, result);

        // Verify caching
        Object cachedResult = cache.get("student-with-subjects-" + STUDENT_ID);
        assertNotNull(cachedResult);
        assertEquals(mockStudent, cachedResult);

        verify(studentRepository).findByIdWithSubjects(STUDENT_ID);
    }

    @Test
    void findStudentWithSubjects_WhenRepositoryReturnsEmpty_ShouldThrowException() {
        // Arrange
        when(studentRepository.findByIdWithSubjects(STUDENT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentSubjectService.findStudentWithSubjects(STUDENT_ID)
        );

        assertEquals(STUDENT_ERR, exception.getMessage());

        // Verify no caching of non-existent student
        assertNull(cache.get("student-with-subjects-" + STUDENT_ID));

        verify(studentRepository).findByIdWithSubjects(STUDENT_ID);
    }

    @Test
    void findSubjectWithStudents_WhenCacheIsEmpty_ShouldFetchFromRepository() {
        // Arrange
        Subject mockSubject = new Subject();
        mockSubject.setId(SUBJECT_ID);

        when(subjectRepository.findByIdWithStudents(SUBJECT_ID))
                .thenReturn(Optional.of(mockSubject));

        // Act
        Subject result = studentSubjectService.findSubjectWithStudents(SUBJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(mockSubject, result);

        // Verify caching
        Object cachedResult = cache.get("subject-with-students-" + SUBJECT_ID);
        assertNotNull(cachedResult);
        assertEquals(mockSubject, cachedResult);

        verify(subjectRepository).findByIdWithStudents(SUBJECT_ID);
    }

    @Test
    void findSubjectWithStudents_WhenRepositoryReturnsEmpty_ShouldThrowException() {
        // Arrange
        when(subjectRepository.findByIdWithStudents(SUBJECT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        idespring.lab5.exceptions.EntityNotFoundException exception = assertThrows(
                idespring.lab5.exceptions.EntityNotFoundException.class,
                () -> studentSubjectService.findSubjectWithStudents(SUBJECT_ID)
        );

        assertEquals(SUBJECT_ERR, exception.getMessage());

        // Verify no caching of non-existent subject
        assertNull(cache.get("subject-with-students-" + SUBJECT_ID));

        verify(subjectRepository).findByIdWithStudents(SUBJECT_ID);
    }

    @Test
    void getStudentsBySubject_WhenCacheContainsValue_ShouldReturnCachedValue() {
        // Arrange
        Set<Student> mockStudents = new HashSet<>();
        mockStudents.add(new Student());

        // Manually put in cache
        cache.put("students-" + SUBJECT_ID, mockStudents);

        // Act
        Set<Student> result = studentSubjectService.getStudentsBySubject(SUBJECT_ID);

        // Assert
        assertNotNull(result);
        assertEquals(mockStudents, result);

        // Verify no repository call
        verify(subjectRepository, never()).findByIdWithStudents(SUBJECT_ID);
    }

    @Test
    void getStudentsBySubject_WhenSubjectNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange
        when(subjectRepository.findByIdWithStudents(SUBJECT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentSubjectService.getStudentsBySubject(SUBJECT_ID)
        );

        // Дополнительные проверки
        assertEquals(SUBJECT_ERR, exception.getMessage());
        verify(subjectRepository).findByIdWithStudents(SUBJECT_ID);

        // Проверяем, что кэш пустой
        assertNull(cache.get("students-" + SUBJECT_ID));
    }

    @Test
    void findStudentWithSubjects_WhenStudentNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange
        when(studentRepository.findByIdWithSubjects(STUDENT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentSubjectService.findStudentWithSubjects(STUDENT_ID)
        );

        // Дополнительные проверки
        assertEquals(STUDENT_ERR, exception.getMessage());
        verify(studentRepository).findByIdWithSubjects(STUDENT_ID);

        // Проверяем, что кэш пустой
        assertNull(cache.get("student-with-subjects-" + STUDENT_ID));
    }

    @Test
    void findStudentWithSubjects_WhenCacheContainsNonExistentStudent_ShouldThrowEntityNotFoundException() {
        // Arrange
        when(studentRepository.findByIdWithSubjects(STUDENT_ID))
                .thenReturn(Optional.empty());

        // Пытаемся найти несуществующего студента
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentSubjectService.findStudentWithSubjects(STUDENT_ID)
        );

        // Проверки
        assertEquals(STUDENT_ERR, exception.getMessage());
        verify(studentRepository).findByIdWithSubjects(STUDENT_ID);
    }

    @AfterEach
    void tearDown() {
        // Очищаем кэш после каждого теста
        cache.shutdown();
    }

    @Test
    void addSubjectToStudent_WhenStudentAndSubjectExist_ShouldAddSubjectAndClearCache() {
        // Arrange
        Student student = new Student();
        student.setId(STUDENT_ID);
        Subject subject = new Subject();
        subject.setId(SUBJECT_ID);

        // Предварительно кладем объекты в кэш
        cache.put("subjects-" + STUDENT_ID, Collections.singletonList(subject));
        cache.put("student-with-subjects-" + STUDENT_ID, student);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));

        // Act
        studentSubjectService.addSubjectToStudent(STUDENT_ID, SUBJECT_ID);

        // Assert
        verify(studentRepository).addSubject(STUDENT_ID, SUBJECT_ID);

        // Проверяем, что кэш очищен
        assertNull(cache.get("subjects-" + STUDENT_ID));
        assertNull(cache.get("students-" + SUBJECT_ID));
        assertNull(cache.get("student-with-subjects-" + STUDENT_ID));
        assertNull(cache.get("subject-with-students-" + SUBJECT_ID));
    }

    @Test
    void getSubjectsByStudent_WhenCacheEmpty_ShouldFetchAndCacheSubjects() {
        // Arrange
        List<Subject> subjects = Collections.singletonList(new Subject());

        when(subjectRepository.findByStudentId(STUDENT_ID)).thenReturn(subjects);

        // Act
        List<Subject> result = studentSubjectService.getSubjectsByStudent(STUDENT_ID);

        // Assert
        assertEquals(subjects, result);

        // Проверяем, что объект добавлен в кэш
        assertEquals(subjects, cache.get("subjects-" + STUDENT_ID));
    }

    @Test
    void getSubjectsByStudent_WhenCacheNotEmpty_ShouldReturnCachedSubjects() {
        // Arrange
        List<Subject> subjects = Collections.singletonList(new Subject());

        // Предварительно кладем объекты в кэш
        cache.put("subjects-" + STUDENT_ID, subjects);

        // Act
        List<Subject> result = studentSubjectService.getSubjectsByStudent(STUDENT_ID);

        // Assert
        assertEquals(subjects, result);
        verify(subjectRepository, never()).findByStudentId(STUDENT_ID);
    }

    @Test
    void getStudentsBySubject_WhenCacheEmpty_ShouldFetchAndCacheStudents() {
        // Arrange
        Subject subject = new Subject();
        Set<Student> students = new HashSet<>(Collections.singletonList(new Student()));
        subject.setStudents(students);

        when(subjectRepository.findByIdWithStudents(SUBJECT_ID)).thenReturn(Optional.of(subject));

        // Act
        Set<Student> result = studentSubjectService.getStudentsBySubject(SUBJECT_ID);

        // Assert
        assertEquals(students, result);

        // Проверяем, что объект добавлен в кэш
        assertEquals(students, cache.get("students-" + SUBJECT_ID));
    }

    @Test
    void findStudentWithSubjects_WhenCacheEmpty_ShouldFetchAndCacheStudent() {
        // Arrange
        Student student = new Student();
        student.setId(STUDENT_ID);

        when(studentRepository.findByIdWithSubjects(STUDENT_ID)).thenReturn(Optional.of(student));

        // Act
        Student result = studentSubjectService.findStudentWithSubjects(STUDENT_ID);

        // Assert
        assertEquals(student, result);

        // Проверяем, что объект добавлен в кэш
        assertEquals(student, cache.get("student-with-subjects-" + STUDENT_ID));
    }

    @Test
    void findSubjectWithStudents_WhenCacheEmpty_ShouldFetchAndCacheSubject() {
        // Arrange
        Subject subject = new Subject();
        subject.setId(SUBJECT_ID);

        when(subjectRepository.findByIdWithStudents(SUBJECT_ID)).thenReturn(Optional.of(subject));

        // Act
        Subject result = studentSubjectService.findSubjectWithStudents(SUBJECT_ID);

        // Assert
        assertEquals(subject, result);

        // Проверяем, что объект добавлен в кэш
        assertEquals(subject, cache.get("subject-with-students-" + SUBJECT_ID));
    }

    @Test
    void cache_ShouldRespectMaxSizeAndExpiration() throws InterruptedException {
        // Создаем кэш с маленьким размером и коротким временем жизни
        CacheConfig<String, String> smallCache = new CacheConfig<>(100, 3);

        // Добавляем больше элементов, чем позволяет размер кэша
        smallCache.put("key1", "value1");
        smallCache.put("key2", "value2");
        smallCache.put("key3", "value3");
        smallCache.put("key4", "value4");

        // Проверяем, что размер кэша не превышает максимальный
        assertEquals(3, smallCache.size());

        // Проверяем время жизни
        TimeUnit.MILLISECONDS.sleep(150);

        // Проверяем, что кэш очистился
        assertEquals(0, smallCache.size());

        smallCache.shutdown();
    }

    @Test
    void removeSubjectFromStudent_ShouldClearCorrespondingCaches() {
        // Arrange
        Student student = new Student();
        student.setId(STUDENT_ID);
        Subject subject = new Subject();
        subject.setId(SUBJECT_ID);

        // Предварительно кладем объекты в кэш
        cache.put("subjects-" + STUDENT_ID, Collections.singletonList(subject));
        cache.put("students-" + SUBJECT_ID, Collections.singleton(student));
        cache.put("student-with-subjects-" + STUDENT_ID, student);
        cache.put("subject-with-students-" + SUBJECT_ID, subject);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));

        // Act
        studentSubjectService.removeSubjectFromStudent(STUDENT_ID, SUBJECT_ID);

        // Assert
        verify(studentRepository).removeSubject(STUDENT_ID, SUBJECT_ID);

        // Проверяем, что все связанные кэши очищены
        assertNull(cache.get("subjects-" + STUDENT_ID));
        assertNull(cache.get("students-" + SUBJECT_ID));
        assertNull(cache.get("student-with-subjects-" + STUDENT_ID));
        assertNull(cache.get("subject-with-students-" + SUBJECT_ID));
    }

    @Test
    void removeSubjectFromStudent_WhenStudentNotFound_ShouldThrowException() {
        // Arrange
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentSubjectService.removeSubjectFromStudent(STUDENT_ID, SUBJECT_ID)
        );
        assertEquals(STUDENT_ERR, exception.getMessage());
        verify(studentRepository).findById(STUDENT_ID);
    }

    @Test
    void removeSubjectFromStudent_WhenSubjectNotFound_ShouldThrowException() {
        // Arrange
        Student student = new Student();
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentSubjectService.removeSubjectFromStudent(STUDENT_ID, SUBJECT_ID)
        );
        assertEquals(SUBJECT_ERR, exception.getMessage());
        verify(subjectRepository).findById(SUBJECT_ID);
    }

    @Test
    void shouldFetchFromDatabaseWhenNotInCache() {
        Long studentId = 1L;
        cache.put("student-with-subjects-" + STUDENT_ID, null);
        when(studentRepository.findByIdWithSubjects(studentId))
                .thenReturn(Optional.of(new Student()));

        Student student = studentSubjectService.findStudentWithSubjects(studentId);

        assertNotNull(student);
        verify(studentRepository).findByIdWithSubjects(studentId);
    }

    @Test
    void shouldThrowExceptionWhenStudentNotFound() {
        Long studentId = 99L;
        cache.put("student-with-subjects-" + STUDENT_ID, null);
        when(studentRepository.findByIdWithSubjects(studentId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> studentSubjectService.findStudentWithSubjects(studentId));
    }

    @Test
    void shouldFetchFromCache() {
        Long studentId = 1L;
        Student cachedStudent = new Student();
        cache.put("student-with-subjects-" + STUDENT_ID, cachedStudent);

        Student student = studentSubjectService.findStudentWithSubjects(studentId);

        assertEquals(cachedStudent, student);
        verify(studentRepository, never()).findByIdWithSubjects(anyLong());
    }


}
