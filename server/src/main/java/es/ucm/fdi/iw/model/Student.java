package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A student.
 */
@Entity
public class Student extends Referenceable {
    @JsonIgnore
	private long id;
	@JsonIgnore
	private Instance instance;
	@JsonView(Views.Public.class)
	private String sid;
	@JsonView(Views.Public.class)
	private String firstName;
	@JsonView(Views.Public.class)
	private String lastName;
	@JsonIgnore
	private EClass eClass;
	@JsonView(Views.Public.class)
	@JsonSerialize(using = Referenceable.ListSerializer.class)
	private List<User> guardians = new ArrayList<>();

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

	@Column(unique=true)
	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
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

	@ManyToOne(targetEntity = EClass.class)
	@JsonIgnore
	public EClass getEClass() {
		return eClass;
	}

	public void setEClass(EClass eClass) {
		this.eClass = eClass;
	}

	@ManyToMany(targetEntity = User.class)
	public List<User> getGuardians() {
		return guardians;
	}

	public void setGuardians(List<User> guardians) {
		this.guardians = guardians;
	}

	@Override
	@Transient
	@JsonIgnore
	public String getRef() {
		return getSid();
	}

	@Transient
	@JsonView(Views.Public.class)
	public String getCid() {
		return getEClass().getCid();
	}
}
