package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A student.
 */
@Entity
public class Student {
    @JsonView(Views.Public.class)    
	private long id;
	private Instance instance;
	@JsonView(Views.Public.class)
	private long eid;
	private String firstName;
	private String lastName;
	private EClass eClass;
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
	public long getEid() {
		return eid;
	}

	public void setEid(long eid) {
		this.eid = eid;
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
}
