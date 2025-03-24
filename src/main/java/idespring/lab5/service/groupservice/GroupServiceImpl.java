package idespring.lab5.service.groupservice;

import idespring.lab5.config.CacheConfig;
import idespring.lab5.exceptions.EntityNotFoundException;
import idespring.lab5.model.Group;
import idespring.lab5.model.Student;
import idespring.lab5.repository.grouprepo.GroupRepository;
import idespring.lab5.repository.studentrepo.StudentRepository;
import idespring.lab5.service.studservice.StudentServiceImpl;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {
    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final CacheConfig<String, Object> cache;
    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    private static final String GROUP_ID_PREFIX = "group_";
    private static final String GROUP_NAME_PREFIX = "name_";
    private static final String ALL_GROUPS_PREFIX = "allGroups";

    private final Set<String> groupCacheKeys = ConcurrentHashMap.newKeySet();
    private final StudentServiceImpl studentServiceImpl;

    @Autowired
    public GroupServiceImpl(GroupRepository groupRepository, StudentRepository studentRepository,
                            CacheConfig<String, Object> cache,
                            StudentServiceImpl studentServiceImpl) {
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
        this.cache = cache;
        this.studentServiceImpl = studentServiceImpl;
    }

    @Override
    public List<Group> readGroups(String namePattern, String sort) {
        String cacheKey = ALL_GROUPS_PREFIX
                + (namePattern != null ? namePattern : "") + (sort != null ? sort : "");

        groupCacheKeys.add(cacheKey);

        List<Group> cachedGroups = (List<Group>) cache.get(cacheKey);
        if (cachedGroups != null) {
            return cachedGroups;
        }

        final long start = System.nanoTime();
        logger.info("Fetching groups with namePattern: {}, sort: {}", namePattern, sort);

        List<Group> groups;
        if (namePattern != null) {
            groups = groupRepository.findByNameContaining(namePattern);
        } else if (sort != null && sort.equalsIgnoreCase("asc")) {
            groups = groupRepository.findAllByOrderByNameAsc();
        } else {
            groups = groupRepository.findAll();
        }

        cache.put(cacheKey, groups);
        long end = System.nanoTime();
        logger.info("Execution time for readGroups: {} ms", (end - start) / 1_000_000);
        return groups;
    }

    @Override
    public Group findById(Long id) {
        String cacheKey = GROUP_ID_PREFIX + id;

        groupCacheKeys.add(cacheKey);

        Group cachedGroup = (Group) cache.get(cacheKey);
        if (cachedGroup != null) {
            return cachedGroup;
        }

        long start = System.nanoTime();
        logger.info("Fetching group by ID: {}", id);

        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));

        cache.put(cacheKey, group);
        long end = System.nanoTime();
        logger.info("Execution time for findById: {} ms", (end - start) / 1_000_000);
        return group;
    }

    @Override
    public Group findByName(String name) {
        String cacheKey = GROUP_NAME_PREFIX + name;

        groupCacheKeys.add(cacheKey);

        Group cachedGroup = (Group) cache.get(cacheKey);
        if (cachedGroup != null) {
            return cachedGroup;
        }

        long start = System.nanoTime();
        logger.info("Fetching group by name: {}", name);

        Group group = groupRepository.findByName(name)
                .orElseThrow(() ->
                        new EntityNotFoundException("Group not found with name: " + name));

        cache.put(cacheKey, group);
        long end = System.nanoTime();
        logger.info("Execution time for findByName: {} ms", (end - start) / 1_000_000);
        return group;
    }

    @Override
    @Transactional
    public Group addGroup(String name, List<Integer> studentIds) {
        final long start = System.nanoTime();
        logger.info("Adding new group: {}", name);

        Group group = new Group(name);
        if (studentIds != null && !studentIds.isEmpty()) {
            List<Long> longStudentIds = studentIds.stream().map(Long::valueOf).toList();
            List<Student> students = studentRepository.findAllById(longStudentIds);

            if (students.size() != studentIds.size()) {
                Set<Long> foundStudentIds =
                        students.stream().map(Student::getId).collect(Collectors.toSet());
                List<Long> nonExistentIds = longStudentIds.stream()
                        .filter(id -> !foundStudentIds.contains(id)).toList();
                throw new
                        EntityNotFoundException("Студенты с ID " + nonExistentIds + " не найдены");
            }

            List<Student> studentsWithGroup = students.stream()
                    .filter(student -> student.getGroup() != null)
                    .toList();

            if (!studentsWithGroup.isEmpty()) {
                List<Long> studentsWithGroupIds = studentsWithGroup.stream()
                        .map(Student::getId)
                        .toList();
                throw new IllegalStateException("Студенты с ID " + studentsWithGroupIds
                        + " уже прикреплены к группе");
            }

            for (Student student : students) {
                student.setGroup(group);
            }
            group.setStudents(students);
        }

        Group savedGroup = groupRepository.save(group);

        String groupIdKey = GROUP_ID_PREFIX + savedGroup.getId();
        String groupNameKey = GROUP_NAME_PREFIX + savedGroup.getName();

        cache.put(groupIdKey, savedGroup);
        cache.put(groupNameKey, savedGroup);

        groupCacheKeys.add(groupIdKey);
        groupCacheKeys.add(groupNameKey);

        invalidateGroupListCaches();

        long end = System.nanoTime();
        logger.info("Execution time for addGroup: {} ms", (end - start) / 1_000_000);
        return savedGroup;
    }

    private Group findByIdInternal(Long id) {
        return groupRepository.findById(id).orElse(null);
    }

    private Group findByNameInternal(String name) {
        return groupRepository.findByName(name).orElse(null);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        logger.info("Deleting group with ID: {}", id);
        Group group = findByIdInternal(id);
        if (group == null) {
            throw new EntityNotFoundException("Group with ID " + id + " not found");
        }

        Set<Student> students = studentRepository.findByGroupId(id);

        clearStudentsCache(students);

        String groupIdKey = GROUP_ID_PREFIX + id;
        String groupNameKey = GROUP_NAME_PREFIX + group.getName();

        cache.remove(groupIdKey);
        cache.remove(groupNameKey);

        groupCacheKeys.remove(groupIdKey);
        groupCacheKeys.remove(groupNameKey);

        invalidateGroupListCaches();

        groupRepository.deleteById(id);
    }

    private void clearStudentsCache(Set<Student> students) {
        for (Student student : students) {
            studentServiceImpl.clearStudentCache(student.getId());
            studentServiceImpl.clearCachesByAge(student.getAge());
            studentServiceImpl.clearListCaches();
        }
        logger.info("Cleared student cache for {} students", students.size());
    }

    @Transactional
    public void deleteGroupByName(String name) {
        logger.info("Deleting group with name: {}", name);
        Group group = findByNameInternal(name);
        if (group == null) {
            throw new EntityNotFoundException("Group with name " + name + " not found");
        }

        Set<Student> students = studentRepository.findByGroupId(group.getId());

        clearStudentsCache(students);

        String groupIdKey = GROUP_ID_PREFIX + group.getId();
        String groupNameKey = GROUP_NAME_PREFIX + name;

        cache.remove(groupIdKey);
        cache.remove(groupNameKey);

        groupCacheKeys.remove(groupIdKey);
        groupCacheKeys.remove(groupNameKey);

        invalidateGroupListCaches();

        groupRepository.deleteByName(name);
    }

    private void invalidateGroupListCaches() {
        logger.debug("Invalidating all group list caches");

        Set<String> keysToRemove = new HashSet<>();

        for (String key : groupCacheKeys) {
            if (key.startsWith(ALL_GROUPS_PREFIX)) {
                keysToRemove.add(key);
                cache.remove(key);
            }
        }

        groupCacheKeys.removeAll(keysToRemove);
    }

    public void invalidateAllGroupCaches() {
        logger.info("Invalidating all group caches");

        Set<String> keysToRemove = new HashSet<>(groupCacheKeys);

        for (String key : keysToRemove) {
            cache.remove(key);
        }

        groupCacheKeys.clear();
    }

    @PreDestroy
    public void cleanup() {
        invalidateAllGroupCaches();
    }
}