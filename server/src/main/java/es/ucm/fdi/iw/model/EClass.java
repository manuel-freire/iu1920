package es.ucm.fdi.iw.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * A group of students, with one or more teachers.
 * EClass stands for ElementaryClass; "Class" by itself is a reserved word in Java
 * 
 * @author mfreire
 */
@Entity
public class EClass {
	private long id;
    @JsonView(Views.Public.class)
	private String eid;
    @JsonView(Views.Public.class)
	private List<User> teachers = new ArrayList<>();
    @JsonView(Views.Public.class)
	private List<Student> students = new ArrayList<>();
	private Instance instance;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = Instance.class)
	public Instance getInstance() {
		return instance;
	}

	public void setInstance(Instance instance) {
		this.instance = instance;
	}

	@Column(unique = true)
	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	@ManyToMany(targetEntity = User.class)
	public List<User> getTeachers() {
		return teachers;
	}

	public void setTeachers(List<User> teachers) {
		this.teachers = teachers;
	}

	@OneToMany(targetEntity = Student.class)
	public List<Student> getStudents() {
		return students;
	}

	public void setStudents(List<Student> students) {
		this.students = students;
	}
}
