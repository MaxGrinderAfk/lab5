package idespring.lab5.model;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(schema = "studentmanagement", name = "students")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @NotEmpty
    private String name;

    private int age;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "groupid")
    @JsonIdentityReference(alwaysAsId = true)
    private Group group;

    @JsonIdentityReference(alwaysAsId = true)
    @ManyToMany(cascade = {CascadeType.DETACH,
            CascadeType.REFRESH, CascadeType.MERGE,
            CascadeType.PERSIST},
             fetch = FetchType.EAGER)
    @JoinTable (
            name = "student_subject",
            joinColumns = @JoinColumn(name = "studentid"),
            inverseJoinColumns = @JoinColumn(name = "subjectid")
    )
    private Set<Subject> subjects = new HashSet<>();

    @JsonIdentityReference(alwaysAsId = true)
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Mark> marks = new HashSet<>();

    public Student() {}

    public Student(Long id) {
        this.id = id;
    }

    public Student(String name, int age, Group group, Set<Subject> subjects) {
        this.name = name;
        this.age = age;
        this.group = group;
        this.subjects = subjects;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(Set<Subject> subjects) {
        this.subjects = subjects;
    }

    public Set<Mark> getMarks() {
        return marks;
    }

    public void setMarks(Set<Mark> marks) {
        this.marks = marks;
    }

    public int getAge() {
        return age;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void addMark(Mark mark) {
        mark.setStudent(this);
        this.marks.add(mark);
    }

    public void setAge(int age) {
        this.age = age;
    }
}