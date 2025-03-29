package idespring.lab5.controller.groupcontroller;

import idespring.lab5.exceptions.ValidationException;
import idespring.lab5.model.Group;
import idespring.lab5.service.groupservice.GroupService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/groups")
public class GroupController {
    private final GroupService groupService;
    private static final String STUDIDERR = "studentIds";

    @Autowired
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<Group>>
        createGroupsBulk(@RequestBody List<Map<String, Object>> requests) {
        List<Group> createdGroups = requests.stream()
                .map(request -> {
                    if (!request.containsKey("name") || !(request.get("name") instanceof String)
                            || ((String) request.get("name")).isBlank()) {
                        throw new ValidationException(
                                "Поле 'name' обязательно и не может быть пустым");
                    }

                    if (!request.containsKey(STUDIDERR)
                            || !(request.get(STUDIDERR) instanceof List)) {
                        throw new ValidationException(
                                "Поле 'studentIds' обязательно и должно быть списком чисел");
                    }

                    String name = (String) request.get("name");
                    List<Integer> studentIds = (List<Integer>) request.get(STUDIDERR);
                    return groupService.addGroup(name, studentIds);
                })
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroups);
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Map<String, Object> request) {
        if (!request.containsKey("name") || !(request.get("name") instanceof String)
                || ((String) request.get("name")).isBlank()) {
            throw new ValidationException("Поле 'name' обязательно и не может быть пустым");
        }

        if (!request.containsKey(STUDIDERR) || !(request.get(STUDIDERR) instanceof List)) {
            throw new ValidationException(
                    "Поле 'studentIds' обязательно и должно быть списком чисел");
        }

        String name = (String) request.get("name");
        List<Integer> studentIds = (List<Integer>) request.get(STUDIDERR);

        Group group = groupService.addGroup(name, studentIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping
    public ResponseEntity<List<Group>> getGroups(
            @RequestParam(required = false) String namePattern,
            @RequestParam(required = false) String sort) {
        List<Group> groups = groupService.readGroups(namePattern, sort);
        return !groups.isEmpty()
                ? ResponseEntity.ok(groups)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<Group> getGroupById(@Positive @NotNull @PathVariable Long groupId) {
        Group group = groupService.findById(groupId);
        if (group == null) {
            throw new EntityNotFoundException("Группа с ID " + groupId + " не найдена");
        }
        return ResponseEntity.ok(group);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Group> getGroupByName(@NotEmpty @PathVariable String name) {
        Group group = groupService.findByName(name);
        if (group == null) {
            throw new EntityNotFoundException("Группа с именем '" + name + "' не найдена");
        }
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@Positive @NotNull @PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/name/{name}")
    public ResponseEntity<Void> deleteGroupByName(@NotEmpty @PathVariable String name) {
        groupService.deleteGroupByName(name);
        return ResponseEntity.noContent().build();
    }
}
