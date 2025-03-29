package idespring.lab5.markservice;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.exceptions.SubjectNotAssignedException;
import idespring.lab5.model.Mark;
import idespring.lab5.model.Student;
import idespring.lab5.model.Subject;
import idespring.lab5.repository.markrepo.MarkRepository;
import idespring.lab5.repository.studentrepo.StudentRepository;
import idespring.lab5.repository.subjectrepo.SubjectRepository;
import idespring.lab5.service.markservice.MarkServiceImpl;
import idespring.lab5.service.studentsubjserv.StudentSubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MarkServiceImplTest {

    @Mock
    private MarkRepository markRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StudentSubjectService studentSubjectService;

    @Mock
    private CacheConfig<String, Object> cache;

    @InjectMocks
    private MarkServiceImpl markService;

    private Student student;
    private Subject subject;
    private Mark mark;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);

        subject = new Subject();
        subject.setId(2L);
        subject.setName("Math");

        mark = new Mark();
        mark.setId(3L);
        mark.setStudent(student);
        mark.setSubject(subject);
        mark.setValue(5);
    }

    @Test
    void testReadMarks_WithStudentAndSubject() {
        // Arrange
        when(cache.get(anyString())).thenReturn(null);
        when(studentSubjectService.getSubjectsByStudent(student.getId()))
                .thenReturn(List.of(subject));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));

        List<Mark> expectedMarks = List.of(mark);
        when(markRepository.findByStudentAndSubject(student, subject)).thenReturn(expectedMarks);

        // Act
        List<Mark> actualMarks = markService.readMarks(student.getId(), subject.getId());

        // Assert
        assertEquals(expectedMarks, actualMarks);
        verify(cache).put(anyString(), eq(expectedMarks));
    }

    @Test
    void testReadMarks_CachedResult() {
        // Arrange
        List<Mark> cachedMarks = List.of(mark);

        // Simulate that the student has the subject assigned
        when(studentSubjectService.getSubjectsByStudent(student.getId()))
                .thenReturn(List.of(subject));

        // Return cached marks
        when(cache.get(anyString())).thenReturn(cachedMarks);

        // Act
        List<Mark> actualMarks = markService.readMarks(student.getId(), subject.getId());

        // Assert
        assertEquals(cachedMarks, actualMarks);
        verify(cache, never()).put(anyString(), any());
        verify(markRepository, never()).findByStudentAndSubject(any(), any());
    }

    @Test
    void testReadMarks_SubjectNotAssigned() {
        // Arrange
        when(studentSubjectService.getSubjectsByStudent(student.getId()))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(SubjectNotAssignedException.class,
                () -> markService.readMarks(student.getId(), subject.getId()));
    }

    @Test
    void testReadMarks_StudentNotFound() {
        // Arrange
        when(studentSubjectService.getSubjectsByStudent(student.getId()))
                .thenReturn(List.of(subject));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> markService.readMarks(student.getId(), subject.getId()));
    }

    @Test
    void testReadMarks_AllCases() {
        // Test all combinations of null studentId and subjectId
        when(cache.get(anyString())).thenReturn(null);

        List<Mark> expectedMarks = new ArrayList<>();
        expectedMarks.add(mark);

        // Case 4: Both studentId and subjectId are provided
        when(studentSubjectService.getSubjectsByStudent(student.getId()))
                .thenReturn(List.of(subject));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(markRepository.findByStudentAndSubject(student, subject)).thenReturn(expectedMarks);
        List<Mark> specificMarks = markService.readMarks(student.getId(), subject.getId());
        assertEquals(expectedMarks, specificMarks);
    }

    private void testReadMarksCase(Long studentId, Long subjectId) {
        // Arrange
        when(cache.get(anyString())).thenReturn(null);

        List<Mark> expectedMarks = new ArrayList<>();
        expectedMarks.add(mark);

        if (studentId != null && subjectId != null) {
            when(studentSubjectService.getSubjectsByStudent(studentId))
                    .thenReturn(List.of(subject));
            when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
            when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
            when(markRepository.findByStudentAndSubject(student, subject)).thenReturn(expectedMarks);
        } else if (studentId != null) {
            when(markRepository.findByStudentId(studentId)).thenReturn(expectedMarks);
        } else if (subjectId != null) {
            when(markRepository.findBySubjectId(subjectId)).thenReturn(expectedMarks);
        } else {
            when(markRepository.findAll()).thenReturn(expectedMarks);
        }

        // Act
        List<Mark> actualMarks = markService.readMarks(studentId, subjectId);

        // Assert
        assertEquals(expectedMarks, actualMarks);
        verify(cache).put(anyString(), eq(expectedMarks));
    }

    @Test
    void testFindByValue() {
        // Arrange
        int markValue = 5;
        when(cache.get(anyString())).thenReturn(null);

        List<Mark> expectedMarks = List.of(mark);
        when(markRepository.findByValue(markValue)).thenReturn(expectedMarks);

        // Act
        List<Mark> actualMarks = markService.findByValue(markValue);

        // Assert
        assertEquals(expectedMarks, actualMarks);
        verify(cache).put(anyString(), eq(expectedMarks));
    }

    @Test
    void testFindByValue_Cached() {
        // Arrange
        int markValue = 5;
        List<Mark> cachedMarks = List.of(mark);
        when(cache.get(anyString())).thenReturn(cachedMarks);

        // Act
        List<Mark> actualMarks = markService.findByValue(markValue);

        // Assert
        assertEquals(cachedMarks, actualMarks);
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void testGetAverageMarkByStudentId() {
        // Arrange
        Long studentId = 1L;
        when(cache.get(anyString())).thenReturn(null);

        Double expectedAvg = 4.5;
        when(markRepository.getAverageMarkByStudentId(studentId)).thenReturn(expectedAvg);

        // Act
        Double actualAvg = markService.getAverageMarkByStudentId(studentId);

        // Assert
        assertEquals(expectedAvg, actualAvg);
        verify(cache).put(anyString(), eq(expectedAvg));
    }

    @Test
    void testGetAverageMarkBySubjectId() {
        // Arrange
        Long subjectId = 2L;
        when(cache.get(anyString())).thenReturn(null);

        Double expectedAvg = 4.5;
        when(markRepository.getAverageMarkBySubjectId(subjectId)).thenReturn(expectedAvg);

        // Act
        Double actualAvg = markService.getAverageMarkBySubjectId(subjectId);

        // Assert
        assertEquals(expectedAvg, actualAvg);
        verify(cache).put(anyString(), eq(expectedAvg));
    }

    @Test
    void testDeleteMarkSpecific() {
        // Arrange
        String subjectName = "Math";

        // Mock finding subject by name
        when(subjectRepository.findByName(subjectName))
                .thenReturn(Optional.of(subject));

        // Mock mark deletion
        when(markRepository.deleteMarkByStudentIdSubjectNameValueAndOptionalId(
                student.getId(),
                subjectName,
                mark.getValue(),
                mark.getId())
        ).thenReturn(1);

        // Act
        markService.deleteMarkSpecific(
                student.getId(),
                subjectName,
                mark.getValue(),
                mark.getId()
        );

        // Assert
        verify(markRepository).deleteMarkByStudentIdSubjectNameValueAndOptionalId(
                student.getId(),
                subjectName,
                mark.getValue(),
                mark.getId()
        );
    }

    @Test
    void testDeleteMarkSpecific_NotFound() {
        // Arrange
        String subjectName = "Math";
        when(subjectRepository.findByName(subjectName)).thenReturn(Optional.of(subject));

        when(markRepository.deleteMarkByStudentIdSubjectNameValueAndOptionalId(
                student.getId(), subjectName, mark.getValue(), mark.getId())).thenReturn(0);

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> markService.deleteMarkSpecific(student.getId(), subjectName, mark.getValue(), mark.getId()));
    }

    @Test
    void testAddMark() {
        // Arrange
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(studentSubjectService.getSubjectsByStudent(student.getId()))
                .thenReturn(List.of(subject));
        when(markRepository.save(mark)).thenReturn(mark);

        // Act
        Mark savedMark = markService.addMark(mark);

        // Assert
        assertEquals(mark, savedMark);
    }

    @Test
    void testAddMark_SubjectNotAssigned() {
        // Arrange
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(studentSubjectService.getSubjectsByStudent(student.getId()))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(SubjectNotAssignedException.class,
                () -> markService.addMark(mark));
    }

    @Test
    void testDeleteMark() {
        // Arrange
        when(markRepository.findById(mark.getId())).thenReturn(Optional.of(mark));

        // Act
        markService.deleteMark(mark.getId());

        // Assert
        verify(markRepository).deleteById(mark.getId());
    }

    @Test
    void testDeleteMark_NotFound() {
        // Arrange
        when(markRepository.findById(mark.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> markService.deleteMark(mark.getId()));
    }

    @Test
    void testClearCacheForSubject() {
        // Arrange
        List<Mark> subjectMarks = List.of(mark);
        when(markRepository.findBySubjectId(subject.getId())).thenReturn(subjectMarks);

        // Act
        markService.clearCacheForSubject(subject.getId());

        // Assert
        verify(cache).remove("marks-" + student.getId() + "-" + subject.getId());
        verify(cache).remove("marks-" + student.getId() + "-all");
        verify(cache).remove("avg-student-" + student.getId());
        verify(cache).remove("marks-all-" + subject.getId());
        verify(cache).remove("marks-all-all");
        verify(cache).remove("avg-subject-" + subject.getId());
        verify(cache).remove("mark-" + mark.getId());
        verify(cache).remove("value-" + mark.getValue());
    }

    @Test
    void testReadMarks_CachedMarksExists() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 2L;
        List<Mark> cachedMarks = new ArrayList<>();
        cachedMarks.add(new Mark());

        // Добавляем mock для проверки предметов студента
        Subject subject = new Subject();
        subject.setId(subjectId);
        when(studentSubjectService.getSubjectsByStudent(studentId))
                .thenReturn(List.of(subject));

        when(cache.get("marks-" + studentId + "-" + subjectId)).thenReturn(cachedMarks);

        // Act
        List<Mark> result = markService.readMarks(studentId, subjectId);

        // Assert
        assertEquals(cachedMarks, result);
        verify(cache).get("marks-" + studentId + "-" + subjectId);
        verify(markRepository, never()).findByStudentAndSubject(any(), any());
    }

    @Test
    void testReadMarks_StudentDoesNotHaveSubject() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 2L;

        when(studentSubjectService.getSubjectsByStudent(studentId)).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(SubjectNotAssignedException.class,
                () -> markService.readMarks(studentId, subjectId));
    }

    @Test
    void testReadMarks_BothStudentIdAndSubjectIdProvided_Success() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 2L;
        Student student = new Student();
        Subject subject = new Subject();
        subject.setId(subjectId);  // Устанавливаем ID предмета
        List<Mark> marks = new ArrayList<>();
        marks.add(new Mark());

        when(studentSubjectService.getSubjectsByStudent(studentId))
                .thenReturn(List.of(subject));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        when(markRepository.findByStudentAndSubject(student, subject)).thenReturn(marks);
        when(cache.get(anyString())).thenReturn(null);

        // Act
        List<Mark> result = markService.readMarks(studentId, subjectId);

        // Assert
        assertNotNull(result);
        assertEquals(marks, result);
        verify(cache).put(eq("marks-" + studentId + "-" + subjectId), eq(marks));
    }

    @Test
    void testReadMarks_StudentWithSubject_Success() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 999L;

        // Создаем mock Subject для студента
        Subject mockSubject = new Subject();
        mockSubject.setId(subjectId);

        // Создаем список оценок
        List<Mark> marks = List.of(new Mark());

        // Настройка mock-объектов
        when(studentSubjectService.getSubjectsByStudent(studentId))
                .thenReturn(List.of(mockSubject));

        when(cache.get(anyString())).thenReturn(null);

        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(new Student()));

        when(subjectRepository.findById(subjectId))
                .thenReturn(Optional.of(new Subject()));

        when(markRepository.findByStudentAndSubject(any(), any()))
                .thenReturn(marks);

        // Act
        List<Mark> result = markService.readMarks(studentId, subjectId);

        // Assert
        assertNotNull(result);
        assertEquals(marks, result);

        // Verify interactions
        verify(studentSubjectService).getSubjectsByStudent(studentId);
        verify(markRepository).findByStudentAndSubject(any(), any());
        verify(cache).put(eq("marks-" + studentId + "-" + subjectId), eq(marks));
    }

    @Test
    void testReadMarks_SubjectNotAssigned_ThrowsException() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 999L;

        // Создаем mock Subject, но с другим ID
        Subject mockSubject = new Subject();
        mockSubject.setId(888L);

        // Настройка mock-объектов
        when(studentSubjectService.getSubjectsByStudent(studentId))
                .thenReturn(List.of(mockSubject));

        // Act & Assert
        assertThrows(SubjectNotAssignedException.class, () -> {
            markService.readMarks(studentId, subjectId);
        });
    }

    @Test
    void testReadMarks_CachedResult_ReturnsCachedMarks() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 999L;
        String cacheKey = "marks-" + studentId + "-" + subjectId;

        // Создаем mock Subject для студента
        Subject mockSubject = new Subject();
        mockSubject.setId(subjectId);

        // Создаем список кэшированных оценок
        List<Mark> cachedMarks = List.of(new Mark());

        // Настройка mock-объектов
        // ВАЖНО: добавляем mock для studentSubjectService
        when(studentSubjectService.getSubjectsByStudent(studentId))
                .thenReturn(List.of(mockSubject));

        when(cache.get(cacheKey)).thenReturn(cachedMarks);

        // Act
        List<Mark> result = markService.readMarks(studentId, subjectId);

        // Assert
        assertNotNull(result);
        assertEquals(cachedMarks, result);

        // Verify interactions
        verify(cache).get(cacheKey);
        verifyNoInteractions(markRepository);
    }

    @Test
    void testReadMarks_BothStudentIdAndSubjectIdNull_Success() {
        // Arrange
        List<Mark> marks = new ArrayList<>();
        marks.add(new Mark());

        when(markRepository.findAll()).thenReturn(marks);
        when(cache.get(anyString())).thenReturn(null);

        // Act
        List<Mark> result = markService.readMarks(null, null);

        // Assert
        assertNotNull(result);
        assertEquals(marks, result);
        verify(cache).put(eq("marks-all-all"), eq(marks));
    }

    @Test
    void testReadMarks_SubjectNotFound() {
        // Arrange
        Long studentId = 1L;
        Long subjectId = 2L;

        Student student = new Student();
        Subject subject = new Subject();
        subject.setId(subjectId); // Убедитесь, что ID установлен

        when(studentSubjectService.getSubjectsByStudent(studentId))
                .thenReturn(List.of(subject));
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));
        when(subjectRepository.findById(subjectId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> markService.readMarks(studentId, subjectId));
    }

    @Test
    void testAddMark_StudentNotFound() {
        when(studentRepository.findById(student.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> markService.addMark(mark));
    }

    @Test
    void testAddMark_SubjectNotFound() {
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> markService.addMark(mark));
    }

    @Test
    void testGetAverageMarkBySubjectId_NoData() {
        when(cache.get(anyString())).thenReturn(null);
        when(markRepository.getAverageMarkBySubjectId(subject.getId())).thenReturn(null);

        Double avg = markService.getAverageMarkBySubjectId(subject.getId());

        assertNull(avg);
        verify(cache).put(anyString(), eq(null));
    }

    @Test
    void testClearCacheForStudent() {
        // Arrange
        List<Mark> studentMarks = List.of(mark);
        when(markRepository.findByStudentId(student.getId())).thenReturn(studentMarks);

        // Act
        markService.clearCacheForStudent(student.getId());

        // Assert
        verify(cache).remove("marks-" + student.getId() + "-" + subject.getId());
        verify(cache).remove("marks-all-" + subject.getId());
        verify(cache).remove("avg-subject-" + subject.getId());
        verify(cache).remove("marks-" + student.getId() + "-all");
        verify(cache).remove("marks-all-all");
        verify(cache).remove("avg-student-" + student.getId());
        verify(cache).remove("mark-" + mark.getId());
        verify(cache).remove("value-" + mark.getValue());
    }

    @Test
    void testGetAverageMarkByStudentId_CacheHit() {
        String cacheKey = "avg-student-1";
        when(cache.get(cacheKey)).thenReturn(4.5);

        Double avg = markService.getAverageMarkByStudentId(1L);

        assertEquals(4.5, avg);
        verify(markRepository, never()).getAverageMarkByStudentId(anyLong());
    }

    @Test
    void testGetAverageMarkByStudentId_CacheMiss() {
        String cacheKey = "avg-student-1";
        when(cache.get(cacheKey)).thenReturn(null);
        when(markRepository.getAverageMarkByStudentId(1L)).thenReturn(4.5);

        Double avg = markService.getAverageMarkByStudentId(1L);

        assertEquals(4.5, avg);
        verify(cache).put(cacheKey, 4.5);
    }

    @Test
    void testDeleteMark_ClearsCache() {
        when(markRepository.findById(1L)).thenReturn(Optional.of(mark));

        markService.deleteMark(1L);

        verify(markRepository).deleteById(1L);
    }

    @Test
    void testFindByValue_Success() {
        when(markRepository.findByValue(5)).thenReturn(List.of(mark));

        List<Mark> result = markService.findByValue(5);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

}