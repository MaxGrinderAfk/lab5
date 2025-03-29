package idespring.lab5.groupservice;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.model.Group;
import idespring.lab5.model.Student;
import idespring.lab5.repository.grouprepo.GroupRepository;
import idespring.lab5.repository.studentrepo.StudentRepository;
import idespring.lab5.service.groupservice.GroupServiceImpl;
import idespring.lab5.service.studservice.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
public class GroupServiceImplTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CacheConfig<String, Object> cache;

    @Mock
    private StudentServiceImpl studentServiceImpl;

    @InjectMocks
    private GroupServiceImpl groupService;

    private Group testGroup;
    private List<Student> testStudents;

    @BeforeEach
    void setUp() {
        testGroup = new Group("Test Group");
        testGroup.setId(1L);

        testStudents = Arrays.asList(
                createStudent(1, 20),
                createStudent(2, 21),
                createStudent(3, 22)
        );
    }

    private Student createStudent(int id, int age) {
        Student student = new Student();
        student.setId((long) id);
        student.setAge(age);
        return student;
    }

    @Test
    void testFindById_CacheHit() {
        when(cache.get("group_1")).thenReturn(testGroup);
        Group result = groupService.findById(1L);
        assertNotNull(result);
        assertEquals("Test Group", result.getName());
        verify(groupRepository, never()).findById(anyLong());
    }

    @Test
    void testFindById_CacheMiss() {
        when(cache.get("group_1")).thenReturn(null);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        Group result = groupService.findById(1L);

        assertNotNull(result);
        assertEquals("Test Group", result.getName());
        verify(cache).put("group_1", testGroup);
    }

    @Test
    void testAddGroup() {
        when(groupRepository.save(any(Group.class))).thenReturn(testGroup);

        Group result = groupService.addGroup("Test Group", Collections.emptyList());

        assertNotNull(result);
        assertEquals("Test Group", result.getName());
        verify(cache).put("group_1", result);
        verify(cache).put("name_Test Group", result);
    }

    @Test
    void testDeleteGroup_Success() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));
        when(studentRepository.findByGroupId(1L)).thenReturn(Collections.emptySet());

        groupService.deleteGroup(1L);

        verify(cache).remove("group_1");
        verify(cache).remove("name_Test Group");
        verify(groupRepository).deleteById(1L);
    }

    @Test
    void readGroups_withNamePattern_shouldReturnFilteredGroups() {
        String namePattern = "Test";
        List<Group> expectedGroups = Collections.singletonList(testGroup);

        when(cache.get(anyString())).thenReturn(null);
        when(groupRepository.findByNameContaining(namePattern)).thenReturn(expectedGroups);

        List<Group> result = groupService.readGroups(namePattern, null);

        assertEquals(expectedGroups, result);
        verify(cache).put(anyString(), eq(expectedGroups));
    }

    @Test
    void readGroups_withSortAsc_shouldReturnSortedGroups() {
        List<Group> expectedGroups = Collections.singletonList(testGroup);

        when(cache.get(anyString())).thenReturn(null);
        when(groupRepository.findAllByOrderByNameAsc()).thenReturn(expectedGroups);

        List<Group> result = groupService.readGroups(null, "asc");

        assertEquals(expectedGroups, result);
        verify(cache).put(anyString(), eq(expectedGroups));
    }

    @Test
    void readGroups_default_shouldReturnAllGroups() {
        List<Group> expectedGroups = Collections.singletonList(testGroup);

        when(cache.get(anyString())).thenReturn(null);
        when(groupRepository.findAll()).thenReturn(expectedGroups);

        List<Group> result = groupService.readGroups(null, null);

        assertEquals(expectedGroups, result);
        verify(cache).put(anyString(), eq(expectedGroups));
    }

    @Test
    void readGroups_fromCache_shouldReturnCachedGroups() {
        List<Group> cachedGroups = Collections.singletonList(testGroup);

        when(cache.get(anyString())).thenReturn(cachedGroups);

        List<Group> result = groupService.readGroups(null, null);

        assertEquals(cachedGroups, result);
        verify(groupRepository, never()).findAll();
    }

    @Test
    void findById_existingGroup_shouldReturnGroup() {
        when(cache.get(anyString())).thenReturn(null);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(testGroup));

        Group result = groupService.findById(1L);

        assertEquals(testGroup, result);
        verify(cache).put(anyString(), eq(testGroup));
    }

    @Test
    void findById_nonExistingGroup_shouldThrowException() {
        when(cache.get(anyString())).thenReturn(null);
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> groupService.findById(1L));
    }

    @Test
    void findByName_existingGroup_shouldReturnGroup() {
        when(cache.get(anyString())).thenReturn(null);
        when(groupRepository.findByName("Test Group")).thenReturn(Optional.of(testGroup));

        Group result = groupService.findByName("Test Group");

        assertEquals(testGroup, result);
        verify(cache).put(anyString(), eq(testGroup));
    }

    @Test
    void findByName_nonExistingGroup_shouldThrowException() {
        when(cache.get(anyString())).thenReturn(null);
        when(groupRepository.findByName("Test Group")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> groupService.findByName("Test Group"));
    }

    @Test
    void addGroup_withStudents_shouldCreateGroupAndAttachStudents() {
        List<Integer> studentIds = Arrays.asList(1, 2, 3);
        List<Long> longStudentIds = studentIds.stream().map(Long::valueOf).collect(Collectors.toList());

        when(studentRepository.findAllById(longStudentIds)).thenReturn(testStudents);
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group savedGroup = invocation.getArgument(0);
            savedGroup.setId(1L);
            return savedGroup;
        });

        Group result = groupService.addGroup("New Group", studentIds);

        assertNotNull(result);
        assertEquals(3, result.getStudents().size());
        verify(groupRepository).save(any(Group.class));
        verify(cache, times(2)).put(anyString(), eq(result));
    }

    @Test
    void addGroup_withNonExistentStudents_shouldThrowException() {
        List<Integer> studentIds = Arrays.asList(1, 2, 3);
        List<Long> longStudentIds = studentIds.stream().map(Long::valueOf).collect(Collectors.toList());

        when(studentRepository.findAllById(longStudentIds)).thenReturn(Collections.singletonList(testStudents.getFirst()));

        assertThrows(EntityNotFoundException.class, () -> groupService.addGroup("New Group", studentIds));
    }

    @Test
    void addGroup_withStudentsAlreadyInGroup_shouldThrowException() {
        List<Integer> studentIds = Arrays.asList(1, 2, 3);
        List<Long> longStudentIds = studentIds.stream().map(Long::valueOf).collect(Collectors.toList());

        List<Student> studentsWithGroup = testStudents.stream()
                .peek(student -> student.setGroup(new Group("Existing Group")))
                .collect(Collectors.toList());

        when(studentRepository.findAllById(longStudentIds)).thenReturn(studentsWithGroup);

        assertThrows(IllegalStateException.class, () -> groupService.addGroup("New Group", studentIds));
    }

    @Test
    void deleteGroup_nonExistentGroup_shouldThrowException() {
        when(groupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> groupService.deleteGroup(1L));
    }

    @Test
    void deleteGroupByName_nonExistentGroup_shouldThrowException() {
        when(groupRepository.findByName("Group to Delete")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> groupService.deleteGroupByName("Group to Delete"));
    }

    @Test
    void invalidateGroupListCaches_shouldRemoveAllListCaches() {
        Set<String> groupCacheKeys = new HashSet<>(Arrays.asList(
                "allGroups",
                "allGroups_Test",
                "group_1",
                "name_TestGroup"
        ));

        ReflectionTestUtils.setField(groupService, "groupCacheKeys", groupCacheKeys);

        groupService.invalidateGroupListCaches();

        verify(cache, times(2)).remove(argThat(key -> key.startsWith("allGroups")));
        assertTrue(groupCacheKeys.stream().noneMatch(key -> key.startsWith("allGroups")));
    }

    @Test
    void testDeleteGroup_NotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> groupService.deleteGroup(99L));
    }


    @Test
    void testInvalidateAllGroupCaches() {
        // Добавляем фиктивные ключи в set перед вызовом метода
        groupService.groupCacheKeys.add("group_1");
        groupService.groupCacheKeys.add("name_Test Group");

        groupService.invalidateAllGroupCaches();

        verify(cache, times(2)).remove(anyString()); // Проверяем, что cache.remove был вызван дважды
        assertTrue(groupService.groupCacheKeys.isEmpty()); // Проверяем, что set очистился
    }


    @Test
    void testFindById_NotFound() {
        when(cache.get("group_99")).thenReturn(null);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> groupService.findById(99L));
    }
}