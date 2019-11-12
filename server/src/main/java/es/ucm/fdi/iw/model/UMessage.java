package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;

/**
 * A group of students, with one or more teachers.
 * EClass stands for ElementaryClass; "Class" by itself is a reserved word in Java
 * 
 * @author mfreire
 */
@Entity
public class UMessage {
	@JsonView(Views.Public.class)
	private long id;
	private Instance instance;
	private User user;
	private Message message;
	private String labels;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@ManyToOne(targetEntity=User.class)
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@ManyToOne(targetEntity=Message.class)
	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public String getLabels() {
		return labels;
	}

	public void setLabels(String labels) {
		this.labels = labels;
	}
}