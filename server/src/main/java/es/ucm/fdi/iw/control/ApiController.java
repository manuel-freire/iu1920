package es.ucm.fdi.iw.control;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
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
	public static class ApiAuthException extends RuntimeException {
		public ApiAuthException(String text) {
			super(text);
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
	 * Requests a token from the system. Provides a user to do so, for which only the
	 * password and uid are looked at
	 * @param loginUser attempting to log in.
	 * @throws JsonProcessingException
	 */
    @PostMapping("/login")
	@JsonView(Views.Public.class)
    @Transactional
    public GlobalState login(
            @RequestBody User loginUser) throws JsonProcessingException {
        log.info("/login/" + new ObjectMapper().writeValueAsString(loginUser));

		List<User> results = entityManager.createQuery(
				"from User where uid = :uid", User.class)
				.setParameter("uid", loginUser.getUid())
				.getResultList();
		// only expecting one, because uid is unique
		User u = results.isEmpty() ? null : results.get(0);

		if (u == null // NOTE: THIS SHOULD CHECK AN *ENCODED* PASSWORD
		              // PLAINTEXT IS A VERY BAD IDEA (outside this demo code)
				|| ! u.getPassword().equals(loginUser.getPassword())) {
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
			result.setPassword(pass);
		}

		result.setInstance(u.getInstance());
		u.getInstance().getUsers().add(result);
		entityManager.persist(result);
		entityManager.flush(); // so returned state includes new user
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
            @RequestBody UMessage umessage) throws JsonProcessingException {
        log.info(token + "/send/" + new ObjectMapper().writeValueAsString(umessage));
        return null;
    }

	@PostMapping("/{token}/file/{fileId}")
	public @ResponseBody String uploadFile(
			@PathVariable long token,
			@RequestParam MultipartFile file,
			@PathVariable String fileId,
			HttpServletResponse response) throws IOException {

		log.info(token + "/file (POST), " + fileId);

		String error = "";
        if (file.isEmpty() || fileId.isEmpty()) {
            error = "You failed to upload a file for "
                    + token + "/" + fileId + " because the file or its id was empty.";
        } else if (file.getBytes().length > 100000) {
			error = "Only files <= 100k can be uploaded"
					+ token + "/" + fileId + ": size is " + file.getBytes().length;
		} else {
			File f = localData.getFile("user/" + token,
					fileId.replaceAll("/", "_"));
			try (BufferedOutputStream stream =
						 new BufferedOutputStream(
								 new FileOutputStream(f))) {
				stream.write(file.getBytes());
				return "Uploaded " + token + "/" + fileId +
						" into " + f.getAbsolutePath() + "!";
			} catch (Exception e) {
				error = "Upload failed " + token + "/" + fileId +
						" => " + e.getMessage();
			}
		}
		// exit with error, blame user
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return error;
	}

	/**
	 * @param fileId to retrieve
	 * @return the file, or an 'unknown-user' image
	 */
	@RequestMapping(value="/{token}/file/{fileId}",
			method = RequestMethod.GET,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public void getFile(@PathVariable long apiKey,
						@PathVariable String fileId,
						HttpServletResponse response) {

		log.info(apiKey + "/file (GET), " + fileId);

		File f = localData.getFile("user/" + apiKey,
				fileId.replaceAll("/", "_"));
		try (InputStream in = f.exists() ?
				new BufferedInputStream(new FileInputStream(f)) :
				new BufferedInputStream(this.getClass().getClassLoader()
						.getResourceAsStream("unknown-user.jpg"))) {
			FileCopyUtils.copy(in, response.getOutputStream());
		} catch (IOException ioe) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
			log.info("Error retrieving file: " + f + " -- " + ioe.getMessage());
		}
	}
}
