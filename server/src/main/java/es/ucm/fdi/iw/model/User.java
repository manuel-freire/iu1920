package es.ucm.fdi.iw.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * A user; can be an Admin, a Teacher, or a Guardian.
 *
 * Users can log in and send each other messages.
 *
 * @author mfreire
 */
@Entity
public class User {

	enum Role {
		ADMIN,
		TEACHER,
		GUARDIAN
	}

	@JsonView(Views.Public.class)
	private long id;

	@JsonView(Views.Public.class)
	private String password;
	private String roles; // split by ',' to separate roles
	private byte enabled;

	public boolean hasRole(Role role) {
		String roleName = role.name();
		return Arrays.stream(roles.split(","))
				.anyMatch(r -> r.equals(roleName));
	}
	
	// application-specific fields
	private String eid;
	private Instance instance;
	private String firstName;
	private String lastName;
	private String tels;

	// for teachers, represents where the teacher teaches
	// for guardians, what they are guarding
	private List<EClass> classes = new ArrayList<>();

	private List<UMessage> sent = new ArrayList<>();
	private List<UMessage> received = new ArrayList<>();
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}	

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
	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
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
}
