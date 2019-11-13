package es.ucm.fdi.iw.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A login token to be included in all requests.
 * THIS IS ONLY DEMO CODE - IT IS NOT SPECIALLY SECURE. USE OAUTH FOR REAL STUFF
 *
 * @author mfreire
 */
@Entity
public class Token {
	private long id;
	private String key;
	private User user;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	@Column(unique=true)
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

    @ManyToOne(targetEntity = User.class)
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
