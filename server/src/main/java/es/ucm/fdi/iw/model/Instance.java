package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A separate instance of the application, as identified through an API key.
 * 
 * @author mfreire
 */
@Entity
@JsonIgnoreType
public class Instance {
	private long id;
	private List<EClass> classes = new ArrayList<>();
	private List<User> users= new ArrayList<>();
	private List<Student> students = new ArrayList<>();
	private List<Message> messages = new ArrayList<>();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	@OneToMany(targetEntity = EClass.class)
	@JoinColumn(name = "instance_id")
	public List<EClass> getClasses() {
		return classes;
	}

	public void setClasses(List<EClass> classes) {
		this.classes = classes;
	}

	@OneToMany(targetEntity = User.class)
	@JoinColumn(name = "instance_id")
	public List<User> getUsers() {
		return users;
	}

	public void setUsers(List<User> users) {
		this.users = users;
	}

	@OneToMany(targetEntity = Student.class)
	@JoinColumn(name = "instance_id")
	public List<Student> getStudents() {
		return students;
	}

	public void setStudents(List<Student> students) {
		this.students = students;
	}

	@OneToMany(targetEntity = Message.class)
	@JoinColumn(name = "instance_id")
	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}
}
