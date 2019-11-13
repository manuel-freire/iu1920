package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;
import java.io.IOException;
import java.util.List;

/**
 * A group of students, with one or more teachers.
 * EClass stands for ElementaryClass; "Class" by itself is a reserved word in Java
 * 
 * @author mfreire
 */
@Entity
public class UMessage {
	@JsonIgnore
	private long id;
	@JsonIgnore
	private User user;
	@JsonIgnore
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

	@ManyToOne(targetEntity = User.class)
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@ManyToOne(targetEntity = Message.class)
	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	@JsonSerialize(using = Referenceable.StringToListSerializer.class)
	public String getLabels() {
		return labels;
	}

	public void setLabels(String labels) {
		this.labels = labels;
	}

	@Transient
	public String getMsgid() {
		return message.getMid();
	}

	@Transient
	public String getDate() {
		return message.getDate();
	}

	@Transient
	@JsonSerialize(using = Referenceable.RefSerializer.class)
	public User getFrom() {
		return message.getFrom();
	}

	@Transient
	@JsonSerialize(using = Referenceable.ListSerializer.class)
	public List<User> getTo() {
		return message.getTo();
	}

	@Transient
	public String getBody() {
		return message.getBody();
	}

	@Transient
	public String getSubject() {
		return message.getSubject();
	}
}
