package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.persistence.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A user; can be an Admin, a Teacher, or a Guardian.
 *
 * Users can log in and send each other messages.
 *
 * @author mfreire
 */
@Entity
public class User extends Referenceable {

	private static BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	public enum Role {
		ADMIN,
		TEACHER,
		GUARDIAN,
		CLASS		// used to send to entire classes at once
	}
	@JsonIgnore
	private long id;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;
	@JsonIgnore
	private String roles; // split by ',' to separate roles
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private byte enabled;

	public boolean hasRole(Role role) {
		String roleName = role.name();
		return Arrays.stream(roles.split(","))
				.anyMatch(r -> r.equals(roleName));
	}
	
	// application-specific fields
	@JsonIgnore
	private Instance instance;

	@JsonView(Views.Public.class)
	@JsonSerialize(using = Referenceable.ListSerializer.class)
	private List<Student> students = new ArrayList<>();

	@JsonView(Views.Public.class)
	private String uid;
	@JsonView(Views.Public.class)
	@JsonProperty("first_name")
	private String firstName;
	@JsonView(Views.Public.class)
	@JsonProperty("last_name")
	private String lastName;
	@JsonIgnore
	private String telephones;

	// for teachers, represents where the teacher teaches
	// for guardians, what they are guarding
	@JsonView(Views.Public.class)
	@JsonSerialize(using = Referenceable.ListSerializer.class)
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

	// call only with encoded passwords - NEVER STORE PLAINTEXT PASSWORDS
	public void setPassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	public boolean passwordMatches(String rawPassword) {
		return encoder.matches(rawPassword, this.password);
	}

	public static String encodePassword(String rawPassword) {
		return encoder.encode(rawPassword);
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

	@ManyToMany(targetEntity = Student.class)
	public List<Student> getStudents() {
		return students;
	}

	public void setStudents(List<Student> students) {
		this.students = students;
	}

	public String getTelephones() {
		return telephones;
	}

	public void setTelephones(String tels) {
		this.telephones = tels;
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

	@ManyToMany(targetEntity = UMessage.class, cascade = CascadeType.ALL)
	public List<UMessage> getSent() {
		return sent;
	}

	public void setSent(List<UMessage> sent) {
		this.sent = sent;
	}

	@ManyToMany(targetEntity = UMessage.class, cascade = CascadeType.ALL)
	public List<UMessage> getReceived() {
		return received;
	}

	public void setReceived(List<UMessage> received) {
		this.received = received;
	}

	@Override
	@Transient
	@JsonIgnore
	public String getRef() {
		return getUid();
	}

	@Transient
	public String getType() {
		return roles.toLowerCase();
	}
	public void setType(String type) {
		this.roles = ""+Role.valueOf(type.toUpperCase());
	}

	@Transient
	@JsonView(Views.Public.class)
	@JsonSerialize(using = Referenceable.StringToListSerializer.class)
	public String getTels() {
		return getTelephones();
	}
}
