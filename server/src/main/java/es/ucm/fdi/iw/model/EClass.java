package es.ucm.fdi.iw.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A group of students, with one or more teachers.
 * EClass stands for ElementaryClass; "Class" by itself is a reserved word in Java
 * 
 * @author mfreire
 */
@Entity
public class EClass extends Referenceable {
	private long id;
    @JsonView(Views.Public.class)
	private String cid;
	@JsonSerialize(using = Referenceable.ListSerializer.class)
	private List<User> teachers = new ArrayList<>();
	@JsonSerialize(using = Referenceable.ListSerializer.class)
	private List<Student> students = new ArrayList<>();
	@JsonIgnore
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

	@Column(unique = true, nullable = false)
	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	@ManyToMany(targetEntity = User.class)
	public List<User> getTeachers() {
		return teachers;
	}

	public void setTeachers(List<User> teachers) {
		this.teachers = teachers;
	}

	@OneToMany(targetEntity = Student.class, cascade = CascadeType.ALL)
	public List<Student> getStudents() {
		return students;
	}

	public void setStudents(List<Student> students) {
		this.students = students;
	}

	@Override
	@Transient
	@JsonIgnore
	public String getRef() {
		return getCid();
	}
}
