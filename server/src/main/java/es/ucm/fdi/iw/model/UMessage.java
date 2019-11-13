package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

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

	public static class RefsSerializer extends JsonSerializer<List<UMessage>> {
		@Override
		public void serialize(List<UMessage> os, JsonGenerator g, SerializerProvider serializerProvider)
				throws IOException, JsonProcessingException {
			g.writeStartArray();
			for (UMessage o : os) g.writeObject(o.getMessage().getMid());
			g.writeEndArray();
		}
	}
}