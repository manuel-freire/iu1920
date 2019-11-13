package es.ucm.fdi.iw.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Much like an e-mail, but without attachments.
 * 
 * @author mfreire
 */
@Entity
@JsonIgnoreType
public class Message extends Referenceable {
	private long id;
	private String mid;
	private Instance instance;
    private User from;
	private List<User> to = new ArrayList<>();
    private Message parent;
	private String date;
    private String subject;
    private String body;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	@Column(nullable = false, unique = true)
	public String getMid() {
		return mid;
	}

	public void setMid(String mid) {
		this.mid = mid;
	}

	@ManyToOne(targetEntity = Instance.class)
	public Instance getInstance() {
		return instance;
	}

	public void setInstance(Instance instance) {
		this.instance = instance;
	}

	@ManyToOne(targetEntity = User.class)
	public User getFrom() {
		return from;
	}

	public void setFrom(User from) {
		this.from = from;
	}

	@ManyToMany(targetEntity = User.class)
	public List<User> getTo() {
		return to;
	}

	public void setTo(List<User> to) {
		this.to = to;
	}

	@ManyToOne(targetEntity = Message.class)
	public Message getParent() {
		return parent;
	}

	public void setParent(Message parent) {
		this.parent = parent;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	@Override
	@Transient
	@JsonIgnore
	public String getRef() {
		return getMid();
	}
}
