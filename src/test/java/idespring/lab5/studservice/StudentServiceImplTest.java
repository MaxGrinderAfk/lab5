package idespring.lab5.studservice;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.model.Group;
import idespring.lab5.model.Student;
import idespring.lab5.repository.studentrepo.StudentRepository;
import idespring.lab5.service.studservice.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.DefaultArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CacheConfig<String, Object> cache;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student student;
    private final Long studentId = 1L;
    private final int age = 20;
    private final String sort = "asc";
    private final String cacheKey = age + "-" + sort + "-" + studentId;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(studentId);
        student.setAge(age);
    }

    @Test
    void readStudents_WhenIdNotNullAndCacheMiss_ShouldFetchFromRepository() {
        // Передаем только id, age и sort == null
        Long id = 1L;
        Integer age = null;
        String sort = null;

        // Формируем правильный ключ кэша: "null-null-1"
        String expectedCacheKey = age + "-" + sort + "-" + id;

        when(cache.get(expectedCacheKey)).thenReturn(null);
        when(studentRepository.findById(id)).thenReturn(Optional.of(student));

        List<Student> result = studentService.readStudents(age, sort, id);

        assertThat(result).containsExactly(student);
        verify(cache).put(expectedCacheKey, Collections.singletonList(student));
    }

    @Test
    void readStudents_WhenIdNotNullAndCacheHit_ShouldReturnFromCache() {
        Long id = 1L;
        Integer age = null;
        String sort = null;
        String expectedCacheKey = age + "-" + sort + "-" + id;

        when(cache.get(expectedCacheKey)).thenReturn(Collections.singletonList(student));

        List<Student> result = studentService.readStudents(age, sort, id);

        assertThat(result).containsExactly(student);
        verify(studentRepository, never()).findById(any());
    }

    @Test
    void readStudents_WhenStudentNotFound_ShouldThrowException() {
        when(cache.get(any())).thenReturn(null);
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.readStudents(null, null, studentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void findByGroupId_WhenCacheMiss_ShouldFetchFromRepository() {
        Long groupId = 1L;
        String cacheKey = "group-" + groupId;
        List<Student> students = List.of(student);

        when(cache.get(cacheKey)).thenReturn(null);
        when(studentRepository.findByGroupId(groupId)).thenReturn(Set.of(student));

        List<Student> result = studentService.findByGroupId(groupId);

        assertThat(result).isEqualTo(students);
        verify(cache).put(cacheKey, students);
    }

    @Test
    void findByGroupId_WhenCacheHit_ShouldReturnFromCache() {
        Long groupId = 1L;
        String cacheKey = "group-" + groupId;
        when(cache.get(cacheKey)).thenReturn(List.of(student));

        List<Student> result = studentService.findByGroupId(groupId);

        assertThat(result).containsExactly(student);
        verify(studentRepository, never()).findByGroupId(any());
    }

    @Test
    void findById_WhenCacheMiss_ShouldFetchFromRepository() {
        when(cache.get(studentId.toString())).thenReturn(null);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        Student result = studentService.findById(studentId);

        assertThat(result).isEqualTo(student);
        verify(cache).put(studentId.toString(), student);
    }

    @Test
    void findById_WhenStudentNotExists_ShouldThrowException() {
        when(cache.get(any())).thenReturn(null);
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.findById(studentId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addStudent_ShouldClearRelatedCaches() {
        // 1. Подготовка данных
        Student newStudent = new Student();
        newStudent.setAge(25);

        Group group = new Group("Group A");
        group.setId(1L);
        newStudent.setGroup(group);

        Student savedStudent = new Student();
        savedStudent.setId(1L);
        savedStudent.setAge(25);
        savedStudent.setGroup(group);

        // 2. Настройка моков
        when(studentRepository.save(any())).thenReturn(savedStudent);

        // 3. Вызов метода
        studentService.addStudent(newStudent);

        // 4. Проверки
        verify(cache).put(eq("1"), eq(savedStudent));

        // Проверка очистки по возрасту (все возможные ключи)
        verify(cache).remove("25-null-null");
        verify(cache).remove("25-asc-null");
        verify(cache).remove("25-desc-null");

        // Проверка очистки группы
        verify(cache).remove("group-1");
        verify(cache).remove("students-in-group-1");

        // Проверка очистки списков
        verify(cache).remove("null-null-null");
        verify(cache).remove("null-asc-null");
        verify(cache).remove("null-desc-null");
    }

    @Test
    void updateStudent_ShouldClearCacheForPreviousAge() {
        // 1. Подготовка данных
        Student existingStudent = new Student();
        existingStudent.setId(studentId);
        existingStudent.setAge(20); // Предыдущий возраст
        existingStudent.setGroup(new Group("Group A"));

        // 2. Настройка моков
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(existingStudent));

        // 3. Вызов метода
        studentService.updateStudent("New Name", 21, studentId);

        // 4. Проверки
        verify(cache).remove(String.valueOf(studentId)); // Очистка кэша студента
        verify(cache).remove("20-null-null"); // Предыдущий возраст без сортировки
        verify(cache).remove("20-asc-null"); // Предыдущий возраст с сортировкой
        verify(cache).remove("20-desc-null");

        // Проверка очистки общих списков
        verify(cache).remove("null-null-null");
        verify(cache).remove("null-asc-null");
        verify(cache).remove("null-desc-null");
    }

    @Test
    void deleteStudent_ShouldClearAllRelatedCaches() {
        // 1. Подготовка данных
        Long studentId = 1L; // Явная инициализация
        int studentAge = 20;
        Long groupId = 1L;

        Group group = new Group("Group");
        group.setId(groupId);

        Student student = new Student();
        student.setId(studentId);
        student.setAge(studentAge);
        student.setGroup(group);

        // 2. Настройка стабов с использованием "ленивого" подхода
        doReturn(Optional.of(student))
                .when(studentRepository)
                .findById(eq(1L)); // Явное указание аргумента

        // 3. Вызов тестируемого метода
        studentService.deleteStudent(studentId);

        // 4. Проверки кэша
        verify(cache, times(1)).remove(eq(String.valueOf(studentId)));
        verify(cache, times(1)).remove(eq("group-" + groupId));
        verify(cache, times(1)).remove(eq("students-in-group-" + groupId));

        // Проверка очистки по возрасту
        verify(cache, times(1)).remove(eq("20-null-null"));
        verify(cache, times(1)).remove(eq("20-asc-null"));
        verify(cache, times(1)).remove(eq("20-desc-null"));

        // Проверка очистки списков
        verify(cache, times(1)).remove(eq("null-null-null"));
        verify(cache, times(1)).remove(eq("null-asc-null"));
        verify(cache, times(1)).remove(eq("null-desc-null"));

        // 5. Проверка вызовов репозитория
        verify(studentRepository, times(1)).findById(eq(1L));
        verify(studentRepository, times(1)).delete(any(Student.class));
    }

    @Test
    void readStudents_WhenAgeAndSortNotNullAndCacheMiss_ShouldFetchFromRepository() {
        // Подготовка
        Integer age = 20;
        String sort = "asc";
        String cacheKey = age + "-" + sort + "-null";

        List<Student> expectedStudents = List.of(new Student(), new Student());

        when(cache.get(cacheKey)).thenReturn(null);
        when(studentRepository.findByAgeAndSortByName(age, sort)).thenReturn(expectedStudents);

        // Вызов
        List<Student> result = studentService.readStudents(age, sort, null);

        // Проверки
        assertThat(result).isEqualTo(expectedStudents);
        verify(cache).put(cacheKey, expectedStudents);
        verify(studentRepository).findByAgeAndSortByName(age, sort);
    }

    @Test
    void readStudents_WhenAgeAndSortNotNullAndCacheHit_ShouldReturnFromCache() {
        Integer age = 20;
        String sort = "asc";
        String cacheKey = age + "-" + sort + "-null";

        List<Student> cachedStudents = List.of(new Student());

        when(cache.get(cacheKey)).thenReturn(cachedStudents);

        List<Student> result = studentService.readStudents(age, sort, null);

        assertThat(result).isSameAs(cachedStudents);
        verify(studentRepository, never()).findByAgeAndSortByName(anyInt(), anyString());
    }

    @Test
    void readStudents_WhenAgeNotNullAndSortNull_ShouldUseFindByAge() {
        Integer age = 25;
        String cacheKey = age + "-null-null";

        Set<Student> repoStudents = Set.of(new Student(), new Student());
        when(cache.get(cacheKey)).thenReturn(null);
        when(studentRepository.findByAge(age)).thenReturn(repoStudents);

        List<Student> result = studentService.readStudents(age, null, null);

        assertThat(result).containsExactlyElementsOf(repoStudents);
        verify(cache).put(cacheKey, result);
    }

    @Test
    void readStudents_WhenSortNotNullAndAgeNull_ShouldUseSortByName() {
        String sort = "desc";
        String cacheKey = "null-" + sort + "-null";

        List<Student> expected = List.of(new Student());
        when(cache.get(cacheKey)).thenReturn(null);
        when(studentRepository.sortByName(sort)).thenReturn(expected);

        List<Student> result = studentService.readStudents(null, sort, null);

        assertThat(result).isSameAs(expected);
        verify(cache).put(cacheKey, expected);
    }

    @Test
    void readStudents_WhenAllParamsNull_ShouldUseFindAll() {
        String cacheKey = "null-null-null";

        List<Student> allStudents = List.of(new Student(), new Student());
        when(cache.get(cacheKey)).thenReturn(null);
        when(studentRepository.findAll()).thenReturn(allStudents);

        List<Student> result = studentService.readStudents(null, null, null);

        assertThat(result).isEqualTo(allStudents);
        verify(cache).put(cacheKey, allStudents);
    }

    @ParameterizedTest
    @CsvSource({
            "20,   asc,    1,      '20-asc-1'",
            "25,   '',     2,      '25-null-2'",
            "'',   desc,   3,      'null-desc-3'",
            "30,   asc,    'NULL', '30-asc-null'"
    })
    void readStudents_ShouldGenerateCorrectCacheKeys(
            @ConvertWith(NullableIntegerConverter.class) Integer age,
            @ConvertWith(NullableStringConverter.class) String sort,
            @ConvertWith(NullableLongConverter.class) Long id,
            String expectedKey
    ) {
        // Настройка моков
        if (id != null) {
            Student student = new Student();
            student.setId(id);
            when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        }

        // Вызов метода
        studentService.readStudents(age, sort, id);

        // Проверка
        verify(cache).get(expectedKey);
    }

    public static class NullableIntegerConverter implements ArgumentConverter {
        @Override
        public Integer convert(Object source, ParameterContext context) {
            if (source == null || "NULL".equals(source) || "".equals(source)) {
                return null;
            }
            return Integer.parseInt(source.toString());
        }
    }

    public static class NullableStringConverter implements ArgumentConverter {
        @Override
        public String convert(Object source, ParameterContext context) {
            if (source == null || "NULL".equals(source) || "".equals(source)) {
                return null;
            }
            return source.toString();
        }
    }

    public static class NullableLongConverter implements ArgumentConverter {
        @Override
        public Long convert(Object source, ParameterContext context) {
            if (source == null || "NULL".equals(source) || "".equals(source)) {
                return null;
            }
            return Long.parseLong(source.toString());
        }
    }

    @Test
    void readStudents_WhenAllParamsNull_ShouldCallFindAll() {
        studentService.readStudents(null, null, null);

        verify(cache).get("null-null-null");
        verify(studentRepository).findAll();
    }

    @Test
    void readStudents_WithAgeOnly_ShouldCallFindByAge() {
        studentService.readStudents(25, null, null);

        verify(cache).get("25-null-null");
        verify(studentRepository).findByAge(25);
    }

    @Test
    void clearRelatedCaches_WhenStudentIsNull_ShouldDoNothing() {
        // Вызов метода с null
        studentService.clearRelatedCaches(null);

        // Проверяем, что ни один метод очистки не вызывался
        verifyNoInteractions(cache);
        verifyNoInteractions(studentRepository);
    }

    @Test
    void clearRelatedCaches_WhenStudentWithGroup_ShouldClearAllCaches() {
        // Подготовка данных
        Student student = new Student();
        student.setAge(25);

        Group group = new Group("Group A");
        group.setId(1L);
        student.setGroup(group);

        // Вызов метода
        studentService.clearRelatedCaches(student);

        // Проверки
        verify(cache).remove("25-null-null");
        verify(cache).remove("25-asc-null");
        verify(cache).remove("25-desc-null");
        verify(cache).remove("group-1");
        verify(cache).remove("students-in-group-1");
        verify(cache).remove("null-null-null");
        verify(cache).remove("null-asc-null");
        verify(cache).remove("null-desc-null");
    }
}