package es.ucm.fdi.iw.model;

import java.io.IOException;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Arrays;
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
 * A user; can be an Admin, a Teacher, or a Guardian.
 *
 * Users can log in and send each other messages.
 *
 * @author mfreire
 */
@Entity
public class User {

	public enum Role {
		ADMIN,
		TEACHER,
		GUARDIAN
	}
	private long id;

	private String password;
	@JsonView(Views.Public.class)
	private String roles; // split by ',' to separate roles
	private byte enabled;

	public boolean hasRole(Role role) {
		String roleName = role.name();
		return Arrays.stream(roles.split(","))
				.anyMatch(r -> r.equals(roleName));
	}
	
	// application-specific fields
	@JsonView(Views.Public.class)
	private String uid;
	private Instance instance;
	@JsonView(Views.Public.class)
	private String firstName;
	@JsonView(Views.Public.class)
	private String lastName;
	@JsonView(Views.Public.class)
	private String tels;

	// for teachers, represents where the teacher teaches
	// for guardians, what they are guarding
	@JsonView(Views.Public.class)
	@JsonSerialize(using = EClass.RefsSerializer.class)
	private List<EClass> classes = new ArrayList<>();

	@JsonIgnore
	private List<UMessage> sent = new ArrayList<>();
	@JsonIgnore
	private List<UMessage> received = new ArrayList<>();
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}	

	@Column(nullable = false)
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	public byte getEnabled() {
		return enabled;
	}

	public void setEnabled(byte enabled) {
		this.enabled = enabled;
	}

	@ManyToOne(targetEntity = Instance.class)
	public Instance getInstance() {
		return instance;
	}

	public void setInstance(Instance instance) {
		this.instance = instance;
	}

	@Column(unique=true)
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@ManyToMany(targetEntity = EClass.class)
	public List<EClass> getClasses() {
		return classes;
	}

	public void setClasses(List<EClass> classes) {
		this.classes = classes;
	}

	public String getTels() {
		return tels;
	}

	public void setTels(String tels) {
		this.tels = tels;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@ManyToMany(targetEntity = UMessage.class)
	public List<UMessage> getSent() {
		return sent;
	}

	public void setSent(List<UMessage> sent) {
		this.sent = sent;
	}

	@ManyToMany(targetEntity = UMessage.class)
	public List<UMessage> getReceived() {
		return received;
	}

	public void setReceived(List<UMessage> received) {
		this.received = received;
	}

	public static class RefsSerializer extends JsonSerializer< List<User> > {
		@Override
		public void serialize(List<User> os, JsonGenerator g, SerializerProvider serializerProvider)
				throws IOException, JsonProcessingException {
			g.writeStartArray();
			for (User o : os) g.writeObject(o.getUid());
			g.writeEndArray();
		}
	}
}
