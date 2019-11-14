package es.ucm.fdi.iw.control;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.HtmlStreamRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Rest controller for managing a class.
 * Asking questions, removing them, and voting are the main tasks. 
 * 
 * @author mfreire
 */
@RestController
@RequestMapping("api")
public class ApiController {
	
	private static final Logger log = LogManager.getLogger(ApiController.class);
	
	@Autowired 
	private EntityManager entityManager;
	
	@Autowired
	private IwSocketHandler iwSocketHandler;
	
	@Autowired
	private LocalData localData;

	@ExceptionHandler(ApiException.class)
    public ResponseEntity handleException(ApiException e) {
        // log exception
        return ResponseEntity
                .status(e instanceof ApiAuthException ?
						HttpStatus.FORBIDDEN :
						HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }

	@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Invalid request")  // 401
	public static class ApiException extends RuntimeException {
	     public ApiException(String text, Throwable cause) {
	    	 super(text, cause);
	    	 if (cause != null) {
	    		 log.warn(text, cause);
	    	 } else {
	    		 log.info(text);
	    	 }
	     }
	}

	@ResponseStatus(value=HttpStatus.FORBIDDEN, reason="Not authorized")  // 403
	public static class ApiAuthException extends ApiException {
		public ApiAuthException(String text) {
			super(text, null);
			log.info(text);
		}
	}

	private Token resolveTokenOrBail(String tokenKey) {
		List<Token> results = entityManager.createQuery(
				"from Token where key = :key", Token.class)
				.setParameter("key", tokenKey)
				.getResultList();
		if ( ! results.isEmpty()) {
			return results.get(0);
		} else {
			throw new ApiException("Invalid token", null);
		}
	}

	/**
	 * Requires that certain fields exist in a JsonNode, complains otherwise
	 */
	private static void requireFields(JsonNode source, String ... fieldNames) {
		List<String> missing = new ArrayList<>();
		for (String fieldName : fieldNames) {
			if (! source.has(fieldName)) {
				missing.add(fieldName);
			}
		}
		if ( ! missing.isEmpty()) {
			throw new ApiException("Expected to find values for  " + Strings.join(missing, ','), null);
		}
	}

	/**
	 * Tries to take and validate a field from a JsonNode
	 */
	private static void check(JsonNode source, String fieldName, Predicate<String> validTest, String ifInvalid, Consumer<String> ifValid) {
		if (source.has(fieldName)) {
			String s = source.get(fieldName).asText();
			if (validTest.test(s)) {
				ifValid.accept(s);
			} else {
				throw new ApiException("While validating " + fieldName + ": " + ifInvalid, null);
			}
		}
	}

	/**
	 * Generates random tokens. From https://stackoverflow.com/a/44227131/15472
	 * @param byteLength
	 * @return
	 */
	public static String generateRandomBase64Token(int byteLength) {
		SecureRandom secureRandom = new SecureRandom();
		byte[] token = new byte[byteLength];
		secureRandom.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token); //base64 encoding
	}

	/**
	 * Logs out, essentially invalidating an existing token.
	 */
	@PostMapping("/{token}/logout")
	@Transactional
	public void logout(
			@PathVariable String token) {
		log.info(token + "/logout");
		Token t = resolveTokenOrBail(token);
		entityManager.remove(t);
	}

	/**
	 * Initializes the application, adding 10 instances. Can only be done once
	 * (when no instances exist), and prints out credentials for an admin for each instance
	 */
	@GetMapping("/initialize")
	@Transactional
	@ResponseBody
	public String initialize() {
		log.info("/initializing");
		List<Instance> results = entityManager
				.createQuery("from Instance", Instance.class)
				.getResultList();
		if ( ! results.isEmpty()) {
			throw new ApiException("The application has already been initialized. Go away", null);
		}
		StringBuilder sb = new StringBuilder("{\n");
		for (int i=1; i<= 10; i++) {
			Instance instance = new Instance();
			User user = new User();
			user.setInstance(instance);
			user.setEnabled((byte)1);
			user.setUid(generateRandomBase64Token(4));
			user.setFirstName("Admin_g" + i);
			user.setLastName("Apellido1 Apellido2");
			user.setRoles("" + User.Role.ADMIN);
			user.setTelephones("123-456-789");
			String pass = generateRandomBase64Token(4);
			user.setPassword(User.encodePassword(pass));
			instance.getUsers().add(user);
			sb.append("\"" + user.getFirstName() + "\": {" +
					"\"uid\": \"" + user.getUid() + "\"," +
					"\"password\": \"" + pass + "\"}");
			entityManager.persist(instance);
			entityManager.persist(user);

			sb.append(i < 10 ? ",\n": "\n");
		}
		sb.append("}");
		log.info(sb.toString());
		return sb.toString();
	}

	/**
	 * Requests a token from the system. Provides a user to do so, for which only the
	 * password and uid are looked at
	 * @param data attempting to log in.
	 * @throws JsonProcessingException
	 */
    @PostMapping("/login")
	@JsonView(Views.Public.class)
    @Transactional
    public GlobalState login(
            @RequestBody JsonNode data) throws JsonProcessingException {
        log.info("/login/" + new ObjectMapper().writeValueAsString(data));

        requireFields(data, "uid", "password");
        String uid = data.get("uid").asText();
        String pass = data.get("password").asText();

		List<User> results = entityManager.createQuery(
				"from User where uid = :uid", User.class)
				.setParameter("uid", uid)
				.getResultList();
		// only expecting one, because uid is unique
		User u = results.isEmpty() ? null : results.get(0);

		if (u == null
				// we do not allow "class" users to log in - they are more of a hack
				|| u.hasRole(User.Role.CLASS)
				|| ! u.passwordMatches(pass)) {
			throw new ApiAuthException("Invalid uid or password");
		}

		Token token = new Token();
		token.setUser(u);
		token.setKey(generateRandomBase64Token(16));
		entityManager.persist(token);
		return new GlobalState(token);
    }

	@PostMapping("/{token}/addclass")
	@Transactional
	public GlobalState addClass(
			@PathVariable String token,
			@RequestBody JsonNode data) throws JsonProcessingException {
		log.info(token + "/addclass/" + new ObjectMapper().writeValueAsString(data));
		Token t = resolveTokenOrBail(token);
		User u = t.getUser();
		if ( ! u.hasRole(User.Role.ADMIN)) {
			throw new ApiException("Only admins can add classes", null);
		}
		EClass ec = new EClass();
		ec.setCid(data.get("cid").asText());
		ec.setInstance(u.getInstance());
		entityManager.persist(ec);
		User mailbox = new User();
		mailbox.setRoles("" + User.Role.CLASS);
		mailbox.setEnabled((byte)1);
		mailbox.setInstance(u.getInstance());
		mailbox.setUid(ec.getCid());
		mailbox.setPassword("!"); // must be not-null; but /login does not allow classes to log in
		entityManager.persist(mailbox);
        return new GlobalState(t);
	}

	private static <T extends Referenceable> T resolve(Collection<T> ts, String ref) {
    	if (ref == null) return null;
    	for (T t : ts) {
    		if (t.getRef().equals(ref)) return t;
		}
    	return null;
	}

	@PostMapping("/{token}/addstudent")
    @Transactional
    public GlobalState addStudent(
            @PathVariable String token,
            @RequestBody JsonNode data) throws JsonProcessingException {
        log.info(token + "/addstudent/" + new ObjectMapper().writeValueAsString(data));
		Token t = resolveTokenOrBail(token);
		User u = t.getUser();
		if ( ! u.hasRole(User.Role.ADMIN)) {
			throw new ApiException("Only admins can add students", null);
		}
		// create an empty Student, and start to copy stuff over
		Student result = new Student();

		// the student must specify a valid class
		EClass ec = resolve(u.getInstance().getClasses(), data.get("cid").asText());
		if (ec == null) {
			throw new ApiException("Invalid class ref " + data.get("cid"), null);
		} else {
			result.setEClass(ec);
			ec.getStudents().add(result);
		}
		// the student may specify existing guardians
		if (data.has("guardians")) {
			for (JsonNode n : data.get("guardians")) {
				User g = resolve(u.getInstance().getUsers(), n.asText());
				if (g == null) {
					throw new ApiException("Guardian with uid " + n.asText() + " not found", null);
				}
			}
		}
		// the student must specify first and last names
		String first = data.get("first_name").asText();
		String last = data.get("last_name").asText();
		if (first.isEmpty() || last.isEmpty()) {
			throw new ApiException("Missing first or last names", null);
		}
		result.setFirstName(first);
		result.setLastName(last);
		// and a unique student id
		String sid = data.get("sid").asText();
		if (resolve(u.getInstance().getStudents(), sid) != null) {
			throw new ApiException("Duplicate student id: " + sid, null);
		}
		result.setSid(sid);

		result.setInstance(u.getInstance());
		u.getInstance().getStudents().add(result);
		entityManager.persist(result);
		entityManager.flush(); // so returned state includes new student
		return new GlobalState(t);
    }

    private static boolean isValidPass(String pass) {
    	boolean hasUpper = Pattern.compile("[a-z]").matcher(pass).find();
    	boolean hasLower = Pattern.compile("[A-Z]").matcher(pass).find();
    	boolean hasDigits = Pattern.compile("[0-9]").matcher(pass).find();
    	boolean hasLength = pass.length() >= 5;
    	return hasUpper && hasLower && hasDigits && hasLength;
	}

  	public static final Function<HtmlStreamEventReceiver, HtmlSanitizer.Policy>
      POLICY_DEFINITION = new HtmlPolicyBuilder()
          .allowStandardUrlProtocols()
          // Allow title="..." on any element.
          .allowAttributes("title").globally()
          // Allow href="..." on <a> elements.
          .allowAttributes("href").onElements("a")
          // Defeat link spammers.
          .requireRelNofollowOnLinks()
          // The align attribute on <p> elements can have any value below.
          .allowAttributes("align")
              .matching(true, "center", "left", "right", "justify", "char")
              .onElements("p")
          // These elements are allowed.
          .allowElements(
              "a", "p", "div", "i", "b", "em", "blockquote", "tt", "strong",
              "br", "ul", "ol", "li")
          .toFactory();

    // see https://github.com/OWASP/java-html-sanitizer/blob/master/src/main/java/org/owasp/html/examples/SlashdotPolicyExample.java
	private static String whitelistHtml(String input) {
		StringBuilder out = new StringBuilder();
		HtmlSanitizer.sanitize(input, POLICY_DEFINITION
				.apply(HtmlStreamRenderer.create(out, e -> {})));
		return out.toString();
	}

    @PostMapping("/{token}/adduser")
    @Transactional
    public GlobalState addUser(
            @PathVariable String token,
            @RequestBody JsonNode data) throws JsonProcessingException {
        log.info(token + "/adduser/" + new ObjectMapper().writeValueAsString(data));
		Token t = resolveTokenOrBail(token);
		User u = t.getUser();
		if ( ! u.hasRole(User.Role.ADMIN)) {
			throw new ApiException("Only admins can add users", null);
		}
		// create an empty User, and start to copy stuff over
		User result = new User();

		requireFields(data, "type", "uid", "password", "tels", "first_name", "last_name");

		// the user must specify a valid type
		String type = data.get("type").asText();
		User.Role role = null;
		try {
			role = User.Role.valueOf(type.toUpperCase());
		} catch (Throwable e) {
			throw new ApiException("Bad or missing user type", e);
		}
		result.setRoles("" + role);

		// the uid must be present and not exist already
		String uid = data.get("uid").asText();
		if (resolve(u.getInstance().getUsers(), uid) != null) {
			throw new ApiException("Duplicate user id: " + uid, null);
		}
		result.setUid(uid);

		// users must specify first and last names
		String first = data.get("first_name").asText();
		String last = data.get("last_name").asText();
		if (first.isEmpty() || last.isEmpty()) {
			throw new ApiException("Missing first or last names", null);
		}
		result.setFirstName(first);
		result.setLastName(last);

		// users must specify telephones. These must be numeric, with dashes
		ArrayList<String> tels = new ArrayList<>();
		if (data.has("tels")) {
			for (JsonNode n : data.get("tels")) {
				String tel = n.asText();
				if ( ! tel.matches("[0-9]{3}-[0-9]{3}-[0-9]{3}")) {
					throw new ApiException(
							"Bad phone: expected ddd-ddd-ddd, with d a digit. Got " + tel, null);
				} else {
					tels.add(tel);
				}
			}
		}
		result.setTelephones(Strings.join(tels, ','));
		if (tels.isEmpty()) {
			throw new ApiException("Expected at least 1 telephone number", null);
		}

		// the user may specify existing class ids. This is only useful for teachers
		if (data.has("classes") && role.equals(User.Role.TEACHER)) {
			for (JsonNode n : data.get("classes")) {
				EClass ec = resolve(u.getInstance().getClasses(), n.asText());
				if (ec == null) {
					throw new ApiException("Class with cid " + n.asText() + " not found", null);
				} else {
					result.getClasses().add(ec);
					ec.getTeachers().add(result);
				}
			}
		}
		// the user may specify existing student ids. This is only useful for guardians
		if (data.has("students") && role.equals(User.Role.GUARDIAN)) {
			for (JsonNode n : data.get("students")) {
				Student s = resolve(u.getInstance().getStudents(), n.asText());
				if (s == null) {
					throw new ApiException("Student with sid " + n.asText() + " not found", null);
				} else {
					result.getStudents().add(s);
					s.getGuardians().add(result);
				}
			}
		}
		// users must specify a password
		if ( ! data.has("password")) {
			throw new ApiException("Invalid or missing password", null);
		} else {
			String pass = data.get("password").asText();
			if ( ! isValidPass(pass)) {
				throw new ApiException("Invalid or missing password", null);
			}
			result.setPassword(User.encodePassword(pass));
		}

		result.setInstance(u.getInstance());
		u.getInstance().getUsers().add(result);
		entityManager.persist(result);
		entityManager.flush(); // so returned state includes new user
		return new GlobalState(t);
    }

	@PostMapping("/{token}/set")
	@Transactional
	public GlobalState set(
			@PathVariable String token,
			@RequestBody JsonNode data) throws JsonProcessingException {
		log.info(token + "/set/" + new ObjectMapper().writeValueAsString(data));
		Token t = resolveTokenOrBail(token);
		User u = t.getUser();
		boolean found = false;

		if (data.has("msgid")) {
			// setting labels on a message
			Message m = resolve(u.getInstance().getMessages(), data.get("msgid").asText());
			if (m == null) {
				throw new ApiException("Bad message ID", null);
			}
			List<String> labels = new ArrayList<>();
			for (JsonNode ln : data.get("labels")) {
				labels.add(ln.asText());
			}
			String labelString = Strings.join(labels, ',');
			for (UMessage um : u.getSent()) {
				if (um.getMessage().equals(m)) {
					um.setLabels(labelString);
				}
			}
			for (UMessage um : u.getReceived()) {
				if (um.getMessage().equals(m)) {
					um.setLabels(labelString);
				}
			}
		}

		if (data.has("uid")) {
			// changing stuff on a user
			User v = resolve(u.getInstance().getUsers(), data.get("uid").asText());
			if (v == null) {
				throw new ApiException("Bad user ID", null);
			}
			if ( ! u.hasRole(User.Role.ADMIN) && v.getId() != u.getId()) {
				throw new ApiException("Only admin may change other users", null);
			}
			if ( ! u.hasRole(User.Role.ADMIN) && (
					data.has("type") ||
					data.has("classes") ||
					data.has("students"))) {
				throw new ApiException("Only admins may change user type, classes or students", null);
			}

			// normal stuff
			check(data, "first_name", d->!d.isEmpty(),
					"cannot be empty", d->v.setFirstName(d));
			check(data, "last_name", d->!d.isEmpty(),
					"cannot be empty", d->v.setLastName(d));
			check(data, "password", d->isValidPass(d),
					"invalid", d->v.setPassword(User.encodePassword(d)));
			if (data.has("tels")) {
				ArrayList<String> tels = new ArrayList<>();
				for (JsonNode n : data.get("tels")) {
					String tel = n.asText();
					if ( ! tel.matches("[0-9]{3}-[0-9]{3}-[0-9]{3}")) {
						throw new ApiException(
								"Bad phone: expected ddd-ddd-ddd, with d a digit. Got " + tel, null);
					} else {
						tels.add(tel);
					}
				}
				v.setTelephones(Strings.join(tels, ','));
				if (tels.isEmpty()) {
					throw new ApiException("Expected at least 1 telephone number", null);
				}
			}

			// the user may specify existing class ids. This is only useful for teachers
			if (data.has("classes") && v.equals(User.Role.TEACHER)) {
				List<EClass> old = new ArrayList<>(v.getClasses());

				for (JsonNode n : data.get("classes")) {
					EClass ec = resolve(u.getInstance().getClasses(), n.asText());
					if (ec == null) {
						throw new ApiException("Class with cid " + n.asText() + " not found", null);
					} else if ( ! old.contains(ec)){
						v.getClasses().add(ec);
						ec.getTeachers().add(v);
					}
				}
				// and now, remove from old
				for (EClass ec : old) {
					ec.getTeachers().remove(v);
					v.getClasses().remove(ec);
				}
			}
			// the user may specify existing student ids. This is only useful for guardians
			if (data.has("students") && v.equals(User.Role.GUARDIAN)) {
				List<Student> old = new ArrayList<>(v.getStudents());

				for (JsonNode n : data.get("students")) {
					Student s = resolve(u.getInstance().getStudents(), n.asText());
					if (s == null) {
						throw new ApiException("Student with sid " + n.asText() + " not found", null);
					} else if ( ! old.contains(s)) {
						v.getStudents().add(s);
						s.getGuardians().add(v);
					}
				}
				// and now, remove from old
				for (Student s : old) {
					s.getGuardians().remove(v);
					v.getStudents().remove(s);
				}
			}
		}

		if (data.has("sid")) {
			// a student. May be changing class, or guardians, or name and stuff
			Student v = resolve(u.getInstance().getStudents(), data.get("sid").asText());
			if (v == null) {
				throw new ApiException("Bad student ID", null);
			}
			if ( ! u.hasRole(User.Role.ADMIN)) {
				throw new ApiException("Only admins can alter students", null);
			}

			check(data, "first_name", d->!d.isEmpty(),
					"cannot be empty", d->v.setFirstName(d));
			check(data, "last_name", d->!d.isEmpty(),
					"cannot be empty", d->v.setLastName(d));

			if (data.has("cid")) {
				// switching class
				EClass ec = resolve(u.getInstance().getClasses(), data.get("cid").asText());
				if (ec == null) {
					throw new ApiException("Invalid class ref " + data.get("cid"), null);
				} else if (ec.getId() != v.getEClass().getId()){
					v.getEClass().getStudents().remove(v);
					v.setEClass(ec);
					ec.getStudents().add(v);
				}
			}

			if (data.has("guardians")) {
				// switching guardians
				List<User> old = new ArrayList<>(v.getGuardians());

				for (JsonNode n : data.get("guardians")) {
					User g = resolve(u.getInstance().getUsers(), n.asText());
					if (g == null) {
						throw new ApiException("Guardian with uid " + n.asText() + " not found", null);
					} else if ( ! old.contains(g)) {
						g.getStudents().add(v);
						v.getGuardians().add(g);
					}
				}
				// and now, remove from old
				for (User g : old) {
					g.getStudents().remove(v);
					v.getGuardians().remove(g);
				}
			}
		}

		if (data.has("cid")) {
			EClass v = resolve(u.getInstance().getClasses(), data.get("cid").asText());
			if (v == null) {
				throw new ApiException("Bad class ID", null);
			}
			if ( ! u.hasRole(User.Role.ADMIN)) {
				throw new ApiException("Only admins can alter classes", null);
			}

			if (data.has("teachers")) {
				// switching guardians
				List<User> old = new ArrayList<>(v.getTeachers());

				for (JsonNode n : data.get("teachers")) {
					User tea = resolve(u.getInstance().getUsers(), n.asText());
					if (tea == null) {
						throw new ApiException("Teacher with uid " + n.asText() + " not found", null);
					} else if ( ! old.contains(tea)) {
						tea.getClasses().add(v);
						v.getTeachers().add(tea);
					}
				}
				// and now, remove old teachers
				for (User tea : old) {
					tea.getClasses().remove(v);
					v.getTeachers().remove(tea);
				}
			}
			if (data.has("students")) {
				List<Student> old = new ArrayList<>(v.getStudents());

				for (JsonNode n : data.get("students")) {
					Student s = resolve(u.getInstance().getStudents(), n.asText());
					if (s == null) {
						throw new ApiException("Student with sid " + n.asText() + " not found", null);
					} else if ( ! old.contains(s)) {
						v.getStudents().add(s);
						s.getEClass().getStudents().remove(s);
						s.setEClass(v);
					}
				}
				// and now, remove old students
				for (Student s : old) {
					s.setEClass(null);
				}
			}
		}

		entityManager.flush();
		return new GlobalState(t);
	}

	@PostMapping("/{token}/list")
	@Transactional
	public GlobalState list(@PathVariable String token) throws JsonProcessingException {
		log.info(token + "/list");
		Token t = resolveTokenOrBail(token);
		return new GlobalState(t);
	}

	@PostMapping("/{token}/rm/{oid}")
	@Transactional
	public GlobalState rm(
	        @PathVariable String token,
            @PathVariable String oid) throws JsonProcessingException {
		log.info(token + "/rm/" + oid);

		Token t = resolveTokenOrBail(token);
		User u = t.getUser();
		boolean found = false;

		Message m = resolve(u.getInstance().getMessages(), oid);
		if (m != null) {
			List<UMessage> toRemove = new ArrayList<>();
			for (UMessage um : u.getSent()) {
				if (um.getMessage().getId() == m.getId()) toRemove.add(um);
			}
			for (UMessage um : u.getReceived()) {
				if (um.getMessage().getId() == m.getId()) toRemove.add(um);
			}
			for (UMessage um : toRemove) {
				entityManager.remove(um);
				found = true;
			}
		} else if ( ! u.hasRole(User.Role.ADMIN)) {
			throw new ApiException("Bad message ID (and that is the only thing you can remove)", null);
		}

		// admin-only: remove users, students, classes

		if ( ! found) {
			EClass c = resolve(u.getInstance().getClasses(), oid);
			if (c != null) {
				for (User teacher : c.getTeachers()) teacher.getClasses().remove(c);
				// cascades for students, removing them; removes removed students from guardians
				for (Student st : c.getStudents()) {
					for (User g : st.getGuardians()) {
						g.getStudents().remove(g);
					}
					// so that returned globalstate is correct
					u.getInstance().getStudents().remove(st);
				}
				u.getInstance().getClasses().remove(c);
				entityManager.remove(c);

				// also remove class-user
				User mailbox = resolve(u.getInstance().getUsers(), oid);
				for (Message msg : u.getInstance().getMessages()) {
					if (msg.getTo().contains(mailbox)) {
						msg.getTo().remove(mailbox);
					}
				}
				entityManager.remove(mailbox);
				found = true;
			}
		}
		if ( ! found) {
			User o = resolve(u.getInstance().getUsers(), oid);
			if (o != null) {
				if (o.hasRole(User.Role.GUARDIAN)) {
					// remove from children
					for (Student s : o.getStudents()) {
						s.getGuardians().remove(o);
					}
				}
				if (o.hasRole(User.Role.TEACHER)) {
					// remove from class
					for (EClass ec : o.getClasses()) {
						ec.getTeachers().remove(o);
					}
				}
				// remove from messages too
				for (Message msg : u.getInstance().getMessages()) {
					if (msg.getFrom().getId() == o.getId()) {
						msg.setFrom(null);
					}
					if (msg.getTo().contains(o)) {
						msg.getTo().remove(o);
					}
				}

				u.getInstance().getUsers().remove(o);
				entityManager.remove(o);
				found = true;
			}
		}
		if ( ! found) {
			Student s = resolve(u.getInstance().getStudents(), oid);
			if (s != null) {
				// removes from class
				s.getEClass().getStudents().remove(s);
				// removes from guardians
				for (User g : s.getGuardians()) {
					g.getStudents().remove(g);
				}
				u.getInstance().getStudents().remove(s);
				entityManager.remove(s);
				found = true;
			}
		}

		if ( ! found) {
			throw new ApiException("ID not found; nothing removed", null);
		}

		return new GlobalState(t);
	}

    @PostMapping("/{token}/send")
    @Transactional
    public GlobalState send(
            @PathVariable String token,
			@RequestBody JsonNode data) throws JsonProcessingException {
        log.info(token + "/send/" + new ObjectMapper().writeValueAsString(data));

		Token t = resolveTokenOrBail(token);
		User u = t.getUser();
		// create an empty Message, and start to copy stuff over
		Message m = new Message();

		// The Id must be unique
		String mid = data.get("msgid").asText();
		if (resolve(u.getInstance().getUsers(), mid) != null) {
			throw new ApiException("Duplicate message id: " + mid, null);
		}
		m.setMid(mid);

		// The date, if present, must be valid and in the past -- and sent by an admin; uses format from
		DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
		if (data.has("date")) {
			String date = data.get("date").asText();
			Instant d = Instant.from(dtf.parse(date));
			if (d.isAfter(Instant.now()) || ! u.hasRole(User.Role.ADMIN)) {
				throw new ApiException("If date is specified, it must be a valid ISO date; and only admins use them", null);
			}
			m.setDate(date);
		} else {
			// not specified - we will set it ourselves to "now"
			m.setDate(dtf.format(Instant.now()));
		}

		if (data.has("parent")) {
			Message parent = resolve(u.getInstance().getMessages(), data.get("parent").asText());
			m.setParent(parent);
			m.getTo().add(parent.getFrom());
		} else {
			// One or more targets must be identified -- unless is reply
			for (JsonNode n : data.get("to")) {
				User o = resolve(u.getInstance().getUsers(), n.asText());
				if (o == null) {
					throw new ApiException("To-user with uid " + n.asText() + " not found", null);
				}
				// guardians cannot send to classes, or to anybody except their kid's teachers
				if (u.hasRole(User.Role.GUARDIAN)) {
					if ( ! o.hasRole(User.Role.TEACHER)) {
						throw new ApiException("Guardian cannot send to non-teacher " + n.asText(), null);
					}
					boolean hasKidWithTeacher = false;
					for (Student kid : u.getStudents()) {
						if (kid.getEClass().getTeachers().contains(o)) {
							hasKidWithTeacher = true;
							break;
						}
					}
					if ( ! hasKidWithTeacher) {
						throw new ApiException("Guardian can only send to kid's teacher " + n.asText(), null);
					}
				}
				m.getTo().add(o);
			}
		}
		if (m.getTo().isEmpty()) {
			throw new ApiException("Parent message, or at least 1 to-user must be specified", null);
		}

		// Check for title and body
		if ( ! data.has("title") || ! data.has("body")) {
			throw new ApiException("Missing title or body. Try harder.", null);
		}
		m.setSubject(HtmlUtils.htmlEscape(data.get("title").asText()));
		// sanitize body with an OWASP-approved library
		m.setBody(whitelistHtml(data.get("body").asText()));

		log.warn("After sanitization: {}\n{}\n", m.getSubject(), m.getBody());

		// A sender must be specified. And unless token is from admin, it must be a the actual caller
		User from = resolve(u.getInstance().getUsers(), data.get("from").asText());
		if (from == null ||
				(from.getId() != u.getId() && ! u.hasRole(User.Role.ADMIN))) {
			throw new ApiException("Sender must exist, and must be caller unless caller is admin", null);
		}
		m.setFrom(from);
		UMessage sent = new UMessage();
		sent.setMessage(m);
		sent.setLabels("sent, read");
		sent.setUser(from);
		from.getSent().add(sent);
		entityManager.persist(sent);

		// The mail must be delivered!
		for (User o : m.getTo()) {
			if (o.hasRole(User.Role.CLASS)) {
				// deliver to each class guardian, avoiding duplicates
				Set<User> targets = new HashSet<>();
				for (Student s : resolve(u.getInstance().getClasses(), o.getRef()).getStudents()) {
					targets.addAll(s.getGuardians());
				}
				for (User g : targets) {
					UMessage recvd = new UMessage();
					recvd.setMessage(m);
					recvd.setLabels("received");
					recvd.setUser(g);
					g.getReceived().add(recvd);
					entityManager.persist(recvd);
				}
			} else {
				// deliver to actual user
				UMessage recvd = new UMessage();
				recvd.setMessage(m);
				recvd.setLabels("received");
				recvd.setUser(o);
				o.getReceived().add(recvd);
				entityManager.persist(recvd);
			}
		}

		u.getInstance().getMessages().add(m);
		entityManager.persist(m);
		entityManager.flush(); // so returned state includes new message
		return new GlobalState(t);
	}
}
